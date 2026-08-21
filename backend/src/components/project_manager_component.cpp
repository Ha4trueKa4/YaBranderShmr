#include "project_manager_component.h"
#include <userver/formats/json/inline.hpp>
#include "svg_validator.hpp"
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/io/json_types.hpp>
#include <userver/utils/uuid4.hpp>

namespace ya_brander::components {

const std::string kDefaultSvg = R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
</svg>)SVG";

namespace {
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
      bts_(context.GetTaskProcessor("main-task-processor")) {}

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

userver::formats::json::Value ProjectManagerComponent::GetChatHistory(const std::string& project_id) {
    return FetchChatHistoryJson(project_id);
}

std::string ProjectManagerComponent::RunGeneration(const std::string& project_id, const std::string& prompt, const std::string& model) {
    std::string current_state = version_storage_.GetLatestVersionData(project_id); // ЗАГЛУШКА

    std::vector<std::string> history = GetRecentUserPrompts(project_id, kHistoryPromptLimit);
    for (auto& past_message : history) {
        past_message = EscapeXml(past_message);
    }

    std::string safe_prompt = EscapeXml(prompt);

    LlmPrompt full_prompt;
    if (current_state.empty() || current_state == kDefaultSvg) {
        full_prompt = prompt_constructor_.BuildGenerationPrompt(safe_prompt, history);
    }else {
        full_prompt = prompt_constructor_.BuildEditPrompt(current_state, safe_prompt, history);
    }
    auto new_data = llm_client_.SendPrompt(full_prompt, 10000, model, 0.1);
    return version_storage_.SaveVersion(project_id, new_data);
}

namespace {
constexpr std::chrono::minutes kJobRetention{15};
}  // namespace

void ProjectManagerComponent::PruneOldJobsLocked() {
    const auto now = std::chrono::steady_clock::now();
    for (auto it = jobs_.begin(); it != jobs_.end();) {
        JobStatus status;
        {
            std::lock_guard job_lock(it->second->mutex);
            status = it->second->status;
        }
        if (status != JobStatus::kPending && now - it->second->created_at > kJobRetention) {
            it = jobs_.erase(it);
        } else {
            ++it;
        }
    }
}

std::string ProjectManagerComponent::StartGeneration(const std::string& project_id, const std::string& prompt, const std::string& model) {
    std::string job_id = userver::utils::generators::GenerateUuid();
    auto entry = std::make_shared<JobEntry>();
    entry->project_id = project_id;

    {
        std::lock_guard lock(jobs_mutex_);
        PruneOldJobsLocked();
        jobs_.emplace(job_id, entry);
    }

    AppendUserMessage(project_id, job_id, prompt);

    bts_.AsyncDetach("generation-job", [this, project_id, prompt, model, job_id, entry] {
        try {
            std::string version_id = RunGeneration(project_id, prompt, model);
            {
                std::lock_guard job_lock(entry->mutex);
                entry->status = JobStatus::kDone;
                entry->result_version_id = version_id;
            }
            entry->cv.NotifyAll();
            AppendAssistantSuccess(project_id, job_id, version_id);
        } catch (const std::exception& e) {
            LOG_ERROR() << "Generation job " << job_id << " for project_id=" << project_id << " failed: " << e.what();
            {
                std::lock_guard job_lock(entry->mutex);
                entry->status = JobStatus::kError;
                entry->error_message = e.what();
            }
            entry->cv.NotifyAll();
            AppendAssistantError(project_id, job_id, e.what());
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
    std::string result_version_id;
    std::string error_message;
    {
        std::unique_lock lock(entry->mutex);
        entry->cv.WaitFor(lock, timeout, [&] { return entry->status != JobStatus::kPending; });
        status = entry->status;
        result_version_id = entry->result_version_id;
        error_message = entry->error_message;
    }

    models::GenerationJob job;
    job.id = job_id;
    job.project_id = project_id;
    switch (status) {
        case JobStatus::kPending:
            job.status = "pending";
            break;
        case JobStatus::kDone:
            job.status = "done";
            job.result_url = version_storage_.GetVersionUrl(project_id, result_version_id);
            break;
        case JobStatus::kError:
            job.status = "error";
            job.error_message = error_message;
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
