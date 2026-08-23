#include "link_yandex_handler.hpp"

#include <optional>

#include <userver/clients/http/component.hpp>
#include <userver/components/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/formats/serialize/common_containers.hpp>
#include <userver/logging/log.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include "../../utils/errors.hpp"

namespace ya_brander::auth {

using utils::MakeError;
using utils::MakeMessage;

namespace {

constexpr std::string_view kProviderIdentityIndex = "idx_users_provider_identity";

}  // namespace

LinkYandexHandler::LinkYandexHandler(const userver::components::ComponentConfig& config,
                                      const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()),
      yandex_client_(context.FindComponent<userver::components::HttpClient>().GetHttpClient()) {}

userver::formats::json::Value LinkYandexHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext& context) const {
    const auto& user_id = context.GetData<std::string>("user_id");

    const auto code = request_json["code"].As<std::string>("");
    const auto sdk_access_token = request_json["access_token"].As<std::string>("");
    if (code.empty() && sdk_access_token.empty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_REQUEST", "Either code or access_token is required");
    }

    const auto user_result = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT provider_id FROM users WHERE id = $1::uuid", user_id);
    if (user_result.IsEmpty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
        return MakeError("USER_NOT_FOUND", "User not found");
    }
    const auto existing_provider_id = user_result.Front()[0].As<std::optional<std::string>>();

    YandexProfile profile;
    try {
        profile = yandex_client_.FetchProfile(code, sdk_access_token);
    } catch (const std::exception& ex) {
        LOG_ERROR() << "Yandex OAuth flow failed: " << ex.what();
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadGateway);
        return MakeError("YANDEX_OAUTH_FAILED",
                          "Yandex authentication is temporarily unavailable. Please try again.");
    }

    if (existing_provider_id.has_value()) {
        if (*existing_provider_id == profile.provider_id) {
            // Повторная привязка того же Yandex-аккаунта — идемпотентно ок.
            return MakeMessage("Yandex ID is already linked to this account");
        }
        request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
        return MakeError("ACCOUNT_ALREADY_LINKED",
                          "This account is already linked to a different Yandex ID");
    }

    try {
        pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "UPDATE users SET provider = 'yandex', provider_id = $1 WHERE id = $2::uuid",
            profile.provider_id, user_id);
    } catch (const userver::storages::postgres::UniqueViolation& ex) {
        if (ex.GetConstraint() == kProviderIdentityIndex) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
            return MakeError("YANDEX_ACCOUNT_ALREADY_LINKED",
                              "This Yandex ID is already linked to another account");
        }
        throw;
    }

    return MakeMessage("Yandex ID linked successfully");
}

}  // namespace ya_brander::auth
