#pragma once
#include <userver/server/handlers/http_handler_json_base.hpp>
#include <userver/storages/postgres/cluster.hpp>

#include "jwt_service.hpp"

namespace ya_brander::auth {

class LoginHandler final : public userver::server::handlers::HttpHandlerJsonBase {
public:
    static constexpr std::string_view kName = "handler-auth-login";

    LoginHandler(const userver::components::ComponentConfig& config,
                 const userver::components::ComponentContext& context);

    userver::formats::json::Value HandleRequestJsonThrow(
        const userver::server::http::HttpRequest& request,
        const userver::formats::json::Value& request_json,
        userver::server::request::RequestContext& context) const override;

private:
    userver::storages::postgres::ClusterPtr pg_cluster_;
    JwtService jwt_service_;
};

}  // namespace ya_brander::auth
