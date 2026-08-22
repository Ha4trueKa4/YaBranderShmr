#include "test_polisher_handler.h"

#include <userver/formats/json/serialize.hpp>
#include <userver/components/component_context.hpp>

namespace ya_brander {

TestPolisherHandler::TestPolisherHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      prompt_polisher_(context.FindComponent<components::PromptPolisherComponent>()) {}

std::string TestPolisherHandler::HandleRequestThrow(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext&) const {
  
  auto body = userver::formats::json::FromString(request.RequestBody());
  std::string prompt = body["prompt"].As<std::string>("");

  if (prompt.empty()) {
    userver::formats::json::ValueBuilder error_result;
    error_result["success"] = false;
    error_result["error"] = "Field 'prompt' cannot be empty";
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return userver::formats::json::ToString(error_result.ExtractValue());
  }

  userver::formats::json::ValueBuilder result;

  try {
    std::string polished_prompt = prompt_polisher_.PolishGenerate(prompt);
    
    result["success"] = true;
    result["raw_prompt"] = prompt;
    result["polished_prompt"] = polished_prompt;
    
  } catch (const std::exception& e) {
    userver::formats::json::ValueBuilder error_result;
    error_result["success"] = false;
    error_result["error"] = e.what();

    request.SetResponseStatus(
        userver::server::http::HttpStatus::kInternalServerError);
    return userver::formats::json::ToString(error_result.ExtractValue());
  }
  
  return userver::formats::json::ToString(result.ExtractValue());
}

void AppendTestPolisher(userver::components::ComponentList& component_list) {
  component_list.Append<TestPolisherHandler>();
}

}  // namespace ya_brander