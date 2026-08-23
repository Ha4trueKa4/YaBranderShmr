#include "yandex_oauth_handler.hpp"

#include <random>
#include <stdexcept>

#include <userver/clients/http/component.hpp>
#include <userver/components/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/logging/log.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include <ya_brander/sql_queries.hpp>

#include "../../utils/errors.hpp"
#include "../../utils/postgres_retry.hpp"

namespace ya_brander::auth {

using utils::MakeError;

namespace {

constexpr std::string_view kLoginUniqueConstraint = "users_login_key";
constexpr std::string_view kEmailUniqueConstraint = "users_email_key";
constexpr std::string_view kProviderIdentityIndex = "idx_users_provider_identity";

class EmailOwnedByAnotherAccountError final : public std::runtime_error {
 public:
  EmailOwnedByAnotherAccountError()
      : std::runtime_error("email is already registered via another sign-in method") {}
};

class LoginGenerationExhaustedError final : public std::runtime_error {
 public:
  LoginGenerationExhaustedError()
      : std::runtime_error("failed to generate a unique login after multiple attempts") {}
};

std::string GenerateLoginSuffix() {
    static thread_local std::mt19937 rng{std::random_device{}()};
    std::uniform_int_distribution<int> dist(1000, 9999);
    return std::to_string(dist(rng));
}

}  // namespace

YandexOAuthHandler::YandexOAuthHandler(const userver::components::ComponentConfig& config,
                                        const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()),
      yandex_client_(context.FindComponent<userver::components::HttpClient>().GetHttpClient()) {}

YandexOAuthHandler::UserRecord YandexOAuthHandler::FindOrCreateUser(const YandexProfile& profile) const {
    // 1. Пользователь уже входил через Yandex ID раньше.
    {
        auto result = utils::ExecuteSelectWithRetry(
            "select-yandex-user", [&] {
              return pg_cluster_->Execute(
                  userver::storages::postgres::ClusterHostType::kMaster,
                  sql::kSelectYandexUser, profile.provider_id);
            });
        if (result.Size() > 0) {
            const auto row = result.Front();
            return {row[0].As<std::string>(), row[1].As<std::string>(), row[2].As<std::string>()};
        }
    }

    // 2. Есть аккаунт с таким email, зарегистрированный другим способом — не
    //    привязываем Yandex ID автоматически (см. EmailOwnedByAnotherAccountError),
    //    отдаём наверх ошибку, чтобы пользователь подтвердил привязку явно.
    {
        auto result = utils::ExecuteSelectWithRetry(
            "select-password-user-by-email", [&] {
              return pg_cluster_->Execute(
                  userver::storages::postgres::ClusterHostType::kMaster,
                  sql::kSelectPasswordUserByEmail, profile.email);
            });
        if (result.Size() > 0) {
            throw EmailOwnedByAnotherAccountError{};
        }
    }

    // 3. Ни аккаунта, ни привязки нет — создаём нового пользователя.
    std::string login = !profile.login.empty() ? profile.login : "yandex_" + profile.provider_id;

    for (int attempt = 0; attempt < 5; ++attempt) {
        try {
            auto result = pg_cluster_->Execute(
                userver::storages::postgres::ClusterHostType::kMaster,
                sql::kRegisterYandexUser,
                login, profile.email, profile.provider_id);
            const auto row = result.Front();
            return {row[0].As<std::string>(), row[1].As<std::string>(), row[2].As<std::string>()};
        } catch (const userver::storages::postgres::UniqueViolation& ex) {
            const auto constraint = ex.GetConstraint();
            if (constraint == kLoginUniqueConstraint) {
                login += "_" + GenerateLoginSuffix();
                continue;
            }
            if (constraint == kProviderIdentityIndex) {
                auto result = utils::ExecuteSelectWithRetry(
                    "select-yandex-user", [&] {
                      return pg_cluster_->Execute(
                          userver::storages::postgres::ClusterHostType::kMaster,
                          sql::kSelectYandexUser, profile.provider_id);
                    });
                const auto row = result.Front();
                return {row[0].As<std::string>(), row[1].As<std::string>(), row[2].As<std::string>()};
            }
            if (constraint == kEmailUniqueConstraint) {
                throw EmailOwnedByAnotherAccountError{};
            }
            throw;
        }
    }
    throw LoginGenerationExhaustedError{};
}

userver::formats::json::Value YandexOAuthHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext&) const {

    const auto code = request_json["code"].As<std::string>("");
    const auto sdk_access_token = request_json["access_token"].As<std::string>("");
    if (code.empty() && sdk_access_token.empty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_REQUEST", "Either code or access_token is required");
    }

    UserRecord user;
    try {
        const auto profile = yandex_client_.FetchProfile(code, sdk_access_token);

        if (profile.email.empty()) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
            return MakeError("YANDEX_EMAIL_SCOPE_REQUIRED",
                                  "Yandex ID app must request the login:email scope");
        }

        user = FindOrCreateUser(profile);
    } catch (const EmailOwnedByAnotherAccountError&) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
        return MakeError("EMAIL_EXISTS",
                              "This email is already registered with a password. "
                              "Log in with your password and link Yandex ID from account settings.");
    } catch (const LoginGenerationExhaustedError& ex) {
        LOG_ERROR() << "Yandex OAuth flow failed: " << ex.what();
        request.SetResponseStatus(userver::server::http::HttpStatus::kInternalServerError);
        return MakeError("INTERNAL_ERROR", "Something went wrong on our side. Please try again.");
    } catch (const utils::DatabaseUnavailableError&) {
        throw;
    } catch (const std::exception& ex) {
        LOG_ERROR() << "Yandex OAuth flow failed: " << ex.what();
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadGateway);
        return MakeError("YANDEX_OAUTH_FAILED",
                              "Yandex authentication is temporarily unavailable. Please try again.");
    }

    const auto token = jwt_service_.IssueToken(user.id);

    userver::formats::json::ValueBuilder response;
    response["access_token"] = token;
    response["token_type"] = "Bearer";
    response["user"]["id"] = user.id;
    response["user"]["email"] = user.email;
    response["user"]["nickname"] = user.login;
    return response.ExtractValue();
}

}  // namespace ya_brander::auth
