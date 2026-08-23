#include "set_password_handler.hpp"

#include <optional>

#include <userver/components/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/component.hpp>

#include <ya_brander/sql_queries.hpp>

#include "../../utils/errors.hpp"
#include "../../utils/postgres_retry.hpp"
#include "password_hasher.hpp"

namespace ya_brander::auth {

using utils::MakeError;
using utils::MakeMessage;

SetPasswordHandler::SetPasswordHandler(const userver::components::ComponentConfig& config,
                                        const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()) {}

userver::formats::json::Value SetPasswordHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext& context) const {
    const auto& user_id = context.GetData<std::string>("user_id");

    const auto new_password = request_json["new_password"].As<std::string>("");
    const auto current_password = request_json["current_password"].As<std::string>("");

    if (!PasswordHasher::IsValidPassword(new_password)) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
        return MakeError("INVALID_PASSWORD",
                          "Password must be 8-128 chars and contain letters and digits",
                          "new_password");
    }

    const auto result =
        utils::ExecuteSelectWithRetry("select-user-password-hash", [&] {
            return pg_cluster_->Execute(
                userver::storages::postgres::ClusterHostType::kMaster,
                sql::kSelectUserPasswordHash, user_id);
        });

    if (result.IsEmpty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
        return MakeError("USER_NOT_FOUND", "User not found");
    }

    const auto existing_hash = result.Front()[0].As<std::optional<std::string>>();

    if (existing_hash.has_value()) {
        if (current_password.empty()) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
            return MakeError("CURRENT_PASSWORD_REQUIRED",
                              "Current password is required to change an existing password",
                              "current_password");
        }
        if (!PasswordHasher::Verify(current_password, *existing_hash)) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
            return MakeError("INVALID_CURRENT_PASSWORD", "Current password is incorrect",
                              "current_password");
        }
    }

    const auto new_hash = PasswordHasher::Hash(new_password);
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kUpdateUserPassword, new_hash, user_id);

    return MakeMessage(existing_hash.has_value() ? "Password changed successfully"
                                                   : "Password set successfully");
}

}  // namespace ya_brander::auth
