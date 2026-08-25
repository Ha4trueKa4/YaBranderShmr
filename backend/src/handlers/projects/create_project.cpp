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

  if (!prompt.empty()) {
    if (Utf8Length(prompt) > kMaxPromptLength) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
      userver::formats::json::ValueBuilder err;
      err["message"] = "Prompt is too long (max 5000 characters)";
      return err.ExtractValue();
    }

    std::string model = body["model"].As<std::string>("deepseek-v4-flash/latest");

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

  // prompt пуст -> копирование через data (без LLM, синхронно). references/
  // model здесь не парсятся вовсе: они не имеют смысла для этой ветки и не
  // должны мешать копированию, даже если случайно присланы вместе с data.
  std::string data = body["data"].As<std::string>("");
  if (data.empty()) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "Either prompt or data is required";
    return err.ExtractValue();
  }

  auto project = project_manager_.CreateProject(user_id, name, desc);
  // data НЕ валидируется как SVG — как и одноимённое поле в PATCH
  // /projects/{id} (см. UpdateProject/SaveInitialVersion).
  project.url = project_manager_.SaveInitialVersion(project.id, data);

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
  // job_id намеренно не пишем вообще (не null, а отсутствует ключом) —
  // для фронтенда это признак "уже готово, поллинг не нужен".

  request.SetResponseStatus(userver::server::http::HttpStatus::kCreated);
  return res.ExtractValue();
}

}