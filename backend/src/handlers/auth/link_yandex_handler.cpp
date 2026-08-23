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

#include <ya_brander/sql_queries.hpp>

#include "../../utils/errors.hpp"
#include "../../utils/postgres_retry.hpp"

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

    YandexProfile profile;
    try {
        profile = yandex_client_.FetchProfile(code, sdk_access_token);
    } catch (const InvalidYandexCredentialError&) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_YANDEX_CREDENTIAL",
                          "The provided Yandex code/access_token is invalid or expired");
    } catch (const std::exception& ex) {
        LOG_ERROR() << "Yandex OAuth flow failed: " << ex.what();
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadGateway);
        return MakeError("YANDEX_OAUTH_FAILED",
                          "Yandex authentication is temporarily unavailable. Please try again.");
    }

    // Атомарная привязка: WHERE provider_id IS NULL в самом UPDATE, без
    // отдельного SELECT перед записью — иначе между проверкой и записью
    // конкурентный запрос мог бы успеть изменить состояние (TOCTOU).
    try {
        const auto result = pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            sql::kLinkYandexAccount, profile.provider_id, user_id);
        if (result.Size() > 0) {
            return MakeMessage("Yandex ID linked successfully");
        }
    } catch (const userver::storages::postgres::UniqueViolation& ex) {
        // Гонка: два запроса одновременно линкуют один и тот же Yandex-аккаунт
        // к разным пользователям — проигравший ловит нарушение уникального индекса.
        if (ex.GetConstraint() == kProviderIdentityIndex) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
            return MakeError("YANDEX_ACCOUNT_ALREADY_LINKED",
                              "This Yandex ID is already linked to another account");
        }
        throw;
    }

    // UPDATE не тронул ни одной строки: либо пользователя нет, либо у него
    // уже есть какая-то привязка (её отсеяло WHERE provider_id IS NULL).
    // Этот SELECT нужен только чтобы сформировать осмысленный ответ — решение
    // уже принято атомарно выше, гонки тут нет.
    const auto existing = utils::ExecuteSelectWithRetry("select-provider-id", [&] {
        return pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            sql::kSelectProviderId, user_id);
    });

    if (existing.IsEmpty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
        return MakeError("USER_NOT_FOUND", "User not found");
    }

    const auto existing_provider_id = existing.Front()[0].As<std::optional<std::string>>();
    if (existing_provider_id.has_value() && *existing_provider_id == profile.provider_id) {
        // Повторный вызов с тем же Yandex-аккаунтом — идемпотентно ок.
        return MakeMessage("Yandex ID is already linked to this account");
    }

    request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
    return MakeError("ACCOUNT_ALREADY_LINKED",
                      "This account is already linked to a different Yandex ID");
}

}  // namespace ya_brander::auth
