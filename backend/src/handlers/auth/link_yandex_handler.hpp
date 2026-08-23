#pragma once
#include <userver/server/handlers/http_handler_json_base.hpp>
#include <userver/storages/postgres/cluster.hpp>

#include "yandex_profile_client.hpp"

namespace ya_brander::auth {

// Привязывает Yandex ID к уже авторизованному (по JWT) пользователю —
// закрывает флоу, на который отправляет 409 в POST /auth/yandex, когда email
// из Yandex-профиля занят аккаунтом без привязанного provider.
class LinkYandexHandler final : public userver::server::handlers::HttpHandlerJsonBase {
public:
    static constexpr std::string_view kName = "handler-auth-yandex-link";

    LinkYandexHandler(const userver::components::ComponentConfig& config,
                       const userver::components::ComponentContext& context);

    userver::formats::json::Value HandleRequestJsonThrow(
        const userver::server::http::HttpRequest& request,
        const userver::formats::json::Value& request_json,
        userver::server::request::RequestContext& context) const override;

private:
    userver::storages::postgres::ClusterPtr pg_cluster_;
    YandexProfileClient yandex_client_;
};

}  // namespace ya_brander::auth
