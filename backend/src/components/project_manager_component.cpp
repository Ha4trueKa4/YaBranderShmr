#include "project_manager_component.h"
#include "llm_models.hpp"
#include <userver/formats/json/inline.hpp>
#include <userver/formats/json/value_builder.hpp>
#include "svg_validator.hpp"
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/io/json_types.hpp>
#include <userver/storages/redis/client.hpp>
#include <userver/storages/redis/component.hpp>
#include <userver/engine/deadline.hpp>
#include <userver/engine/sleep.hpp>
#include <userver/utils/async.hpp>
#include <userver/utils/uuid4.hpp>
#include <shared_mutex>
#include <cstdint>

#include <ya_brander/sql_queries.hpp>

#include "../utils/postgres_retry.hpp"

namespace ya_brander::components {

const std::string kDefaultSvg = R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
</svg>)SVG";

namespace {
// Сколько генераций может выполняться одновременно (SendPrompt к LLM +
// SaveVersion) — защита bg-task-processor и внешних зависимостей (LLM API,
// S3) от всплеска параллельных запросов.
constexpr std::size_t kMaxConcurrentGenerations = 40;
constexpr std::size_t kMaxGenerationOutputTokens = 40000;

std::string EscapeXml(const std::string& input) {
    std::string output;
    output.reserve(input.size() * 1.1);
    for (char c : input) {
        switch (c) {
            case '<': output += "&lt;"; break;
            case '>': output += "&gt;"; break;
            case '&': output += "&amp;"; break;
            case '"': output += "&quot;"; break;
            case '\'': output += "&apos;"; break;
            default: output += c; break;
        }
    }
    return output;
}
} // namespace

ProjectManagerComponent::ProjectManagerComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()),
      version_storage_(context.FindComponent<VersionStorageComponent>()),
      llm_client_(context.FindComponent<LLMClientComponent>()),
      prompt_constructor_(context.FindComponent<PromptConstructorComponent>()),
	  prompt_polisher_(context.FindComponent<PromptPolisherComponent>()),
      redis_client_(context.FindComponent<userver::components::Redis>("redis").GetClient("ya_brander_jobs")),
      generation_semaphore_(kMaxConcurrentGenerations),
      bts_(context.GetTaskProcessor("bg-task-processor")) {}

models::Project ProjectManagerComponent::CreateProject(const std::string& author_id, const std::string& name, const std::string& description) {
    auto res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kCreateProject,
        author_id, name, description
    );

    auto row = res[0];
    std::string project_id = row["id"].As<std::string>();
    LOG_INFO() << "Created project with ID: " << project_id;
    version_storage_.SaveVersion(project_id, kDefaultSvg);

	std::string url = version_storage_.GetLatestVersionUrl(project_id);

  	return {project_id, author_id, name, description, false, row["created_at"].As<std::chrono::system_clock::time_point>(), url};
}

std::optional<models::Project> ProjectManagerComponent::GetProject(const std::string& id) {
    auto res = utils::ExecuteSelectWithRetry("select-project", [&] {
      return pg_cluster_->Execute(
          userver::storages::postgres::ClusterHostType::kSlave,
          sql::kSelectProject, id);
    });

    if (res.IsEmpty()) return std::nullopt;

    auto row = res[0];
    const bool is_public = !row["is_private"].As<bool>();
   	std::string url = version_storage_.GetLatestVersionUrl(id);

    return models::Project{
        row["id"].As<std::string>(),
        row["author_id"].As<std::string>(),
        row["name"].As<std::string>(),
        row["description"].As<std::string>(),
        is_public,
        row["created_at"].As<std::chrono::system_clock::time_point>(),
        url
    };
}

void ProjectManagerComponent::UpdateProject(
    const std::string& id, std::optional<std::string> name,
    std::optional<bool> is_public, std::string data) {
  if (name.has_value()) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kUpdateProjectName,
        id, *name);
  }
  if (is_public.has_value()) {
    if (is_public.value()) {
      version_storage_.SetLatestAcl(id, true);
      pg_cluster_->Execute(
          userver::storages::postgres::ClusterHostType::kMaster,
          sql::kPublishProject,
          id);
    } else {
      pg_cluster_->Execute(
          userver::storages::postgres::ClusterHostType::kMaster,
          sql::kMakeProjectPrivate,
          id);
      version_storage_.SetLatestAcl(id, false);
    }
  }
  if (!data.empty()) {
    version_storage_.SaveVersion(id, data);
  }
}

    void ProjectManagerComponent::DeleteProject(const std::string& id) {

    // сначала удаляем версии проекта в S3 через этот метод
    // иначе они останутся висеть в S3 для уже удаленного проекта
    version_storage_.DeleteAllVersions(id);

    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kDeleteProject, id
    );
}

namespace {
constexpr std::size_t kHistoryPromptLimit = 10;
constexpr std::size_t kMaxReferenceProjects = 2;
}  // namespace

userver::formats::json::Value ProjectManagerComponent::FetchChatHistoryJson(
    const std::string& project_id) {
    auto res = utils::ExecuteSelectWithRetry(
        "select-project-chat-history", [&] {
          return pg_cluster_->Execute(
              userver::storages::postgres::ClusterHostType::kSlave,
              sql::kSelectProjectChatHistory, project_id);
        });
    if (res.IsEmpty()) return userver::formats::json::MakeArray();
    return res[0]["chat_history"].As<userver::formats::json::Value>();
}

std::vector<std::string> ProjectManagerComponent::GetRecentUserPrompts(
    const std::string& project_id, std::size_t limit, const std::string& exclude_job_id) {
    auto history_json = FetchChatHistoryJson(project_id);

    std::vector<std::string> prompts;
    for (const auto& entry : history_json) {
        if (entry["role"].As<std::string>("") == "user" &&
            entry["job_id"].As<std::string>("") != exclude_job_id) {
            prompts.push_back(entry["text"].As<std::string>(""));
        }
    }
    if (prompts.size() > limit) {
        prompts.erase(prompts.begin(), prompts.end() - static_cast<std::ptrdiff_t>(limit));
    }
    return prompts;
}

void ProjectManagerComponent::AppendUserMessage(const std::string& project_id, const std::string& job_id,
                                                 const std::string& user_message) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kAppendUserMessage,
        project_id, user_message, job_id);
}

void ProjectManagerComponent::AppendAssistantSuccess(const std::string& project_id, const std::string& job_id,
                                                      const std::string& version_id) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kAppendAssistantSuccess,
        project_id, version_id, job_id);
}

void ProjectManagerComponent::AppendAssistantError(const std::string& project_id, const std::string& job_id,
                                                    const std::string& error_message) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kAppendAssistantError,
        project_id, error_message, job_id);
}

void ProjectManagerComponent::AppendAssistantCancelled(const std::string& project_id, const std::string& job_id) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kAppendAssistantCancelled,
        project_id, job_id);
}

userver::formats::json::Value ProjectManagerComponent::GetChatHistory(const std::string& project_id) {
    auto history = FetchChatHistoryJson(project_id);

    // Не больше одного активного job'а на проект (см. active_job:{project_id}
    // в StartGenerationJob), поэтому достаточно одного точечного lookup в Redis.
    auto active_job_id = redis_client_->Get(ActiveJobKey(project_id), {}).Get();
    if (!active_job_id) {
        return history;
    }

    auto snapshot = ReadJob(*active_job_id);
    if (!snapshot || snapshot->status != JobStatus::kPending) {
        return history;
    }

    // Заглушка добавляется только в возвращаемый JSON, в БД ничего не пишем —
    // сообщение пользователя для этого job'а всегда последнее в chat_history
    // (StartGenerationJob резервирует active_job:{project_id} и пишет
    // AppendUserMessage раньше, чем что-либо ещё может добавиться), поэтому
    // достаточно просто дописать заглушку в конец скопированного массива.
    userver::formats::json::ValueBuilder result{history};
    userver::formats::json::ValueBuilder pending;
    pending["role"] = "assistant";
    pending["status"] = snapshot->cancel_requested ? "cancelling" : "pending";
    pending["job_id"] = *active_job_id;
    pending["created_at"] = userver::utils::datetime::Timestring(std::chrono::system_clock::now(), "UTC");;
    result.PushBack(std::move(pending));
    return result.ExtractValue();
}

std::vector<std::string> ProjectManagerComponent::ResolveReferenceSvgs(
    const std::string& caller_id,
    const std::vector<std::string>& reference_project_ids) {
    std::vector<std::string> result;
    for (const auto& ref_id : reference_project_ids) {
        if (result.size() >= kMaxReferenceProjects) break;

        auto ref_project = GetProject(ref_id);
        if (!ref_project) continue;  // не существует
        if (!ref_project->is_public && ref_project->author_id != caller_id) continue;  // нет доступа

        std::string svg = version_storage_.GetLatestVersionData(ref_id);
        if (svg.empty() || svg == kDefaultSvg) continue;  // у референса нет реального контента

        result.push_back(std::move(svg));
    }
    return result;
}

std::string ProjectManagerComponent::RunInitialGeneration(
    const std::string& project_id, const std::string& prompt, const std::string& model,
    const std::string& caller_id, const std::vector<std::string>& reference_project_ids) {
    if (IsRecraftModel(model)) {
        if (!reference_project_ids.empty()) {
            LOG_WARNING() << "Recraft generation does not support project references yet; "
                          << "references are ignored for project_id=" << project_id;
        }
        std::string new_data = llm_client_.GenerateRecraftSvg(prompt, model);
        return version_storage_.SaveVersion(project_id, new_data);
    }

    // Без истории: это первая генерация проекта, предыдущих запросов по
    // определению нет. AppendUserMessage уже синхронно записал этот же prompt
    // в chat_history до запуска этой корутины — GetRecentUserPrompts вернул
    // бы его же как "историю", задвоив текущий запрос ещё раз (в сыром виде,
    // рядом с уже отполированным блоком "ЗАПРОС ПОЛЬЗОВАТЕЛЯ" ниже).
    std::string polished_prompt_text = prompt_polisher_.PolishGenerate(prompt);
    std::string safe_prompt = EscapeXml(polished_prompt_text);
    auto reference_svgs = ResolveReferenceSvgs(caller_id, reference_project_ids);
    LlmPrompt full_prompt = prompt_constructor_.BuildGenerationPrompt(safe_prompt, {}, reference_svgs);

    auto new_data = llm_client_.SendPrompt(
        full_prompt, kMaxGenerationOutputTokens, model, 0.1);
    return version_storage_.SaveVersion(project_id, new_data);
}

std::string ProjectManagerComponent::RunEditGeneration(
    const std::string& project_id, const std::string& prompt, const std::string& model,
    const std::string& job_id) {
    std::string current_state = version_storage_.GetLatestVersionData(project_id);

    if (IsRecraftModel(model)) {
        throw std::invalid_argument(
            "Recraft V2 supports generation only for new projects");
    }

    // exclude_job_id=job_id: AppendUserMessage уже синхронно записал текущий
    // prompt в chat_history под этим же job_id до запуска этой корутины —
    // без исключения он попал бы в "историю" ещё раз, задвоив себя рядом с
    // блоком "ЗАПРОС ПОЛЬЗОВАТЕЛЯ" ниже.
    std::vector<std::string> history = GetRecentUserPrompts(project_id, kHistoryPromptLimit, job_id);
    for (auto& past_message : history) {
        past_message = EscapeXml(past_message);
    }

    std::string polished_prompt_text = prompt_polisher_.PolishEdit(prompt);
    std::string safe_prompt = EscapeXml(polished_prompt_text);
    LlmPrompt full_prompt = prompt_constructor_.BuildEditPrompt(current_state, safe_prompt, history);

    auto new_data = llm_client_.SendPrompt(
        full_prompt, kMaxGenerationOutputTokens, model, 0.1);
    return version_storage_.SaveVersion(project_id, new_data);
}

namespace {
constexpr std::chrono::seconds kJobRetention{30 * 60};
// Зависший kPending (задача не запустилась/не завершилась) переживает
// обычный kJobRetention — иначе легитимные ещё выполняющиеся генерации
// стирались бы из-под клиента. Redis сам вычищает такие записи по истечении
// TTL, никакого отдельного скана-чистильщика больше не нужно.
constexpr std::chrono::seconds kStuckPendingRetention{60 * 60};

// Насколько часто WaitForGeneration перечитывает статус job'а из Redis и
// насколько часто watcher-корутина в StartGeneration проверяет, не попросили
// ли отменить job с ДРУГОГО инстанса — простой поллинг вместо Pub/Sub,
// сознательно: генерация и так занимает секунды (сетевой вызов к LLM), так
// что задержка реакции в пределах этих интервалов не заметна на практике, а
// поллинг избавляет от гонок вида "подписался после того, как событие уже
// произошло", свойственных Pub/Sub.
constexpr std::chrono::milliseconds kWaitPollInterval{200};
constexpr std::chrono::milliseconds kCancelPollInterval{300};
}  // namespace

std::string ProjectManagerComponent::JobKey(const std::string& job_id) { return "job:" + job_id; }
std::string ProjectManagerComponent::ActiveJobKey(const std::string& project_id) { return "active_job:" + project_id; }

std::string ProjectManagerComponent::JobStatusToString(JobStatus status) {
    switch (status) {
        case JobStatus::kPending: return "pending";
        case JobStatus::kDone: return "done";
        case JobStatus::kError: return "error";
        case JobStatus::kCancelled: return "cancelled";
    }
    return "pending";
}

ProjectManagerComponent::JobStatus ProjectManagerComponent::JobStatusFromString(const std::string& value) {
    if (value == "done") return JobStatus::kDone;
    if (value == "error") return JobStatus::kError;
    if (value == "cancelled") return JobStatus::kCancelled;
    return JobStatus::kPending;
}

std::optional<ProjectManagerComponent::JobSnapshot> ProjectManagerComponent::ReadJob(const std::string& job_id) {
    auto fields = redis_client_->Hgetall(JobKey(job_id), {}).Get();
    if (fields.empty()) return std::nullopt;

    auto field = [&](const char* name) -> std::string {
        auto it = fields.find(name);
        return it != fields.end() ? it->second : std::string{};
    };

    JobSnapshot snapshot;
    snapshot.project_id = field("project_id");
    snapshot.status = JobStatusFromString(field("status"));
    snapshot.result_version_id = field("result_version_id");
    snapshot.error_message = field("error_message");
    snapshot.cancel_requested = field("cancel_requested") == "1";
    return snapshot;
}

std::optional<std::string> ProjectManagerComponent::ReserveActiveJob(
    const std::string& project_id, const std::string& job_id) {
    bool reserved = redis_client_
                        ->SetIfNotExist(
                            ActiveJobKey(project_id), job_id,
                            std::chrono::duration_cast<std::chrono::milliseconds>(kStuckPendingRetention), {})
                        .Get();
    if (reserved) return std::nullopt;
    return redis_client_->Get(ActiveJobKey(project_id), {}).Get().value_or(std::string{});
}

void ProjectManagerComponent::ReleaseReservedJob(const std::string& project_id, const std::string& job_id) {
    redis_client_->Del(std::vector<std::string>{ActiveJobKey(project_id), JobKey(job_id)}, {}).Get();
}

void ProjectManagerComponent::WriteInitialJob(const std::string& job_id, const std::string& project_id) {
    const auto now = std::chrono::duration_cast<std::chrono::seconds>(
                          std::chrono::system_clock::now().time_since_epoch())
                          .count();
    redis_client_
        ->Hmset(
            JobKey(job_id),
            {
                {"project_id", project_id},
                {"status", JobStatusToString(JobStatus::kPending)},
                {"result_version_id", ""},
                {"error_message", ""},
                {"cancel_requested", "0"},
                {"created_at", std::to_string(now)},
            },
            {})
        .Get();
    redis_client_->Expire(JobKey(job_id), kStuckPendingRetention, {}).Get();
}

void ProjectManagerComponent::SetCancelRequested(const std::string& job_id) {
    redis_client_->Hset(JobKey(job_id), "cancel_requested", "1", {}).Get();
}

bool ProjectManagerComponent::IsCancelRequested(const std::string& job_id) {
    auto value = redis_client_->Hget(JobKey(job_id), "cancel_requested", {}).Get();
    return value.has_value() && *value == "1";
}

void ProjectManagerComponent::WriteTerminalJob(
    const std::string& job_id, const std::string& project_id, JobStatus status,
    const std::string& result_version_id, const std::string& error_message) {
    // Снятие active_job:{project_id} и запись финального статуса job:{job_id}
    // должны быть одной атомарной операцией: клиент, получивший финальный
    // статус из WaitForGeneration сразу после HSET, мог бы тут же словить
    // спурьезный 409 на POST .../generate, если DEL слота на сервере ещё не
    // применился — простого "сначала DEL, потом HSET" через сеть недостаточно.
    static const std::string kScript = R"(
if redis.call('GET', KEYS[1]) == ARGV[1] then
    redis.call('DEL', KEYS[1])
end
redis.call('HSET', KEYS[2], 'status', ARGV[2], 'result_version_id', ARGV[3], 'error_message', ARGV[4])
redis.call('EXPIRE', KEYS[2], ARGV[5])
return 1
)";
    redis_client_
        ->Eval<std::int64_t>(
            kScript, {ActiveJobKey(project_id), JobKey(job_id)},
            {job_id, JobStatusToString(status), result_version_id, error_message,
             std::to_string(kJobRetention.count())},
            {})
        .Get();
}

models::GenerationJob ProjectManagerComponent::ToGenerationJob(
    const std::string& job_id, const std::string& project_id, const JobSnapshot& snapshot) {
    models::GenerationJob job;
    job.id = job_id;
    job.project_id = project_id;
    switch (snapshot.status) {
        case JobStatus::kPending:
            // Отмену уже запросили (через CancelGeneration), но фоновая
            // корутина (возможно, на другом инстансе) ещё не завершилась —
            // без этой ветки клиент, опросивший ПОСЛЕ чужого cancel, видел бы
            // просто "pending", будто отмены не было вовсе.
            job.status = snapshot.cancel_requested ? "cancelling" : "pending";
            break;
        case JobStatus::kDone:
            job.status = "done";
            job.result_url = version_storage_.GetVersionUrl(project_id, snapshot.result_version_id);
            break;
        case JobStatus::kError:
            job.status = "error";
            job.error_message = snapshot.error_message;
            break;
        case JobStatus::kCancelled:
            job.status = "cancelled";
            break;
    }
    return job;
}

std::string ProjectManagerComponent::StartGenerationJob(
    const std::string& project_id, const std::string& prompt,
    std::function<std::string(const std::string& job_id)> run_core_generation) {
    std::string job_id = userver::utils::generators::GenerateUuid();

    auto existing_job_id = ReserveActiveJob(project_id, job_id);
    if (existing_job_id) {
        throw GenerationInProgressError(project_id, *existing_job_id);
    }
    WriteInitialJob(job_id, project_id);

    try {
        // Сообщение пользователя пишем сразу: если это бросит исключение
        // (например, недоступна БД), откатываем резервацию ниже, чтобы
        // проект не остался навсегда "занят" несостоявшимся job'ом.
        AppendUserMessage(project_id, job_id, prompt);
    } catch (...) {
        ReleaseReservedJob(project_id, job_id);
        throw;
    }

    bts_.AsyncDetach("generation-job", [this, project_id, job_id,
                                        run_core_generation = std::move(run_core_generation)] {
        // Токен для внешней (в т.ч. с другого инстанса) отмены регистрируем
        // сразу же, в самом начале корутины — до захвата семафора, чтобы
        // отмена работала, даже пока job ещё стоит в очереди на генерацию.
        auto cancel_token = userver::engine::current_task::GetCancellationToken();

        // Опрашивает Redis на предмет cancel_requested и, если он выставлен
        // (с любого инстанса — см. CancelGeneration), прерывает саму
        // генерацию через cancel_token. Без этой watcher-корутины отмена,
        // пришедшая на ДРУГОЙ инстанс, никогда не добралась бы до процесса,
        // который реально выполняет RunGeneration.
        auto cancel_watcher = userver::utils::Async(
            "generation-cancel-watcher",
            [this, job_id, cancel_token]() mutable {
                while (!userver::engine::current_task::ShouldCancel()) {
                    if (IsCancelRequested(job_id)) {
                        cancel_token.RequestCancel();
                        return;
                    }
                    userver::engine::InterruptibleSleepFor(kCancelPollInterval);
                }
            });

		// Статус job'а зависит только от того, чем закончилась сама
        // генерация (run_core_generation): успехом, ошибкой или отменой.
        // Запись результата в chat_history — best-effort бухгалтерия ПОСЛЕ
        // того, как статус уже определён и разослан ожидающим в
        // WaitForGeneration: её падение не должно превращать уже случившийся
        // исход в другой — иначе клиент, опросивший чуть позже, увидел бы не
        // то, что было.
        std::string version_id;
        JobStatus outcome;
        std::string error_message;
        try {
            std::shared_lock<userver::engine::CancellableSemaphore> semaphore_lock(generation_semaphore_);
            version_id = run_core_generation(job_id);
            outcome = JobStatus::kDone;
        } catch (const std::exception& e) {
            if (IsCancelRequested(job_id)) {
                outcome = JobStatus::kCancelled;
                // cancel_requested — это флаг "отмену запросили", а не "именно
                // это исключение вызвано отменой": если поймана НЕ связанная с
                // отменой ошибка (LLM/S3/БД) в узком окне гонки с конкурентным
                // CancelGeneration, она будет классифицирована как "cancelled"
                // и e.what() не попадёт в AppendAssistantError — поэтому
                // логируем его здесь отдельно, чтобы не потерять для мониторинга.
                LOG_INFO() << "Generation job " << job_id << " for project_id=" << project_id
                           << " cancelled (underlying exception: " << e.what() << ")";
            } else {
                outcome = JobStatus::kError;
                error_message = e.what();
                LOG_ERROR() << "Generation job " << job_id << " for project_id=" << project_id << " failed: " << e.what();
            }
        }

        // Watcher больше не нужен — исход уже определён локально. Он либо
        // сам заметит свою отмену на следующей проверке ShouldCancel() и
        // выйдет, либо (если уже успел вызвать RequestCancel() из-за
        // подтверждённой отмены) и так вот-вот завершится — Get() просто
        // дожидается этого, не бросая исключение: лямбда watcher'а никогда
        // не кидает.
        cancel_watcher.RequestCancel();
        cancel_watcher.Get();

        WriteTerminalJob(job_id, project_id, outcome, version_id, error_message);

        // Без ретраев: при падении реальный исход (версия сохранена / job
        // отменён) не меняется — просто не появится записью в chat_history.
        // Громко логируем, чтобы это было видно в мониторинге.
        //
        // TaskCancellationBlocker обязателен именно здесь: если outcome ==
        // kCancelled, значит RequestCancel() уже вызван на ЭТОЙ же корутине, а
        // отмена в userver не одноразовая — она продолжает действовать на
        // все последующие точки ожидания той же задачи. Без блокировки
        // следующий же поход в Postgres (сам этот AppendAssistant*) тут же
        // прервался бы с "Task was cancelled before trying to get a
        // connection", и запись в chat_history так и не появлялась бы —
        // проверено вживую curl'ом, именно так и происходило.
        userver::engine::TaskCancellationBlocker cancellation_blocker;
        try {
            switch (outcome) {
                case JobStatus::kDone:
                    AppendAssistantSuccess(project_id, job_id, version_id);
                    break;
                case JobStatus::kError:
                    AppendAssistantError(project_id, job_id, error_message);
                    break;
                case JobStatus::kCancelled:
                    AppendAssistantCancelled(project_id, job_id);
                    break;
                case JobStatus::kPending:
                    break;  // недостижимо
            }
        } catch (const std::exception& e) {
            LOG_ERROR() << "Generation job " << job_id << " for project_id=" << project_id
                        << ": исход определён, но запись в chat_history не удалась: " << e.what();
        }
    });

    return job_id;
}

std::string ProjectManagerComponent::StartInitialGeneration(
    const std::string& project_id, const std::string& prompt, const std::string& model,
    const std::string& caller_id, const std::vector<std::string>& reference_project_ids) {
    return StartGenerationJob(project_id, prompt,
        [this, project_id, prompt, model, caller_id, reference_project_ids](const std::string&) {
            return RunInitialGeneration(project_id, prompt, model, caller_id, reference_project_ids);
        });
}

std::string ProjectManagerComponent::StartEditGeneration(
    const std::string& project_id, const std::string& prompt, const std::string& model) {
    return StartGenerationJob(project_id, prompt,
        [this, project_id, prompt, model](const std::string& job_id) {
            return RunEditGeneration(project_id, prompt, model, job_id);
        });
}

std::optional<models::GenerationJob> ProjectManagerComponent::WaitForGeneration(
    const std::string& project_id, const std::string& job_id, std::chrono::milliseconds timeout) {
    auto deadline = userver::engine::Deadline::FromDuration(timeout);

    while (true) {
        auto snapshot = ReadJob(job_id);
        if (!snapshot || snapshot->project_id != project_id) {
            return std::nullopt;
        }
        if (snapshot->status != JobStatus::kPending || deadline.IsReached()) {
            return ToGenerationJob(job_id, project_id, *snapshot);
        }
        userver::engine::InterruptibleSleepFor(kWaitPollInterval);
        if (userver::engine::current_task::ShouldCancel()) {
            // Клиент отвалился/запрос отменён — не тратим больше циклов на
            // поллинг, отдаём последний известный (всё ещё pending) снимок.
            return ToGenerationJob(job_id, project_id, *snapshot);
        }
    }
}

std::optional<models::GenerationJob> ProjectManagerComponent::CancelGeneration(
    const std::string& project_id, const std::string& job_id) {
    auto snapshot = ReadJob(job_id);
    if (!snapshot || snapshot->project_id != project_id) {
        return std::nullopt;
    }

    if (snapshot->status == JobStatus::kPending) {
        // Отмена запрошена, но фоновая корутина (возможно, на другом
        // инстансе — см. cancel_watcher в StartGenerationJob) ещё не успела её
        // заметить/обработать — финальный статус "cancelled" появится чуть
        // позже, см. WaitForGeneration.
        SetCancelRequested(job_id);
        snapshot->cancel_requested = true;
    }
    return ToGenerationJob(job_id, project_id, *snapshot);
}

std::vector<models::Version> ProjectManagerComponent::ListVersions(
const std::string& project_id) {
    return version_storage_.ListVersions(project_id);
}

std::string ProjectManagerComponent::GetVersionUrl(
    const std::string& project_id, const std::string& version_id) {
    return version_storage_.GetVersionUrl(project_id, version_id);
}

std::string ProjectManagerComponent::RestoreVersion(
    const std::string& project_id, const std::string& version_id) {
    return version_storage_.RestoreVersion(project_id, version_id);
}

std::string ProjectManagerComponent::GetLatestVersionUrl(const std::string& project_id) {
    return version_storage_.GetLatestVersionUrl(project_id);
}

} // namespace ya_brander::components
