#include "patch_project.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

PatchProjectHandler::PatchProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string PatchProjectHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
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
  if (project->author_id != *user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return {};
  }

  auto body = userver::formats::json::FromString(request.RequestBody());
  std::optional<bool> is_public;
  std::optional<userver::formats::json::Value> data;

  if (body.HasMember("is_public")) is_public = body["is_public"].As<bool>();
  if (body.HasMember("data")) data = body["data"];

  project_manager_.UpdateProject(id, is_public, data);

  auto updated = project_manager_.GetProject(id);

  userver::formats::json::ValueBuilder res;
  res["id"] = updated->id;
  res["author_id"] = updated->author_id;
  res["name"] = updated->name;
  res["description"] = updated->description;
  res["is_public"] = updated->is_public;
  res["created_at"] = updated->created_at;
  res["data"] = updated->data;

  return userver::formats::json::ToString(res.ExtractValue());
}

}
