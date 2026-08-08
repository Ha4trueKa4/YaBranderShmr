#pragma once
#include <userver/server/handlers/http_handler_base.hpp>
#include "../../components/project_manager_component.hpp"

namespace ya_brander::handlers {
class CreateProjectHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-projects-create";
  CreateProjectHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context);
  std::string HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext&) const override;
private:
  components::ProjectManagerComponent& project_manager_;
};
}