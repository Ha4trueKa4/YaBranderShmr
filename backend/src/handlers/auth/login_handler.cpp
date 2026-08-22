#include "login_handler.hpp"

#include <userver/components/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/component.hpp>

#include "password_hasher.hpp"
#include "../../utils/errors.hpp"

namespace ya_brander::auth {

using utils::MakeError;

LoginHandler::LoginHandler(const userver::components::ComponentConfig& config,
                            const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()) {}

userver::formats::json::Value LoginHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext&) const {

    const auto email = request_json["email"].As<std::string>("");
    const auto password = request_json["password"].As<std::string>("");


    if (email.empty() || password.empty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
        return MakeError("INVALID_CREDENTIALS", "Invalid email or password");
    }

    auto result = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT id::text, login, password_hash FROM users WHERE email = $1", email);

    if (result.Size() == 0) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
        return MakeError("INVALID_CREDENTIALS", "Invalid email or password");
    }

    const auto row = result.Front();
    const auto user_id = row[0].As<std::string>();
    const auto nickname = row[1].As<std::string>();
    const auto password_hash = row[2].As<std::string>();

    if (!PasswordHasher::Verify(password, password_hash)) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
        return MakeError("INVALID_CREDENTIALS", "Invalid email or password");
    }

    const auto token = jwt_service_.IssueToken(user_id);

    userver::formats::json::ValueBuilder response;
    response["access_token"] = token;
    response["token_type"] = "Bearer";
    response["user"]["id"] = user_id;
    response["user"]["email"] = email;
    response["user"]["nickname"] = nickname;
    return response.ExtractValue();
}

}  // namespace ya_brander::auth
