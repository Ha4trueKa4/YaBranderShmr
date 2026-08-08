#include "project_manager_component.hpp"
#include <userver/storages/postgres/component.hpp>
#include <userver/formats/json/value_builder.hpp>

namespace brandbook::components {

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
        "INSERT INTO projects (author_id, name, description, is_private) VALUES ($1, $2, $3, true) RETURNING id, created_at",
        author_id, name, description
    );
    
    auto row = res[0];
    
    userver::formats::json::ValueBuilder default_data;
    default_data["width"] = 1024;
    default_data["height"] = 1024;
    default_data["background"] = "#FFFFFF";
    default_data["layers"] = userver::formats::json::MakeArray();

    std::string project_id = row["id"].As<std::string>();
    version_storage_.SaveVersion(project_id, default_data.ExtractValue()); // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ

    return {project_id, author_id, name, description, false, row["created_at"].As<std::string>(), default_data.ExtractValue()};
}

std::optional<models::Project> ProjectManagerComponent::GetProject(const std::string& id) {
    auto res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT id, author_id, name, COALESCE(description, ''), is_private, created_at FROM projects WHERE id = $1",
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

void ProjectManagerComponent::UpdateProject(const std::string& id, std::optional<bool> is_public, std::optional<userver::formats::json::Value> data) {
    if (is_public.has_value()) {
        pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE projects SET is_private = $1, updated_at = NOW() WHERE id = $2",
            !is_public.value(), id
        );
    }
    if (data.has_value()) {
        version_storage_.SaveVersion(id, data.value());  // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ
        pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE projects SET updated_at = NOW() WHERE id = $1", id
        );
    }
}

void ProjectManagerComponent::DeleteProject(const std::string& id) {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "DELETE FROM projects WHERE id = $1", id
    );
}

userver::formats::json::Value ProjectManagerComponent::GenerateAI(const std::string& project_id, const std::string& prompt) {
    auto current_state = version_storage_.GetLatestVersionData(project_id); // ЗАГЛУШКА
    std::string full_prompt = prompt_constructor_.BuildEditPrompt(current_state, prompt); // ЗАГЛУШКА
    auto new_data = llm_client_.SendPrompt(full_prompt); // ЗАГЛУШКА
    version_storage_.SaveVersion(project_id, new_data); // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ
    return new_data;
}

} // namespace brandbook::components