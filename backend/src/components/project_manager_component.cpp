#include "project_manager_component.h"
#include <userver/formats/json/inline.hpp>
#include "svg_validator.hpp"
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/io/json_types.hpp>
#include <userver/utils/uuid4.hpp>
#include <shared_mutex>

namespace ya_brander::components {

const std::string kDefaultSvg = R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
</svg>)SVG";

namespace {
// Сколько генераций может выполняться одновременно (SendPrompt к LLM +
// SaveVersion) — защита bg-task-processor и внешних зависимостей (LLM API,
// S3) от всплеска параллельных запросов.
constexpr std::size_t kMaxConcurrentGenerations = 8;
constexpr std::size_t kMaxGenerationOutputTokens = 20000;

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
      generation_semaphore_(kMaxConcurrentGenerations),
      bts_(context.GetTaskProcessor("bg-task-processor")) {}

models::Project ProjectManagerComponent::CreateProject(const std::string& author_id, const std::string& name, const std::string& description) {
    auto res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "INSERT INTO projects (author_id, name, description, is_private) VALUES ($1::uuid, $2, $3, true) RETURNING id::text, created_at",
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
    auto res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT id::text, author_id::text, name, COALESCE(description, '') AS description, is_private, created_at FROM projects WHERE id = $1::uuid",
        id
    );

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
        "UPDATE projects SET name = $2, updated_at = NOW() "
        "WHERE id = $1::uuid",
        id, *name);
  }
  if (is_public.has_value()) {
    if (is_public.value()) {
      version_storage_.SetLatestAcl(id, true);
      pg_cluster_->Execute(
          userver::storages::postgres::ClusterHostType::kMaster,
          "UPDATE projects SET is_private=false, updated_at=NOW() "
          "WHERE id=$1::uuid",
          id);
    } else {
      pg_cluster_->Execute(
          userver::storages::postgres::ClusterHostType::kMaster,
          "UPDATE projects SET is_private=true, updated_at=NOW() "
          "WHERE id=$1::uuid",
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
        "DELETE FROM projects WHERE id = $1::uuid", id
    );
}

namespace {
constexpr std::size_t kHistoryPromptLimit = 10;
constexpr std::size_t kMaxReferenceProjects = 2;
}  // namespace

userver::formats::json::Value ProjectManagerComponent::FetchChatHistoryJson(
    const std::string& project_id) {
    auto res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT chat_history FROM projects WHERE id = $1::uuid",
        project_id);
    if (res.IsEmpty()) return userver::formats::json::MakeArray();
    return res[0]["chat_history"].As<userver::formats::json::Value>();
}

std::vector<std::string> ProjectManagerComponent::GetRecentUserPrompts(
    const std::string& project_id, std::size_t limit) {
    auto history_json = FetchChatHistoryJson(project_id);

    std::vector<std::string> prompts;
    for (const auto& entry : history_json) {
        if (entry["role"].As<std::string>("") == "user") {
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
        "UPDATE projects SET chat_history = chat_history || jsonb_build_array("
        "  jsonb_build_object('role', 'user', 'text', $2::text, 'job_id', $3::text, 'created_at', NOW())"
        "), updated_at = NOW() WHERE id = $1::uuid",
        project_id, user_message, job_id);
}

void ProjectManagerComponent::AppendAssistantSuccess(const std::string& project_id, const std::string& job_id,
                                                      const std::string& version_id) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "UPDATE projects SET chat_history = chat_history || jsonb_build_array("
        "  jsonb_build_object('role', 'assistant', 'version_id', $2::text, 'job_id', $3::text, 'created_at', NOW())"
        "), updated_at = NOW() WHERE id = $1::uuid",
        project_id, version_id, job_id);
}

void ProjectManagerComponent::AppendAssistantError(const std::string& project_id, const std::string& job_id,
                                                    const std::string& error_message) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "UPDATE projects SET chat_history = chat_history || jsonb_build_array("
        "  jsonb_build_object('role', 'assistant', 'error', $2::text, 'job_id', $3::text, 'created_at', NOW())"
        "), updated_at = NOW() WHERE id = $1::uuid",
        project_id, error_message, job_id);
}

void ProjectManagerComponent::AppendAssistantCancelled(const std::string& project_id, const std::string& job_id) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "UPDATE projects SET chat_history = chat_history || jsonb_build_array("
        "  jsonb_build_object('role', 'assistant', 'status', 'cancelled', 'job_id', $2::text, 'created_at', NOW())"
        "), updated_at = NOW() WHERE id = $1::uuid",
        project_id, job_id);
}

userver::formats::json::Value ProjectManagerComponent::GetChatHistory(const std::string& project_id) {
    return FetchChatHistoryJson(project_id);
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

std::string ProjectManagerComponent::RunGeneration(const std::string& project_id, const std::string& prompt,
                                                    const std::string& model, const std::string& caller_id,
                                                    const std::vector<std::string>& reference_project_ids) {
    std::string current_state = version_storage_.GetLatestVersionData(project_id); // ЗАГЛУШКА

    std::vector<std::string> history = GetRecentUserPrompts(project_id, kHistoryPromptLimit);
    for (auto& past_message : history) {
        past_message = EscapeXml(past_message);
    }

    LlmPrompt full_prompt;
    if (current_state.empty() || current_state == kDefaultSvg) {
		std::string polished_prompt_text = prompt_polisher_.PolishGenerate(prompt);
		std::string safe_prompt = EscapeXml(polished_prompt_text);
        auto reference_svgs = ResolveReferenceSvgs(caller_id, reference_project_ids);
        full_prompt = prompt_constructor_.BuildGenerationPrompt(safe_prompt, history, reference_svgs);
    }else {
		std::string polished_prompt_text = prompt_polisher_.PolishEdit(prompt);
		std::string safe_prompt = EscapeXml(polished_prompt_text);
        full_prompt = prompt_constructor_.BuildEditPrompt(current_state, safe_prompt, history);
    }
    auto new_data = llm_client_.SendPrompt(
        full_prompt, kMaxGenerationOutputTokens, model, 0.1);
    return version_storage_.SaveVersion(project_id, new_data);
}

namespace {
constexpr std::chrono::minutes kJobRetention{30};
// Зависший kPending (задача не запустилась/не завершилась) переживает
// обычный kJobRetention — иначе легитимные ещё выполняющиеся генерации
// стирались бы из-под клиента. Чистим их только как страховку намного позже.
constexpr std::chrono::minutes kStuckPendingRetention{60};
}  // namespace

void ProjectManagerComponent::PruneOldJobsLocked() {
    const auto now = std::chrono::steady_clock::now();
    for (auto it = jobs_.begin(); it != jobs_.end();) {
        JobStatus status;
        {
            std::lock_guard job_lock(it->second->mutex);
            status = it->second->status;
        }
        const auto age = now - it->second->created_at;
        const bool expired = status == JobStatus::kPending ? age > kStuckPendingRetention : age > kJobRetention;
        if (expired) {
            auto active_it = active_job_by_project_.find(it->second->project_id);
            if (active_it != active_job_by_project_.end() && active_it->second == it->first) {
                active_job_by_project_.erase(active_it);
            }
            it = jobs_.erase(it);
        } else {
            ++it;
        }
    }
}

std::string ProjectManagerComponent::StartGeneration(const std::string& project_id, const std::string& prompt,
                                                     const std::string& model, const std::string& caller_id,
                                                     const std::vector<std::string>& reference_project_ids) {
    std::string job_id = userver::utils::generators::GenerateUuid();
    auto entry = std::make_shared<JobEntry>();
    entry->project_id = project_id;

    // Проверка "нет ли уже активной генерации для проекта" и резервирование
    // слота под неё — одной атомарной операцией под jobs_mutex_. Если бы это
    // были два отдельных захвата лока, два конкурентных StartGeneration для
    // одного проекта могли бы оба проскочить проверку раньше, чем любой из
    // них зарезервирует active_job_by_project_.
    {
        std::lock_guard lock(jobs_mutex_);
        PruneOldJobsLocked();
        auto active_it = active_job_by_project_.find(project_id);
        if (active_it != active_job_by_project_.end()) {
            throw GenerationInProgressError(project_id, active_it->second);
        }
        jobs_.emplace(job_id, entry);
        active_job_by_project_.emplace(project_id, job_id);
    }

    try {
        // Сообщение пользователя пишем сразу: если это бросит исключение
        // (например, недоступна БД), откатываем резервацию ниже, чтобы
        // проект не остался навсегда "занят" несостоявшимся job'ом.
        AppendUserMessage(project_id, job_id, prompt);
    } catch (...) {
        std::lock_guard lock(jobs_mutex_);
        jobs_.erase(job_id);
        active_job_by_project_.erase(project_id);
        throw;
    }

    bts_.AsyncDetach("generation-job", [this, project_id, prompt, model, caller_id, reference_project_ids, job_id, entry] {
        // Статус job'а зависит только от того, чем закончилась сама
        // генерация (RunGeneration): успехом, ошибкой или отменой. Запись
        // результата в chat_history — best-effort бухгалтерия ПОСЛЕ того, как
        // статус уже определён и разослан ожидающим в WaitForGeneration: её
        // падение не должно превращать уже случившийся исход в другой —
        // иначе клиент, опросивший чуть позже, увидел бы не то, что было.
        std::string version_id;
        JobStatus outcome;
        std::string error_message;
        try {
            std::shared_lock<userver::engine::CancellableSemaphore> semaphore_lock(generation_semaphore_);
            version_id = RunGeneration(project_id, prompt, model, caller_id, reference_project_ids);
            outcome = JobStatus::kDone;
        } catch (const std::exception& e) {
            bool was_cancelled;
            {
                std::lock_guard job_lock(entry->mutex);
                was_cancelled = entry->cancel_requested;
            }
            if (was_cancelled) {
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

        // Слот active_job_by_project_ освобождаем ДО того, как исход станет
        // видимым снаружи (entry->status + NotifyAll ниже) — иначе клиент,
        // получивший "done"/"error"/"cancelled" из WaitForGeneration, мог бы
        // тут же получить спурьезный 409 на новый POST .../generate: его
        // StartGeneration успел бы проверить active_job_by_project_ раньше,
        // чем эта же корутина (на отдельном bg-task-processor) дошла бы сюда.
        // TODO(redis): в редис-версии это должно стать одной атомарной
        // операцией (Lua-скрипт/MULTI-EXEC: снять active_job:{project_id} и
        // записать финальный статус job:{job_id} одной транзакцией) — простого
        // "поменять порядок двух критических секций" через сеть недостаточно.
        {
            std::lock_guard lock(jobs_mutex_);
            auto active_it = active_job_by_project_.find(project_id);
            if (active_it != active_job_by_project_.end() && active_it->second == job_id) {
                active_job_by_project_.erase(active_it);
            }
        }

        {
            std::lock_guard job_lock(entry->mutex);
            entry->status = outcome;
            if (outcome == JobStatus::kDone) entry->result_version_id = version_id;
            if (outcome == JobStatus::kError) entry->error_message = error_message;
        }
        entry->cv.NotifyAll();

        // Без ретраев: при падении реальный исход (версия сохранена / job
        // отменён) не меняется — просто не появится записью в chat_history.
        // Громко логируем, чтобы это было видно в мониторинге.
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

std::optional<models::GenerationJob> ProjectManagerComponent::WaitForGeneration(
    const std::string& project_id, const std::string& job_id, std::chrono::milliseconds timeout) {
    std::shared_ptr<JobEntry> entry;
    {
        std::lock_guard lock(jobs_mutex_);
        auto it = jobs_.find(job_id);
        if (it == jobs_.end() || it->second->project_id != project_id) {
            return std::nullopt;
        }
        entry = it->second;
    }

    JobStatus status;
    bool cancel_requested;
    std::string result_version_id;
    std::string error_message;
    {
        std::unique_lock lock(entry->mutex);
        entry->cv.WaitFor(lock, timeout, [&] { return entry->status != JobStatus::kPending; });
        status = entry->status;
        cancel_requested = entry->cancel_requested;
        result_version_id = entry->result_version_id;
        error_message = entry->error_message;
    }

    models::GenerationJob job;
    job.id = job_id;
    job.project_id = project_id;
    switch (status) {
        case JobStatus::kPending:
            // Отмену уже запросили (через CancelGeneration), но фоновая
            // корутина ещё не завершилась — тот же переходный статус, что
            // CancelGeneration возвращает сама. Без этой ветки клиент,
            // опросивший через long-poll ПОСЛЕ чужого cancel, видел бы просто
            // "pending", будто отмены не было вовсе.
            job.status = cancel_requested ? "cancelling" : "pending";
            break;
        case JobStatus::kDone:
            job.status = "done";
            job.result_url = version_storage_.GetVersionUrl(project_id, result_version_id);
            break;
        case JobStatus::kError:
            job.status = "error";
            job.error_message = error_message;
            break;
        case JobStatus::kCancelled:
            job.status = "cancelled";
            break;
    }
    return job;
}

std::optional<models::GenerationJob> ProjectManagerComponent::CancelGeneration(
    const std::string& project_id, const std::string& job_id) {
    std::shared_ptr<JobEntry> entry;
    {
        std::lock_guard lock(jobs_mutex_);
        auto it = jobs_.find(job_id);
        if (it == jobs_.end() || it->second->project_id != project_id) {
            return std::nullopt;
        }
        entry = it->second;
    }

    JobStatus status;
    std::string result_version_id;
    std::string error_message;
    {
        std::lock_guard job_lock(entry->mutex);
        status = entry->status;
        if (status == JobStatus::kPending) {
            entry->cancel_requested = true;
            if (entry->cancel_token) {
                entry->cancel_token->RequestCancel();
            }
        }
        result_version_id = entry->result_version_id;
        error_message = entry->error_message;
    }

    models::GenerationJob job;
    job.id = job_id;
    job.project_id = project_id;
    switch (status) {
        case JobStatus::kPending:
            // Отмена запрошена, но фоновая корутина ещё не успела её
            // заметить/обработать — финальный статус "cancelled" появится
            // чуть позже, см. WaitForGeneration.
            job.status = "cancelling";
            break;
        case JobStatus::kDone:
            job.status = "done";
            job.result_url = version_storage_.GetVersionUrl(project_id, result_version_id);
            break;
        case JobStatus::kError:
            job.status = "error";
            job.error_message = error_message;
            break;
        case JobStatus::kCancelled:
            job.status = "cancelled";
            break;
    }
    return job;
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
