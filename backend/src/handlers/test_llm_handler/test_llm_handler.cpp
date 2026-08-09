#include "test_llm_handler.hpp"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander {

TestLLMHandler::TestLLMHandler(const userver::components::ComponentConfig& config, 
                               const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context), 
      llm_client_(context.FindComponent<components::LLMClientComponent>()) {}

std::string TestLLMHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, 
                                               userver::server::request::RequestContext&) const {
  auto body = userver::formats::json::FromString(request.RequestBody());
  std::string prompt = body["prompt"].As<std::string>("");
  size_t max_tokens = body["max_tokens"].As<size_t>(500);
  std::string model = body["model"].As<std::string>("yandexgpt-lite/latest");
  double temperature = body["temperature"].As<double>(0.1);

  std::string response = llm_client_.SendPrompt(prompt, max_tokens, model, temperature);
  
  userver::formats::json::ValueBuilder result;
  result["prompt"] = prompt;
  result["response"] = response;
  
  return userver::formats::json::ToString(result.ExtractValue());
}

void AppendLLM(userver::components::ComponentList& component_list) {
  component_list.Append<TestLLMHandler>();
}

}