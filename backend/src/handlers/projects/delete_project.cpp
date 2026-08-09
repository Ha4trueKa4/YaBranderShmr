#include "delete_project.h"

namespace ya_brander::handlers {

DeleteProjectHandler::DeleteProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string DeleteProjectHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext&) const {
  // ЗАГЛУШКА:
  std::optional<std::string> user_id = "11111111-1111-1111-1111-111111111111"; // ЗАГЛУШКА, требуется реализация
  if (!user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  const auto& id = request.GetPathArg("id");
  auto project = project_manager_.GetProject(id);

  if (!project) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }
  if (project->author_id != *user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return {};
  }

  project_manager_.DeleteProject(id);
  return R"({"message": "Project deleted successfully"})";
}

}