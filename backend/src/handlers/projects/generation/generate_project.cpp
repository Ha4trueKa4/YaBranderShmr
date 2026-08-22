#include "generate_project.h"

#include <algorithm>
#include <string_view>

#include <userver/formats/json/serialize.hpp>
#include <userver/logging/log.hpp>

namespace ya_brander::handlers {

namespace {
constexpr std::size_t kMaxPromptLength = 5000;
constexpr std::size_t kMaxReferences = 2;

std::size_t Utf8Length(std::string_view value) {
  return static_cast<std::size_t>(
      std::count_if(value.begin(), value.end(), [](const char character) {
        return (static_cast<unsigned char>(character) & 0xC0U) != 0x80U;
      }));
}
}

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

  // Сообщение пользователя сохраняется в chat_history внутри StartGeneration
  // ещё до обращения к LLM — так оно не теряется, даже если сама генерация
  // потом упадёт. Результат (или ошибка) генерации опрашивается отдельным
  // long-poll эндпоинтом GET .../generate/{job_id}.
  std::string job_id = project_manager_.StartGeneration(id, prompt, model, user_id, references);

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
