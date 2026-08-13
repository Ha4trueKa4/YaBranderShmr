#include "get_user_profile.h"

#include <chrono>
#include <stdexcept>

#include <userver/formats/json/serialize.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/storages/postgres/exceptions.hpp>

namespace ya_brander::handlers {

GetUserProfileHandler::GetUserProfileHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()),
      subscription_storage_(context.FindComponent<components::SubscriptionStorageComponent>()) {}

std::string GetUserProfileHandler::HandleRequestThrow(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext& req_context) const {
    
  const auto& target_id = request.GetPathArg("id");

  std::string* auth_user_id_ptr = req_context.GetDataOptional<std::string>("user_id");
  std::string auth_user_id = auth_user_id_ptr ? *auth_user_id_ptr : "";

  try {
    auto user_res = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT id::text, login FROM users WHERE id = $1::uuid",
        target_id
    );

    if (user_res.IsEmpty()) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return R"({"code": "USER_NOT_FOUND", "message": "Пользователь не найден"})";
    }

    auto user_row = user_res[0];
    auto user_id_str = user_row["id"].As<std::string>();
    auto login = user_row["login"].As<std::string>();

    auto subscribers_count = subscription_storage_.GetSubscribersCount(target_id);
    auto subscriptions_count = subscription_storage_.GetSubscriptionsCount(target_id);

    bool is_self = (auth_user_id == target_id);

    auto proj_res = is_self
        ? pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            "SELECT id::text, author_id::text, name, COALESCE(description, '') as description, is_private, created_at "
            "FROM projects WHERE author_id = $1::uuid ORDER BY created_at DESC",
            target_id
        )
        : pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            "SELECT id::text, author_id::text, name, COALESCE(description, '') as description, is_private, created_at "
            "FROM projects WHERE author_id = $1::uuid AND is_private = FALSE ORDER BY created_at DESC",
            target_id
        );

    userver::formats::json::ValueBuilder builder;
    builder["id"] = user_id_str;
    builder["name"] = login;
    builder["subscribers_count"] = subscribers_count;
    builder["subscriptions_count"] = subscriptions_count;

    userver::formats::json::ValueBuilder projects_array(userver::formats::common::Type::kArray);
    for (const auto& row : proj_res) {
      userver::formats::json::ValueBuilder proj;
      proj["id"] = row["id"].As<std::string>();
      proj["author_id"] = row["author_id"].As<std::string>();
      proj["name"] = row["name"].As<std::string>();
      proj["description"] = row["description"].As<std::string>();
      proj["is_public"] = !row["is_private"].As<bool>();
      proj["created_at"] = row["created_at"].As<std::chrono::system_clock::time_point>();
      projects_array.PushBack(std::move(proj));
    }

    builder["projects"] = std::move(projects_array);

    return userver::formats::json::ToString(builder.ExtractValue());

  } catch (const userver::storages::postgres::DataException& e) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"code": "INVALID_UUID", "message": "Некорректный формат ID пользователя"})";
  } catch (const std::exception& e) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kInternalServerError);
    return R"({"code": "INTERNAL_ERROR", "message": "Внутренняя ошибка сервера"})";
  }
}

}  // namespace ya_brander::handlers