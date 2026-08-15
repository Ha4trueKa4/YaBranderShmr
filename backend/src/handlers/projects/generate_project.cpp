#include "generate_project.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

GenerateProjectHandler::GenerateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

std::string GenerateProjectHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
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
  std::string prompt = body["prompt"].As<std::string>("");

  if (prompt.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"message": "Prompt cannot be empty"})";
  }

  std::string new_url = project_manager_.GenerateAI(id, prompt);

  userver::formats::json::ValueBuilder res;
  if (!new_url.empty()) {
      res["url"] = new_url;
  }

  return userver::formats::json::ToString(res.ExtractValue());
}

}