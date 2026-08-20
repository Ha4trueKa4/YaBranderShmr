#include "yandex_oauth_handler.hpp"

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <random>
#include <stdexcept>

#include <userver/clients/http/component.hpp>
#include <userver/components/component.hpp>
#include <userver/crypto/base64.hpp>
#include <userver/formats/json.hpp>
#include <userver/logging/log.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include "../../utils/errors.hpp"

namespace ya_brander::auth {

using utils::MakeError;

namespace {

constexpr std::string_view kTokenUrl = "https://oauth.yandex.ru/token";
constexpr std::string_view kProfileUrl = "https://login.yandex.ru/info?format=json";

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

userver::formats::json::Value MakeErrorJson(std::string_view code, std::string_view message) {
    userver::formats::json::ValueBuilder builder;
    builder["code"] = std::string(code);
    builder["message"] = std::string(message);
    return builder.ExtractValue();
}

std::string UrlEncode(const std::string& value) {
    static const char* kHex = "0123456789ABCDEF";
    std::string result;
    for (unsigned char c : value) {
        if (std::isalnum(c) || c == '-' || c == '_' || c == '.' || c == '~') {
            result += static_cast<char>(c);
        } else {
            result += '%';
            result += kHex[c >> 4];
            result += kHex[c & 0xF];
        }
    }
    return result;
}

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
      http_client_(context.FindComponent<userver::components::HttpClient>().GetHttpClient()) {
    const char* client_id = std::getenv("YANDEX_OAUTH_CLIENT_ID");
    const char* client_secret = std::getenv("YANDEX_OAUTH_CLIENT_SECRET");
    if (!client_id || std::strlen(client_id) == 0) {
        throw std::runtime_error("YANDEX_OAUTH_CLIENT_ID environment variable is not set!");
    }
    if (!client_secret || std::strlen(client_secret) == 0) {
        throw std::runtime_error("YANDEX_OAUTH_CLIENT_SECRET environment variable is not set!");
    }
    client_id_ = client_id;
    client_secret_ = client_secret;
}

std::string YandexOAuthHandler::ExchangeCodeForToken(const std::string& code) const {
    const std::string body = "grant_type=authorization_code"
                              "&code=" + UrlEncode(code);

    const std::string basic_auth = userver::crypto::base64::Base64Encode(
        client_id_ + ":" + client_secret_);

    auto request = http_client_.CreateRequest();
    request.method(userver::clients::http::HttpMethod::kPost);
    request.url(std::string(kTokenUrl));
    request.headers({
        {"Content-Type", "application/x-www-form-urlencoded"},
        {"Authorization", "Basic " + basic_auth}
    });
    request.data(body);
    request.timeout(10000);

    auto response = request.perform();
    if (response->status_code() != 200) {
        throw std::runtime_error("Yandex token exchange failed: HTTP " +
                                  std::to_string(response->status_code()) +
                                  ", body: " + response->body());
    }

    auto json = userver::formats::json::FromString(response->body());
    return json["access_token"].As<std::string>();
}

YandexOAuthHandler::YandexProfile YandexOAuthHandler::FetchProfile(const std::string& access_token) const {
    auto request = http_client_.CreateRequest();
    request.method(userver::clients::http::HttpMethod::kGet);
    request.url(std::string(kProfileUrl));
    request.headers({{"Authorization", "OAuth " + access_token}});
    request.timeout(10000);

    auto response = request.perform();
    if (response->status_code() != 200) {
        throw std::runtime_error("Failed to fetch Yandex profile: HTTP " +
                                  std::to_string(response->status_code()));
    }

    auto json = userver::formats::json::FromString(response->body());

    YandexProfile profile;
    profile.provider_id = json["id"].As<std::string>();
    profile.email = json["default_email"].As<std::string>("");
    profile.login = json["login"].As<std::string>("");
    return profile;
}

YandexOAuthHandler::UserRecord YandexOAuthHandler::FindOrCreateUser(const YandexProfile& profile) const {
    // 1. Пользователь уже входил через Yandex ID раньше.
    {
        auto result = pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "SELECT id::text, login, email FROM users WHERE provider = 'yandex' AND provider_id = $1",
            profile.provider_id);
        if (result.Size() > 0) {
            const auto row = result.Front();
            return {row[0].As<std::string>(), row[1].As<std::string>(), row[2].As<std::string>()};
        }
    }

    // 2. Есть аккаунт с таким email, зарегистрированный другим способом — не
    //    привязываем Yandex ID автоматически (см. EmailOwnedByAnotherAccountError),
    //    отдаём наверх ошибку, чтобы пользователь подтвердил привязку явно.
    {
        auto result = pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "SELECT 1 FROM users WHERE email = $1 AND provider_id IS NULL",
            profile.email);
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
                "INSERT INTO users(login, email, password_hash, provider, provider_id) "
                "VALUES($1, $2, NULL, 'yandex', $3) "
                "RETURNING id::text, login, email",
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
                auto result = pg_cluster_->Execute(
                    userver::storages::postgres::ClusterHostType::kMaster,
                    "SELECT id::text, login, email FROM users WHERE provider = 'yandex' AND provider_id = $1",
                    profile.provider_id);
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
    if (code.empty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_CODE", "Authorization code is required");
    }

    UserRecord user;
    try {
        const auto access_token = ExchangeCodeForToken(code);
        const auto profile = FetchProfile(access_token);

        if (profile.email.empty()) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
            return MakeErrorJson("YANDEX_EMAIL_SCOPE_REQUIRED",
                                  "Yandex ID app must request the login:email scope");
        }

        user = FindOrCreateUser(profile);
    } catch (const EmailOwnedByAnotherAccountError&) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
        return MakeErrorJson("EMAIL_EXISTS",
                              "This email is already registered with a password. "
                              "Log in with your password and link Yandex ID from account settings.");
    } catch (const LoginGenerationExhaustedError& ex) {
        LOG_ERROR() << "Yandex OAuth flow failed: " << ex.what();
        request.SetResponseStatus(userver::server::http::HttpStatus::kInternalServerError);
        return MakeErrorJson("INTERNAL_ERROR", "Something went wrong on our side. Please try again.");
    } catch (const std::exception& ex) {
        LOG_ERROR() << "Yandex OAuth flow failed: " << ex.what();
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadGateway);
        return MakeErrorJson("YANDEX_OAUTH_FAILED",
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
