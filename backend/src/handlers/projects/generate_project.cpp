#include "generate_project.h"
#include "svg_validation_error_response.hpp"

#include <userver/formats/json/serialize.hpp>
#include <userver/logging/log.hpp>

namespace ya_brander::handlers {

namespace {
constexpr std::size_t kMaxPromptLength = 1500;
}

GenerateProjectHandler::GenerateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value GenerateProjectHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& body,
    userver::server::request::RequestContext& context) const {
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

  std::string prompt = body["prompt"].As<std::string>("");
  std::string model = body["model"].As<std::string>("deepseek-v4-flash/latest");

  if (prompt.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Prompt cannot be empty";
    return err.ExtractValue();
  }

  if (prompt.size() > kMaxPromptLength) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Prompt is too long (max 1500 characters)";
    return err.ExtractValue();
  }

  userver::formats::json::ValueBuilder res;
  try {
      std::string new_url = project_manager_.GenerateAI(id, prompt, model);

    if (!new_url.empty()) {
      res["url"] = new_url;
    }

  }catch (const components::SVGValidationError& error) {
      request.SetResponseStatus(
          userver::server::http::HttpStatus::kUnprocessableEntity);
    userver::formats::json::ValueBuilder err;
    err["message"] = error.what();
    return err.ExtractValue();
  }catch (const std::exception& e) {
    // Логируем реальную причину падения
    LOG_ERROR() << "AI generation failed for project_id=" << id << ": " << e.what();
    // Отдаем клиенту 503 статус вместо краша
    request.SetResponseStatus(userver::server::http::HttpStatus::kServiceUnavailable);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Generation service is temporarily unavailable. Please try again later.";
    return err.ExtractValue();
  }

  return res.ExtractValue();
}

}  // namespace ya_brander::handlers