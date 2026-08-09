// auth_types.hpp
#pragma once

#include <userver/formats/json.hpp>
#include <userver/formats/json/serialize.hpp>
#include <string>
#include <optional>

namespace ya_brander::auth {

struct RegisterRequest {
    std::string email;
    std::string password;
    std::string nickname;
};

struct LoginRequest {
    std::string email;
    std::string password;
};

struct AuthResponse {
    std::string token;
    std::string user_id;
    std::string email;
    std::string nickname;
};

inline RegisterRequest ParseRegisterRequest(const userver::formats::json::Value& json) {
    return {
        json["email"].As<std::string>(),
        json["password"].As<std::string>(),
        json["nickname"].As<std::string>()
    };
}

inline LoginRequest ParseLoginRequest(const userver::formats::json::Value& json) {
    return {
        json["email"].As<std::string>(),
        json["password"].As<std::string>()
    };
}

inline userver::formats::json::Value SerializeAuthResponse(
    const AuthResponse& response,
    userver::formats::json::ValueBuilder& builder
) {
    builder["token"] = response.token;
    builder["user_id"] = response.user_id;
    builder["email"] = response.email;
    builder["nickname"] = response.nickname;
    return builder.ExtractValue();
}

} // namespace ya_brander::auth
