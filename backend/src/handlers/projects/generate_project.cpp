#include "generate_project.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

GenerateProjectHandler::GenerateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string GenerateProjectHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext&) const {
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

  auto body = userver::formats::json::FromString(request.RequestBody());
  std::string prompt = body["prompt"].As<std::string>("");

  if (prompt.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"message": "Prompt cannot be empty"})";
  }

  auto new_data = project_manager_.GenerateAI(id, prompt);
  return userver::formats::json::ToString(new_data);
}

}