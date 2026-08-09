#include "register_handler.hpp"

#include <cctype>
#include <regex>

#include <userver/components/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/server/handlers/exceptions.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include "password_hasher.hpp"

namespace ya_brander::auth {

namespace {

const std::regex kEmailRegex(R"(^[^@\s]+@[^@\s]+\.[^@\s]+$)");

bool IsValidEmail(const std::string& email) {
    return email.size() <= 128 && std::regex_match(email, kEmailRegex);
}

bool IsValidPassword(const std::string& password) {
    if (password.size() < 8 || password.size() > 128) return false;
    bool has_letter = false;
    bool has_digit = false;
    for (unsigned char c : password) {
        if (std::isalpha(c)) has_letter = true;
        if (std::isdigit(c)) has_digit = true;
    }
    return has_letter && has_digit;
}

bool IsValidNickname(const std::string& nickname) {
    return nickname.size() >= 3 && nickname.size() <= 64;
}

std::string MakeErrorBody(std::string_view code, std::string_view message) {
    userver::formats::json::ValueBuilder builder;
    builder["code"] = std::string(code);
    builder["message"] = std::string(message);
    return userver::formats::json::ToString(builder.ExtractValue());
}

}  // namespace

RegisterHandler::RegisterHandler(const userver::components::ComponentConfig& config,
                                  const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()) {}

userver::formats::json::Value RegisterHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest&,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext&) const {
    using userver::server::handlers::ClientError;
    using userver::server::handlers::ConflictError;

    const auto email = request_json["email"].As<std::string>("");
    const auto password = request_json["password"].As<std::string>("");
    const auto nickname = request_json["nickname"].As<std::string>("");

    if (!IsValidEmail(email)) {
        throw ClientError(ClientError::ExternalBody{MakeErrorBody("INVALID_EMAIL", "Email is invalid")});
    }
    if (!IsValidPassword(password)) {
        throw ClientError(ClientError::ExternalBody{MakeErrorBody(
            "INVALID_PASSWORD", "Password must be 8-128 chars and contain letters and digits")});
    }
    if (!IsValidNickname(nickname)) {
        throw ClientError(
            ClientError::ExternalBody{MakeErrorBody("INVALID_NICKNAME", "Nickname must be 3-64 chars")});
    }

    const auto password_hash = PasswordHasher::Hash(password);

    try {
        auto result = pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "INSERT INTO users(login, email, password_hash) VALUES($1, $2, $3) RETURNING id::text",
            nickname, email, password_hash);

        const auto user_id = result.Front()[0].As<std::string>();
        const auto token = jwt_service_.IssueToken(user_id);

        userver::formats::json::ValueBuilder response;
        response["access_token"] = token;
        response["token_type"] = "Bearer";
        response["user"]["id"] = user_id;
        response["user"]["email"] = email;
        response["user"]["nickname"] = nickname;
        return response.ExtractValue();

    } catch (const userver::storages::postgres::UniqueViolation& ex) {
        const auto constraint = ex.GetConstraint();
        if (constraint.find("login") != std::string::npos) {
            throw ConflictError(
                ConflictError::ExternalBody{MakeErrorBody("NICKNAME_EXISTS", "Nickname is already taken")});
        }
        throw ConflictError(
            ConflictError::ExternalBody{MakeErrorBody("EMAIL_EXISTS", "Email is already registered")});
    }
}

}  // namespace ya_brander::auth
