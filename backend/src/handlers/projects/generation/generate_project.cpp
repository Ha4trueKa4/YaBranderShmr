#include "generate_project.h"
#include "../prompt_validation.h"

#include <userver/formats/json/serialize.hpp>
#include <userver/logging/log.hpp>

namespace ya_brander::handlers {

GenerateProjectHandler::GenerateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value GenerateProjectHandler::HandleRequestJsonThrow(
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

  // Сообщение пользователя сохраняется в chat_history внутри общего
  // StartGenerationJob (вызывается StartEditGeneration) ещё до обращения к
  // LLM — так оно не теряется, даже если сама генерация потом упадёт.
  // Результат (или ошибка) генерации опрашивается отдельным long-poll
  // эндпоинтом GET .../generate/{job_id}. references здесь не принимаются:
  // они имеют смысл только при первичной генерации, см. POST /projects.
  try {
    std::string job_id = project_manager_.StartEditGeneration(id, prompt, model);

    request.SetResponseStatus(userver::server::http::HttpStatus::kAccepted);
    userver::formats::json::ValueBuilder res;
    res["job_id"] = job_id;
    return res.ExtractValue();
  } catch (const components::GenerationInProgressError& error) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
    userver::formats::json::ValueBuilder err;
    err["message"] = error.what();
    err["job_id"] = error.active_job_id();
    return err.ExtractValue();
  }
}

}  // namespace ya_brander::handlers
