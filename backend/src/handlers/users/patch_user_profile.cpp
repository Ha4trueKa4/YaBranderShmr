#include "patch_user_profile.h"

#include <string>

#include <userver/formats/json/value_builder.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/exceptions.hpp>

namespace ya_brander::handlers {

namespace {

userver::formats::json::Value MakeError(std::string_view code,
                                        std::string_view message,
                                        std::string_view field = {}) {
  userver::formats::json::ValueBuilder error;
  error["code"] = std::string(code);
  error["message"] = std::string(message);
  if (!field.empty()) {
    error["field"] = std::string(field);
  }
  return error.ExtractValue();
}

bool IsValidNickname(const std::string& nickname) {
  return nickname.size() >= 3 && nickname.size() <= 64;
}

}  // namespace

PatchUserProfileHandler::PatchUserProfileHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(
          context.FindComponent<userver::components::Postgres>("postgres-db-1")
              .GetCluster()) {}

userver::formats::json::Value PatchUserProfileHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value& request_json,
    userver::server::request::RequestContext& context) const {
  const auto* auth_user_id = context.GetDataOptional<std::string>("user_id");
  if (!auth_user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  const auto& target_id = request.GetPathArg("id");

  try {
    const auto user_result = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT id::text FROM users WHERE id = $1::uuid", target_id);

    if (user_result.IsEmpty()) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return MakeError("USER_NOT_FOUND", "User not found");
    }

    const auto canonical_target_id = user_result[0]["id"].As<std::string>();
    if (canonical_target_id != *auth_user_id) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
      return MakeError("FORBIDDEN", "You can only update your own profile");
    }

    const auto nickname = request_json["nickname"].As<std::string>("");
    if (!IsValidNickname(nickname)) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
      return MakeError("INVALID_NICKNAME", "Nickname must be 3-64 chars",
                       "nickname");
    }

    const auto updated = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "UPDATE users SET login = $1 WHERE id = $2::uuid "
        "RETURNING id::text, login",
        nickname, canonical_target_id);

    userver::formats::json::ValueBuilder response;
    response["id"] = updated[0]["id"].As<std::string>();
    response["nickname"] = updated[0]["login"].As<std::string>();
    return response.ExtractValue();
  } catch (const userver::storages::postgres::UniqueViolation&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kConflict);
    return MakeError("NICKNAME_EXISTS", "Nickname is already taken",
                     "nickname");
  } catch (const userver::storages::postgres::DataException&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeError("INVALID_UUID", "Invalid user ID format", "id");
  }
}

}  // namespace ya_brander::handlers
