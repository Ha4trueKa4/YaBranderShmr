#include "get_project.h"
#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

GetProjectHandler::GetProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value GetProjectHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
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
  res["id"] = project->id;
  res["author_id"] = project->author_id;
  res["name"] = project->name;
  res["description"] = project->description;
  res["is_public"] = project->is_public;
  res["created_at"] = userver::utils::datetime::Timestring(
      project->created_at, userver::utils::datetime::kDefaultTimezone,
      userver::utils::datetime::kRfc3339Format);
  res["data"] = project->data;

  return res.ExtractValue();
}

}