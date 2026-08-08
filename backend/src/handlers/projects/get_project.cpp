#include "get_project.hpp"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

GetProjectHandler::GetProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string GetProjectHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext&) const {
  const auto& id = request.GetPathArg("id");
  auto project = project_manager_.GetProject(id);

  if (!project) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }

  auto user_id = context.GetData("user_id"); // ЗАГЛУШКА, требуется реализация
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
  res["id"] = project->id;
  res["author_id"] = project->author_id;
  res["name"] = project->name;
  res["description"] = project->description;
  res["is_public"] = project->is_public;
  res["created_at"] = project->created_at;
  res["data"] = project->data;

  return userver::formats::json::ToString(res.ExtractValue());
}

}