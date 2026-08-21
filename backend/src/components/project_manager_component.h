#pragma once
#include <chrono>
#include <memory>
#include <optional>
#include <string>
#include <unordered_map>
#include <vector>
#include <userver/components/component_context.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/concurrent/background_task_storage.hpp>
#include <userver/engine/condition_variable.hpp>
#include <userver/engine/mutex.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/formats/json/value.hpp>

#include "../models/generation_job.hpp"
#include "../models/project.hpp"
#include "dependencies_stubs.h"
#include "LLMClientComponent.hpp"
#include "version_storage//version_storage_component.hpp"
#include "prompt_constructor/prompt_constructor_component.h"

namespace ya_brander::components {

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
  ///        ответа LLM. Сама генерация выполняется в фоновой корутине.
  std::string StartGeneration(const std::string& project_id, const std::string& prompt, const std::string& model);

  /// @brief Ждёт завершения job'а генерации не дольше timeout (long polling).
  /// @returns std::nullopt, если job с таким id не найден для этого проекта.
  std::optional<models::GenerationJob> WaitForGeneration(const std::string& project_id, const std::string& job_id,
                                                          std::chrono::milliseconds timeout);

  std::vector<models::Version> ListVersions(const std::string& project_id);

  std::string GetVersionUrl(const std::string& project_id,
                          const std::string& version_id);

  std::string RestoreVersion(const std::string& project_id,
                           const std::string& version_id);

  std::string GetLatestVersionUrl(const std::string& project_id);

  /// @brief Возвращает историю чата проекта как есть (JSON-массив).
  /// @details Каждый элемент — либо сообщение пользователя ({"role":"user",
  ///          "text":...}), либо ответ модели, представленный ссылкой на
  ///          версию ({"role":"assistant","version_id":...}), а не самим SVG
  ///          и не presigned-ссылкой (та имеет срок годности) — резолвинг
  ///          version_id в ссылку живёт в отдельном (не этом) эндпоинте.
  userver::formats::json::Value GetChatHistory(const std::string& project_id);

private:
  enum class JobStatus { kPending, kDone, kError };

  /// @brief Состояние одной фоновой задачи генерации. Хранится в jobs_ по
  ///        shared_ptr, чтобы фоновая корутина и WaitForGeneration могли
  ///        безопасно обращаться к нему независимо от того, жив ли ещё
  ///        соответствующий элемент карты jobs_.
  struct JobEntry {
    std::string project_id;
    std::chrono::steady_clock::time_point created_at = std::chrono::steady_clock::now();
    userver::engine::Mutex mutex;
    userver::engine::ConditionVariable cv;
    JobStatus status = JobStatus::kPending;
    std::string result_version_id;
    std::string error_message;
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

  /// @brief Собственно генерация (построение промпта, вызов LLM, сохранение
  ///        версии), выполняется в фоновой корутине из StartGeneration.
  ///        @returns id сохранённой версии.
  std::string RunGeneration(const std::string& project_id, const std::string& prompt, const std::string& model);

  /// @brief Удаляет из jobs_ завершённые job'ы старше kJobRetention, чтобы
  ///        карта не росла неограниченно за время жизни процесса. Вызывается
  ///        под jobs_mutex_.
  void PruneOldJobsLocked();

  userver::storages::postgres::ClusterPtr pg_cluster_;
  VersionStorageComponent& version_storage_;
  LLMClientComponent& llm_client_;
  PromptConstructorComponent& prompt_constructor_;

  userver::engine::Mutex jobs_mutex_;
  std::unordered_map<std::string, std::shared_ptr<JobEntry>> jobs_;

  // Должен быть объявлен последним: при разрушении компонента фоновые задачи
  // должны быть отменены/дожданы раньше, чем уничтожатся поля выше, которые
  // эти задачи используют (pg_cluster_, jobs_ и т.д.).
  userver::concurrent::BackgroundTaskStorage bts_;
};

} // namespace ya_brander::components
