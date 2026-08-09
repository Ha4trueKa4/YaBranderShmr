#pragma once
#include <userver/server/handlers/http_handler_base.hpp>
#include "../../components/LLMClientComponent.hpp"

namespace ya_brander {
class TestLLMHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-test-llm";
  TestLLMHandler(const userver::components::ComponentConfig& config,
                 const userver::components::ComponentContext& context);
  std::string HandleRequestThrow(const userver::server::http::HttpRequest& request,
                                 userver::server::request::RequestContext&) const override;
private:
  components::LLMClientComponent& llm_client_;
};

void AppendLLM(userver::components::ComponentList& component_list);

}