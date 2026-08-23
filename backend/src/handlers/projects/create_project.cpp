#include "create_project.h"
#include "prompt_validation.h"

#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

CreateProjectHandler::CreateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value CreateProjectHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& body,
    userver::server::request::RequestContext& context) const {
  const auto& user_id = context.GetData<std::string>("user_id");

  std::string name = body["name"].As<std::string>("");
  std::string desc = body["description"].As<std::string>("");
  std::string prompt = body["prompt"].As<std::string>("");
  std::string model = body["model"].As<std::string>("deepseek-v4-flash/latest");

  if (prompt.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Prompt cannot be empty";
    return err.ExtractValue();
  }

  if (Utf8Length(prompt) > kMaxPromptLength) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Prompt is too long (max 5000 characters)";
    return err.ExtractValue();
  }

  std::vector<std::string> references;
  if (body.HasMember("references")) {
    references = body["references"].As<std::vector<std::string>>();
    if (references.size() > kMaxReferences) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
      userver::formats::json::ValueBuilder err;
      err["message"] = "references supports at most 2 project ids";
      return err.ExtractValue();
    }
  }

  auto project = project_manager_.CreateProject(user_id, name, desc);

  // GenerationInProgressError структурно недостижим здесь: у только что
  // созданного project.id не может быть записи в active_job_by_project_ —
  // ловить его специально не нужно, любое исключение уйдёт в обычный 500.
  std::string job_id = project_manager_.StartInitialGeneration(project.id, prompt, model, user_id, references);

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
  res["job_id"] = job_id;

  request.SetResponseStatus(userver::server::http::HttpStatus::kAccepted);
  return res.ExtractValue();
}

}