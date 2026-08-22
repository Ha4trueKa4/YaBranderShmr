#pragma once

#include <userver/components/component_list.hpp>
#include <userver/server/handlers/http_handler_base.hpp>
#include "../../components/prompt_constructor/prompt_polisher_component.h"

namespace ya_brander {

class TestPolisherHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-test-polisher";

  TestPolisherHandler(const userver::components::ComponentConfig& config,
                      const userver::components::ComponentContext& context);

  std::string HandleRequestThrow(
      const userver::server::http::HttpRequest& request,
      userver::server::request::RequestContext& context) const override;

private:
  components::PromptPolisherComponent& prompt_polisher_;
};

void AppendTestPolisher(userver::components::ComponentList& component_list);

}  // namespace ya_brander