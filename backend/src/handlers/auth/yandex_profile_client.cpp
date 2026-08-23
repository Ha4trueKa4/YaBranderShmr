#include "yandex_profile_client.hpp"

#include <cctype>
#include <cstdlib>
#include <cstring>
#include <stdexcept>

#include <jwt-cpp/jwt.h>

#include <userver/crypto/base64.hpp>
#include <userver/formats/json.hpp>

namespace ya_brander::auth {

namespace {

constexpr std::string_view kTokenUrl = "https://oauth.yandex.ru/token";
constexpr std::string_view kProfileUrl = "https://login.yandex.ru/info?format=json";
constexpr std::string_view kProfileJwtUrl = "https://login.yandex.ru/info?format=jwt";

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

// uid в JWT-профиле от Яндекса — числовой ID пользователя, и разные клиенты
// (в частности iOS SDK) кодируют его в payload как JSON-число, а не строку.
// claim::as_string() в этом случае кидает std::bad_cast, поэтому тип клейма
// проверяем сами и приводим к строке вручную.
std::string ClaimToString(const jwt::claim& claim) {
    switch (claim.get_type()) {
        case jwt::json::type::string:
            return claim.as_string();
        case jwt::json::type::integer:
            return std::to_string(claim.as_integer());
        case jwt::json::type::number:
            return std::to_string(static_cast<int64_t>(claim.as_number()));
        default:
            throw std::runtime_error("Unexpected JWT claim type");
    }
}

}  // namespace

YandexProfileClient::YandexProfileClient(userver::clients::http::Client& http_client)
    : http_client_(http_client) {
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

YandexProfile YandexProfileClient::FetchProfile(const std::string& code,
                                                  const std::string& access_token) const {
    if (!code.empty()) {
        return FetchProfileFromJson(ExchangeCodeForToken(code));
    }
    return FetchProfileFromJwt(access_token);
}

std::string YandexProfileClient::ExchangeCodeForToken(const std::string& code) const {
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
                                  std::to_string(response->status_code()));
    }

    auto json = userver::formats::json::FromString(response->body());
    return json["access_token"].As<std::string>();
}

YandexProfile YandexProfileClient::FetchProfileFromJson(const std::string& access_token) const {
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

YandexProfile YandexProfileClient::FetchProfileFromJwt(const std::string& access_token) const {
    auto request = http_client_.CreateRequest();
    request.method(userver::clients::http::HttpMethod::kGet);
    request.url(std::string(kProfileJwtUrl));
    request.headers({{"Authorization", "OAuth " + access_token}});
    request.timeout(10000);

    auto response = request.perform();
    if (response->status_code() != 200) {
        throw std::runtime_error("Failed to fetch Yandex profile (jwt): HTTP " +
                                  std::to_string(response->status_code()));
    }

    // login.yandex.ru/info?format=jwt отдаёт не JSON, а сам JWT телом ответа.
    // jwt_secret по умолчанию (мы его не переопределяли) равен client_secret
    // приложения — им и проверяем подпись, без обращения к Яндексу.
    const auto decoded = jwt::decode(response->body());
    auto verifier = jwt::verify().allow_algorithm(jwt::algorithm::hs256{client_secret_});
    verifier.verify(decoded);

    if (!decoded.has_payload_claim("uid")) {
        throw std::runtime_error("Yandex profile JWT is missing the 'uid' claim");
    }

    YandexProfile profile;
    profile.provider_id = ClaimToString(decoded.get_payload_claim("uid"));
    if (decoded.has_payload_claim("email")) {
        profile.email = ClaimToString(decoded.get_payload_claim("email"));
    }
    if (decoded.has_payload_claim("login")) {
        profile.login = ClaimToString(decoded.get_payload_claim("login"));
    }
    return profile;
}

}  // namespace ya_brander::auth
