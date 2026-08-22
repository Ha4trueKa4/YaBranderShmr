#include "register_handler.hpp"

#include <regex>

#include <userver/components/component.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/formats/json.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include "password_hasher.hpp"
#include "../../utils/errors.hpp"

namespace ya_brander::auth {

using utils::MakeError;

namespace {

const std::regex kEmailRegex(R"(^[^@\s]+@[^@\s]+\.[^@\s]+$)");

bool IsValidEmail(const std::string& email) {
    return email.size() <= 128 && std::regex_match(email, kEmailRegex);
}

bool IsValidNickname(const std::string& nickname) {
    return nickname.size() >= 3 && nickname.size() <= 64;
}

}  // namespace

RegisterHandler::RegisterHandler(const userver::components::ComponentConfig& config,
                                  const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()) {}

userver::formats::json::Value RegisterHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext&) const {

    const auto email = request_json["email"].As<std::string>("");
    const auto password = request_json["password"].As<std::string>("");
    const auto nickname = request_json["nickname"].As<std::string>("");

    if (!IsValidEmail(email)) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_EMAIL", "Email is invalid", "email");
    }
    if (!PasswordHasher::IsValidPassword(password)) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_PASSWORD", "Password must be 8-128 chars and contain letters and digits", "password");
    }
    if (!IsValidNickname(nickname)) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_NICKNAME", "Nickname must be 3-64 chars", "nickname");
    }

    const auto password_hash = PasswordHasher::Hash(password);

    try {
        auto result = pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            "INSERT INTO users(login, email, password_hash) VALUES($1, $2, $3) RETURNING id::text",
            nickname, email, password_hash);

        const auto user_id = result.Front()[0].As<std::string>();
        const auto token = jwt_service_.IssueToken(user_id);

        request.SetResponseStatus(userver::server::http::HttpStatus::kCreated);

        userver::formats::json::ValueBuilder response;
        response["access_token"] = token;
        response["token_type"] = "Bearer";
        response["user"]["id"] = user_id;
        response["user"]["email"] = email;
        response["user"]["nickname"] = nickname;
        return response.ExtractValue();

    } catch (const userver::storages::postgres::UniqueViolation& ex) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
        const auto constraint = ex.GetConstraint();
        if (constraint.find("login") != std::string::npos) {
            return MakeError("NICKNAME_EXISTS", "Nickname is already taken", "nickname");
        }
        return MakeError("EMAIL_EXISTS", "Email is already registered", "email");
    }
}

}  // namespace ya_brander::auth
