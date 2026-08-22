#pragma once
#include <chrono>
#include <memory>
#include <optional>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>
#include <userver/components/component_context.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/concurrent/background_task_storage.hpp>
#include <userver/engine/condition_variable.hpp>
#include <userver/engine/mutex.hpp>
#include <userver/engine/semaphore.hpp>
#include <userver/engine/task/cancel.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/formats/json/value.hpp>

#include "../models/generation_job.hpp"
#include "../models/project.hpp"
#include "dependencies_stubs.h"
#include "LLMClientComponent.hpp"
#include "version_storage//version_storage_component.hpp"
#include "prompt_constructor/prompt_constructor_component.h"

namespace ya_brander::components {

/// @brief Бросается StartGeneration, если для проекта уже есть незавершённый
///        (kPending) job генерации — одновременно может выполняться не
///        больше одной генерации на проект.
class GenerationInProgressError : public std::runtime_error {
public:
  GenerationInProgressError(const std::string& project_id, const std::string& active_job_id)
      : std::runtime_error("Generation already in progress for project " + project_id),
        active_job_id_(active_job_id) {}

  const std::string& active_job_id() const noexcept { return active_job_id_; }

private:
  std::string active_job_id_;
};

class ProjectManagerComponent final : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "project-manager";

  ProjectManagerComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context);

  models::Project CreateProject(const std::string& author_id, const std::string& name, const std::string& description);

  std::optional<models::Project> GetProject(const std::string& id);

  void UpdateProject(const std::string& id, std::optional<std::string> name,
                     std::optional<bool> is_public, std::string data);

  void DeleteProject(const std::string& id);

  /// @brief Запускает фоновую генерацию: сразу сохраняет сообщение
  ///        пользователя в chat_history и возвращает job_id, не дожидаясь
  ///        ответа LLM. Сама генерация выполняется в фоновой корутине на
  ///        bg-task-processor (отдельно от main-task-processor, который
  ///        обслуживает HTTP-запросы, включая long-poll GET .../generate/{job_id}).
  ///        version_storage_/llm_client_/prompt_constructor_ безопасны для
  ///        конкурентных вызовов из разных корутин: ни один из них не хранит
  ///        изменяемое состояние между вызовами (методы, читающие/пишущие
  ///        данные, либо const, либо работают только через
  ///        userver::storages::postgres::ClusterPtr / userver::clients::http::Client,
  ///        которые сами по себе конкурентно-безопасны).
  /// @throws GenerationInProgressError если для проекта уже выполняется
  ///         другой job (не больше одной генерации на проект одновременно).
  /// @throws std::exception если не удалось создать job (например, недоступна
  ///         БД при записи сообщения пользователя) — в этом случае job_id
  ///         никому не отдаётся и в jobs_ ничего не остаётся.
  std::string StartGeneration(const std::string& project_id, const std::string& prompt, const std::string& model);

  /// @brief Ждёт завершения job'а генерации не дольше timeout (long polling).
  /// @returns std::nullopt, если job с таким id не найден для этого проекта.
  std::optional<models::GenerationJob> WaitForGeneration(const std::string& project_id, const std::string& job_id,
                                                          std::chrono::milliseconds timeout);

  /// @brief Запрашивает отмену job'а генерации. Если он ещё kPending —
  ///        асинхронно (не дожидаясь здесь) прерывает фоновую корутину через
  ///        engine::TaskCancellationToken (реально прерывает блокирующие
  ///        ожидания — сетевой вызов к LLM, запись в S3/БД — а не просто
  ///        игнорирует уже посчитанный результат) и возвращает status
  ///        "cancelling"; финальный статус "cancelled" появится чуть позже —
  ///        см. WaitForGeneration. Если job уже завершён — не трогает его,
  ///        просто возвращает текущий (уже финальный) статус.
  /// @returns std::nullopt, если job с таким id не найден для этого проекта.
  std::optional<models::GenerationJob> CancelGeneration(const std::string& project_id, const std::string& job_id);

  std::vector<models::Version> ListVersions(const std::string& project_id);

  std::string GetVersionUrl(const std::string& project_id,
                          const std::string& version_id);

  std::string RestoreVersion(const std::string& project_id,
                           const std::string& version_id);

  std::string GetLatestVersionUrl(const std::string& project_id);

  /// @brief Возвращает историю чата проекта как есть (JSON-массив).
  /// @details Каждый элемент — либо сообщение пользователя ({"role":"user",
  ///          "text":...}), либо ответ модели, представленный ссылкой на
  ///          версию ({"role":"assistant","version_id":...}) или, если
  ///          генерация упала/была отменена — {"role":"assistant","error":...}
  ///          / {"role":"assistant","status":"cancelled"}. Не самим SVG и не
  ///          presigned-ссылкой (та имеет срок годности) — резолвинг
  ///          version_id в ссылку живёт в отдельном (не этом) эндпоинте.
  userver::formats::json::Value GetChatHistory(const std::string& project_id);

private:
  enum class JobStatus { kPending, kDone, kError, kCancelled };

  /// @brief Состояние одной фоновой задачи генерации. Хранится в jobs_ по
  ///        shared_ptr, чтобы фоновая корутина и WaitForGeneration могли
  ///        безопасно обращаться к нему независимо от того, жив ли ещё
  ///        соответствующий элемент карты jobs_.
  //
  // TODO(redis, multi-instance): все поля ниже сейчас предполагают, что
  // CancelGeneration/WaitForGeneration и сама фоновая корутина живут в одном
  // процессе. С несколькими инстансами за балансировщиком это не так:
  //  - mutex/cv — межпроцессный long-poll на std::condition_variable не
  //    работает; на Redis это обычно Pub/Sub-канал per job_id (WaitForGeneration
  //    подписывается и параллельно на всякий случай поллит с таймаутом) либо
  //    BLPOP/BRPOPLPUSH, а не WaitFor().
  //  - cancel_token (engine::TaskCancellationToken) — валиден только внутри
  //    процесса, который реально выполняет корутину. Если запрос POST
  //    .../cancel прилетел на ДРУГОЙ инстанс, чем тот, что тянет job, вызвать
  //    RequestCancel() напрямую нельзя — нужно, чтобы владеющий инстанс сам
  //    подписался на "запрос отмены" (тот же Pub/Sub-канал/публикация
  //    cancel_requested в Redis) и вызвал RequestCancel() у себя локально.
  //  - cancel_requested должен читаться не из локальной памяти, а из Redis
  //    (иначе WaitForGeneration/CancelGeneration на инстансе-не-владельце не
  //    увидят актуальное значение).
  struct JobEntry {
    std::string project_id;
    std::chrono::steady_clock::time_point created_at = std::chrono::steady_clock::now();
    userver::engine::Mutex mutex;
    userver::engine::ConditionVariable cv;
    JobStatus status = JobStatus::kPending;
    std::string result_version_id;
    std::string error_message;

    // Токен для запроса отмены фоновой корутины извне (из CancelGeneration).
    // Выставляется самой корутиной в самом начале (engine::current_task::
    // GetCancellationToken()), поэтому в первые мгновения после старта может
    // быть ещё не установлен — на этот случай cancel_requested выставляется
    // отдельно и проверяется корутиной сразу после установки токена.
    bool cancel_requested = false;
    std::optional<userver::engine::TaskCancellationToken> cancel_token;
  };

  /// @brief Сырой project.chat_history одним запросом (jsonb напрямую, без
  ///        ::text каста и повторного парсинга) — общий код для
  ///        GetChatHistory и GetRecentUserPrompts.
  userver::formats::json::Value FetchChatHistoryJson(const std::string& project_id);

  /// @brief Последние limit текстов запросов пользователя (без ответов
  ///        модели), от старых к новым — контекст для промпта LLM.
  std::vector<std::string> GetRecentUserPrompts(const std::string& project_id, std::size_t limit);

  /// @brief Добавляет сообщение пользователя в конец project.chat_history.
  ///        Вызывается сразу при старте job'а, до обращения к LLM.
  void AppendUserMessage(const std::string& project_id, const std::string& job_id, const std::string& user_message);

  /// @brief Добавляет успешный ответ ассистента (ссылку на версию) в конец
  ///        project.chat_history.
  void AppendAssistantSuccess(const std::string& project_id, const std::string& job_id,
                               const std::string& version_id);

  /// @brief Добавляет запись об ошибке генерации в конец project.chat_history.
  void AppendAssistantError(const std::string& project_id, const std::string& job_id,
                             const std::string& error_message);

  /// @brief Добавляет запись об отменённой генерации в конец project.chat_history.
  void AppendAssistantCancelled(const std::string& project_id, const std::string& job_id);

  /// @brief Собственно генерация (построение промпта, вызов LLM, сохранение
  ///        версии), выполняется в фоновой корутине из StartGeneration.
  ///        @returns id сохранённой версии.
  std::string RunGeneration(const std::string& project_id, const std::string& prompt, const std::string& model);

  /// @brief Удаляет из jobs_ завершённые job'ы старше kJobRetention, а
  ///        зависшие в kPending (задача так и не запустилась/не завершилась,
  ///        например из-за перегрузки task processor'а) — старше
  ///        kStuckPendingRetention. Без второго условия такой job остался бы
  ///        в jobs_ навсегда, так как обычное условие смотрит только на
  ///        завершённые статусы. Заодно чистит active_job_by_project_ от
  ///        ссылки на удаляемый job — иначе зависший job навсегда заблокировал
  ///        бы новые генерации для своего проекта даже после того, как сам он
  ///        уже вычищен из jobs_. Вызывается под jobs_mutex_.
  //
  // TODO(redis): при переезде jobs_/active_job_by_project_ на Redis эта
  // функция должна исчезнуть целиком — вместо ручного скана и удаления по
  // возрасту используется TTL на ключах (EXPIRE/PEXPIRE при записи, продление
  // TTL, пока job kPending), Redis чистит протухшие записи сам. Сохранять её
  // "просто на всякий случай" не нужно — TTL и есть замена, а не дополнение.
  void PruneOldJobsLocked();

  userver::storages::postgres::ClusterPtr pg_cluster_;
  VersionStorageComponent& version_storage_;
  LLMClientComponent& llm_client_;
  PromptConstructorComponent& prompt_constructor_;

  // TODO(redis, multi-instance): jobs_/active_job_by_project_/jobs_mutex_ —
  // вся эта пара карт in-process и не переживает ни рестарт процесса, ни
  // наличие второго инстанса за балансировщиком (каждый видит только свои
  // job'ы). Целевая схема на Redis:
  //   job:{job_id}          — хэш {project_id, status, result_version_id,
  //                            error_message, created_at}, TTL = kJobRetention
  //                            (или kStuckPendingRetention, пока pending)
  //   active_job:{project_id} — job_id активного job'а, TTL как у job:{job_id}
  // "Не больше одной активной генерации на проект" на Redis — это
  // SET active_job:{project_id} {job_id} NX (атомарный check-and-reserve одной
  // командой, замена связки "find + emplace под одним mutex" ниже). Освобождать
  // слот — DEL active_job:{project_id}, тоже одной командой, без раздельных
  // критических секций (см. TODO у блока erase/NotifyAll в .cpp).
  userver::engine::Mutex jobs_mutex_;
  std::unordered_map<std::string, std::shared_ptr<JobEntry>> jobs_;

  // project_id -> job_id незавершённого job'а этого проекта (если есть).
  // Не больше одной активной генерации на проект: StartGeneration проверяет
  // и резервирует запись здесь атомарно, под тем же jobs_mutex_, что и
  // вставку в jobs_ — иначе два конкурентных запроса могли бы оба проскочить
  // проверку до того, как любой из них зарезервирует слот.
  std::unordered_map<std::string, std::string> active_job_by_project_;

  // Ограничивает число одновременно выполняющихся генераций (SendPrompt к
  // LLM + SaveVersion), чтобы всплеск запросов не перегрузил bg-task-processor
  // и внешние зависимости (LLM API, S3). Захватывается только на время самой
  // генерации в фоновой корутине, не на всё время жизни job'а. Обязательно
  // CancellableSemaphore, а не Semaphore: последний игнорирует отмену
  // корутины, из-за чего CancelGeneration не смог бы прервать job, стоящий в
  // очереди на этот семафор.
  //
  // TODO(multi-instance): этот семафор ограничивает параллелизм ТОЛЬКО в
  // рамках одного процесса. С N инстансами реальный параллелизм обращений к
  // LLM/S3 может дорасти до N * kMaxConcurrentGenerations. Если лимит должен
  // быть общим на всё приложение (а не per-instance) — нужен распределённый
  // семафор (например, Redis-based token bucket/leases), иначе просто
  // уменьшите kMaxConcurrentGenerations на инстанс с расчётом на N инстансов.
  userver::engine::CancellableSemaphore generation_semaphore_;

  // Должен быть объявлен последним: при разрушении компонента фоновые задачи
  // должны быть отменены/дожданы раньше, чем уничтожатся поля выше, которые
  // эти задачи используют (pg_cluster_, jobs_ и т.д.).
  userver::concurrent::BackgroundTaskStorage bts_;
};

} // namespace ya_brander::components
