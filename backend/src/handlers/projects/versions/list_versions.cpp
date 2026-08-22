#include "list_versions.hpp"
#include <userver/formats/json/value_builder.hpp>
#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

ListVersionsHandler::ListVersionsHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value ListVersionsHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& /*request_json*/,
    userver::server::request::RequestContext& context) const {
  const auto& user_id = context.GetData<std::string>("user_id");

  const auto& id = request.GetPathArg("id");
  auto project = project_manager_.GetProject(id);

  if (!project) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }
  // Author-only: history is private EVEN FOR PUBLIC PROJECTS (no is_public exemption).
  if (project->author_id != user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return {};
  }

  const auto versions = project_manager_.ListVersions(id);

  userver::formats::json::ValueBuilder arr(userver::formats::common::Type::kArray);
  for (const auto& v : versions) {
    userver::formats::json::ValueBuilder item;
    item["id"] = v.id;
    item["version_num"] = v.version_num;
    item["created_at"] = userver::utils::datetime::Timestring(
        v.created_at.GetUnderlying(), "UTC", "%Y-%m-%dT%H:%M:%S%Ez");
    arr.PushBack(std::move(item));
  }

  userver::formats::json::ValueBuilder res;
  res["versions"] = arr.ExtractValue();
  return res.ExtractValue();
}

}