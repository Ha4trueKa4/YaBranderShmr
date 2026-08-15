#include "get_project_chat.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

GetProjectChatHandler::GetProjectChatHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string GetProjectChatHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
  const auto& id = request.GetPathArg("id");
  auto project = project_manager_.GetProject(id);

  if (!project) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }

  std::string* user_id = context.GetDataOptional<std::string>("user_id");
  if (!project->is_public) {
    if (!user_id) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
      return {};
    }
    if (*user_id != project->author_id) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
      return {};
    }
  }

  userver::formats::json::ValueBuilder res;
  res["chat_history"] = project_manager_.GetChatHistory(id);

  return userver::formats::json::ToString(res.ExtractValue());
}

}
