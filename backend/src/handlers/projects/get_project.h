#pragma once
#include <userver/server/handlers/http_handler_json_base.hpp>
#include "../../components/project_manager_component.h"

namespace ya_brander::handlers {
class GetProjectHandler final : public userver::server::handlers::HttpHandlerJsonBase {
public:
  static constexpr std::string_view kName = "handler-projects-get";
  GetProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context);
  userver::formats::json::Value HandleRequestJsonThrow(
      const userver::server::http::HttpRequest& request,
      const userver::formats::json::Value& request_json,
      userver::server::request::RequestContext&) const override;
private:
  components::ProjectManagerComponent& project_manager_;
};
}