#include "create_project.h"
#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

CreateProjectHandler::CreateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value CreateProjectHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& body,
    userver::server::request::RequestContext& context) const {
  std::string* user_id = context.GetDataOptional<std::string>("user_id");
  if (!user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  std::string name = body["name"].As<std::string>("");
  std::string desc = body["description"].As<std::string>("");

  if (name.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Name is required";
    return err.ExtractValue();
  }

  auto project = project_manager_.CreateProject(*user_id, name, desc);

  userver::formats::json::ValueBuilder res;
  res["id"] = project.id;
  res["author_id"] = project.author_id;
  res["name"] = project.name;
  res["description"] = project.description;
  res["is_public"] = project.is_public;
  res["created_at"] = userver::utils::datetime::Timestring(
      project.created_at, userver::utils::datetime::kDefaultTimezone,
      userver::utils::datetime::kRfc3339Format);  
  
  if (!project.url.empty()){
	  res["url"] = project.url;
  }
  

  request.SetResponseStatus(userver::server::http::HttpStatus::kCreated);
  return res.ExtractValue();
}

}