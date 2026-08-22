#include "cancel_generation.hpp"

namespace ya_brander::handlers {

CancelGenerationHandler::CancelGenerationHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

userver::formats::json::Value CancelGenerationHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
  const auto& user_id = context.GetData<std::string>("user_id");

  const auto& id = request.GetPathArg("id");
  const auto& job_id = request.GetPathArg("job_id");

  auto project = project_manager_.GetProject(id);
  if (!project) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }
  if (project->author_id != user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return {};
  }

  auto job = project_manager_.CancelGeneration(id, job_id);
  if (!job) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return {};
  }

  userver::formats::json::ValueBuilder res;
  res["status"] = job->status;
  if (job->result_url) {
    res["url"] = *job->result_url;
  }
  if (job->error_message) {
    res["message"] = *job->error_message;
  }
  return res.ExtractValue();
}

}  // namespace ya_brander::handlers
