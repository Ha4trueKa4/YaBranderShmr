#pragma once
#include <userver/server/handlers/http_handler_json_base.hpp>
#include "../../../components/project_manager_component.h"

namespace ya_brander::handlers {
    class RestoreVersionHandler final : public userver::server::handlers::HttpHandlerJsonBase {
    public:
        static constexpr std::string_view kName = "handler-projects-versions-restore";
        RestoreVersionHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context);
        userver::formats::json::Value HandleRequestJsonThrow(
            const userver::server::http::HttpRequest& request,
            const userver::formats::json::Value& request_json,
            userver::server::request::RequestContext& context) const override;
    private:
        components::ProjectManagerComponent& project_manager_;
    };
}