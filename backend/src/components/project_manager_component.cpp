#include "project_manager_component.h"
#include <userver/storages/postgres/component.hpp>
#include <userver/formats/json/value_builder.hpp>

namespace ya_brander::components {

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
    
  std::string default_data = R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
  <g>
  </g>
</svg>)SVG";


    std::string project_id = row["id"].As<std::string>();
    version_storage_.SaveVersion(project_id, default_data); // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ

    return {project_id, author_id, name, description, false, row["created_at"].As<std::string>(), default_data};
}

std::optional<models::Project> ProjectManagerComponent::GetProject(const std::string& id) {
    auto res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT id::text, author_id::text, name, COALESCE(description, '') AS description, is_private, created_at FROM projects WHERE id = $1::uuid",
        id
    );

    if (res.IsEmpty()) return std::nullopt;

    auto row = res[0];
    auto data = version_storage_.GetLatestVersionData(id); // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ

    return models::Project{
        row["id"].As<std::string>(),
        row["author_id"].As<std::string>(),
        row["name"].As<std::string>(),
        row["description"].As<std::string>(),
        !row["is_private"].As<bool>(),
        row["created_at"].As<std::string>(),
        data
    };
}

void ProjectManagerComponent::UpdateProject(const std::string& id, std::optional<bool> is_public, std::string data) {
    if (is_public.has_value()) {
        pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE projects SET is_private = $1, updated_at = NOW() WHERE id = $2::uuid",
            !is_public.value(), id
        );
    }
    if (!data.empty()) {
        version_storage_.SaveVersion(id, data);  // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ
        pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE projects SET updated_at = NOW() WHERE id = $1::uuid", id
        );
    }
}

void ProjectManagerComponent::DeleteProject(const std::string& id) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "DELETE FROM projects WHERE id = $1::uuid", id
    );
}

std::string ProjectManagerComponent::GenerateAI(const std::string& project_id, const std::string& prompt) {
    auto current_state = version_storage_.GetLatestVersionData(project_id); // ЗАГЛУШКА
    std::string full_prompt = prompt_constructor_.BuildEditPrompt(current_state, prompt); // ЗАГЛУШКА
    auto new_data = llm_client_.SendPrompt(full_prompt, 500, "yandexgpt-lite/latest", 0.1); // ЗАГЛУШКА
    version_storage_.SaveVersion(project_id, new_data); // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ
    return new_data;
}

} // namespace ya_brander::components
