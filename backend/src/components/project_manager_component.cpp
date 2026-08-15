#include "project_manager_component.h"
#include <userver/storages/postgres/component.hpp>

namespace ya_brander::components {

const std::string kDefaultSvg = R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
  <g>
  </g>
</svg>)SVG";

ProjectManagerComponent::ProjectManagerComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()),
      version_storage_(context.FindComponent<VersionStorageComponent>()),
      llm_client_(context.FindComponent<LLMClientComponent>()),
      prompt_constructor_(context.FindComponent<PromptConstructorComponent>()) {}

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
    const std::string& id, std::optional<bool> is_public, std::string data) {
if (is_public.has_value()) {
    if (is_public.value()) {
        version_storage_.SetLatestAcl(id, true);
        pg_cluster_->Execute(userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE projects SET is_private=false, updated_at=NOW() WHERE id=$1::uuid", id);
    } else {
        pg_cluster_->Execute(userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE projects SET is_private=true, updated_at=NOW() WHERE id=$1::uuid", id);
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

std::string ProjectManagerComponent::GenerateAI(const std::string& project_id, const std::string& prompt) {
    std::string current_state = version_storage_.GetLatestVersionData(project_id);
    std::string full_prompt;

    if (current_state.empty() || current_state == kDefaultSvg) {
        full_prompt = prompt_constructor_.BuildGenerationPrompt(prompt);
    }else {
        full_prompt = prompt_constructor_.BuildEditPrompt(current_state, prompt);
    }
    auto new_data = llm_client_.SendPrompt(full_prompt, 500, "yandexgpt-lite/latest", 0.1);
    version_storage_.SaveVersion(project_id, new_data);
    return version_storage_.GetLatestVersionUrl(project_id);
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