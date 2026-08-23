#pragma once
#include <userver/clients/http/client.hpp>
#include <userver/server/handlers/http_handler_json_base.hpp>
#include <userver/storages/postgres/cluster.hpp>

#include "jwt_service.hpp"
#include "yandex_profile_client.hpp"

namespace ya_brander::auth {

class YandexOAuthHandler final : public userver::server::handlers::HttpHandlerJsonBase {
public:
    static constexpr std::string_view kName = "handler-auth-yandex";

    YandexOAuthHandler(const userver::components::ComponentConfig& config,
                        const userver::components::ComponentContext& context);

    userver::formats::json::Value HandleRequestJsonThrow(
        const userver::server::http::HttpRequest& request,
        const userver::formats::json::Value& request_json,
        userver::server::request::RequestContext& context) const override;

private:
    struct UserRecord {
        std::string id;
        std::string login;
        std::string email;
    };

    UserRecord FindOrCreateUser(const YandexProfile& profile) const;

    userver::storages::postgres::ClusterPtr pg_cluster_;
    YandexProfileClient yandex_client_;
    JwtService jwt_service_;
};

}  // namespace ya_brander::auth
