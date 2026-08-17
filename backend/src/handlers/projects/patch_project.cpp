#include "patch_project.h"
#include "svg_validation_error_response.hpp"

#include <userver/formats/json/serialize.hpp>
#include <userver/logging/log.hpp>
#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

PatchProjectHandler::PatchProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value PatchProjectHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& body,
    userver::server::request::RequestContext& context) const {
  const auto& user_id = context.GetData<std::string>("user_id");

  const auto& id = request.GetPathArg("id");
  auto project = project_manager_.GetProject(id);

  if (!project) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }
  if (project->author_id != user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return {};
  }

  std::optional<std::string> name;
  std::optional<bool> is_public;
  std::string data;

  if (body.HasMember("name")) name = body["name"].As<std::string>();
  if (body.HasMember("is_public")) is_public = body["is_public"].As<bool>();
  if (body.HasMember("data")) data = body["data"].As<std::string>();

  if (name.has_value() && (name->empty() || name->size() > 255)) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["code"] = "INVALID_NAME";
    err["message"] = "Name must be 1-255 chars";
    err["field"] = "name";
    return err.ExtractValue();
  }

  project_manager_.UpdateProject(id, name, is_public, data);

  auto updated = project_manager_.GetProject(id);

  userver::formats::json::ValueBuilder res;
  res["id"] = updated->id;
  res["author_id"] = updated->author_id;
  res["name"] = updated->name;
  res["description"] = updated->description;
  res["is_public"] = updated->is_public;
  res["created_at"] = userver::utils::datetime::Timestring(
      updated->created_at, userver::utils::datetime::kDefaultTimezone,
      userver::utils::datetime::kRfc3339Format);

  if (!updated->url.empty()) {
      res["url"] = updated->url;
  }

  return res.ExtractValue();
}

}  // namespace ya_brander::handlers
