#include "create_project.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

CreateProjectHandler::CreateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string CreateProjectHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext&) const {
  // ЗАГЛУШКА:
  std::optional<std::string> user_id = "11111111-1111-1111-1111-111111111111";  // ЗАГЛУШКА, требуется реализация context.GetData("user_id");
  if (!user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  auto body = userver::formats::json::FromString(request.RequestBody());
  std::string name = body["name"].As<std::string>("");
  std::string desc = body["description"].As<std::string>("");

  if (name.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"message": "Name is required"})";
  }

  auto project = project_manager_.CreateProject(*user_id, name, desc);

  userver::formats::json::ValueBuilder res;
  res["id"] = project.id;
  res["author_id"] = project.author_id;
  res["name"] = project.name;
  res["is_public"] = project.is_public;
  res["created_at"] = project.created_at;

  request.SetResponseStatus(userver::server::http::HttpStatus::kCreated);
  return userver::formats::json::ToString(res.ExtractValue());
}

}