#include "get_project_chat.h"

namespace ya_brander::handlers {

GetProjectChatHandler::GetProjectChatHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value GetProjectChatHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
  std::string* user_id = context.GetDataOptional<std::string>("user_id");
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

  // История чата приватна даже для публичного проекта — доступна только
  // автору, так же как история версий (GET /projects/{id}/versions).
  if (project->author_id != *user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return {};
  }

  userver::formats::json::ValueBuilder res;
  res["chat_history"] = project_manager_.GetChatHistory(id);

  return res.ExtractValue();
}

}
