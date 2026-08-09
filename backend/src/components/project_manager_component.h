#pragma once
#include <optional>
#include <string>
#include <userver/components/component_context.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/formats/json/value.hpp>

#include "../models/project.hpp"
#include "dependencies_stubs.h"

namespace ya_brander::components {

class ProjectManagerComponent final : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "project-manager";

  ProjectManagerComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context);

  models::Project CreateProject(const std::string& author_id, const std::string& name, const std::string& description);

  std::optional<models::Project> GetProject(const std::string& id);

  void UpdateProject(const std::string& id, std::optional<bool> is_public, std::optional<userver::formats::json::Value> data);

  void DeleteProject(const std::string& id);

  userver::formats::json::Value GenerateAI(const std::string& project_id, const std::string& prompt);

private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
  VersionStorageComponent& version_storage_;
  LLMClientComponent& llm_client_;
  PromptConstructorComponent& prompt_constructor_;
};

} // namespace ya_brander::components