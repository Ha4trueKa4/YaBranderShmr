#pragma once
#include <userver/server/handlers/http_handler_base.hpp>
#include "../../components/project_manager_component.h"

namespace ya_brander::handlers {
class GetModels final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-models-get";

  GetModels(const userver::components::ComponentConfig& config,
            const userver::components::ComponentContext& context);

  std::string HandleRequestThrow(
      const userver::server::http::HttpRequest& request,
      userver::server::request::RequestContext&) const override;

  static userver::yaml_config::Schema GetStaticConfigSchema();

private:
  userver::clients::http::Client& http_client_;
  std::string api_url_;
  std::string api_key_;
};

void AppendGetModels(userver::components::ComponentList& component_list);
}