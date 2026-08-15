#include "get_user_profile.h"

#include <chrono>
#include <stdexcept>
#include <optional>
#include <vector>
#include <unordered_map>

#include <userver/formats/json/serialize.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/storages/postgres/exceptions.hpp>

namespace ya_brander::handlers {

GetUserProfileHandler::GetUserProfileHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()),
      subscription_storage_(context.FindComponent<components::SubscriptionStorageComponent>()),
      version_storage_(context.FindComponent<components::VersionStorageComponent>()) {}

userver::formats::json::Value GetUserProfileHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
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
      userver::formats::json::ValueBuilder err;
      err["code"] = "USER_NOT_FOUND";
      err["message"] = "Пользователь не найден";
      return err.ExtractValue();
    }

    auto user_row = user_res[0];
    auto user_id_str = user_row["id"].As<std::string>();
    auto login = user_row["login"].As<std::string>();

    auto subscribers_count = subscription_storage_.GetSubscribersCount(target_id);
    auto subscriptions_count = subscription_storage_.GetSubscriptionsCount(target_id);

    bool is_self = (auth_user_id == target_id);

    std::optional<bool> is_subscribed = std::nullopt;
    if (!is_self && !auth_user_id.empty()) {
      is_subscribed = subscription_storage_.IsSubscribed(auth_user_id, target_id);
    }

    auto proj_res = is_self
        ? pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            "SELECT id::text, author_id::text, name, COALESCE(description, '') as description, is_private, created_at, last_version_key "
            "FROM projects WHERE author_id = $1::uuid ORDER BY created_at DESC",
            target_id
        )
        : pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            "SELECT id::text, author_id::text, name, COALESCE(description, '') as description, is_private, created_at, last_version_key "
            "FROM projects WHERE author_id = $1::uuid AND is_private = FALSE ORDER BY created_at DESC",
            target_id
        );

    std::vector<std::string> keys_public;
    std::vector<std::string> keys_private;

    for (const auto& row : proj_res) {
      if (!row["last_version_key"].IsNull()) {
        auto key = row["last_version_key"].As<std::string>();
        if (row["is_private"].As<bool>()) {
          keys_private.push_back(key);
        } else {
          keys_public.push_back(key);
        }
      }
    }

    std::unordered_map<std::string, std::string> urls;

    if (!keys_public.empty()) {
      auto pub_urls = version_storage_.GetManyUrls(keys_public, true);
      urls.insert(pub_urls.begin(), pub_urls.end());
    }

    if (!keys_private.empty()) {
      auto priv_urls = version_storage_.GetManyUrls(keys_private, false);
      urls.insert(priv_urls.begin(), priv_urls.end());
    }

    userver::formats::json::ValueBuilder builder;
    builder["id"] = user_id_str;
    builder["name"] = login;
    builder["subscribers_count"] = subscribers_count;
    builder["subscriptions_count"] = subscriptions_count;

    if (is_subscribed.has_value()) {
      builder["is_subscribed"] = *is_subscribed;
    }

    userver::formats::json::ValueBuilder projects_array(userver::formats::common::Type::kArray);
    for (const auto& row : proj_res) {
      userver::formats::json::ValueBuilder proj;
      proj["id"] = row["id"].As<std::string>();
      proj["author_id"] = row["author_id"].As<std::string>();
      proj["name"] = row["name"].As<std::string>();
      proj["description"] = row["description"].As<std::string>();
      proj["is_public"] = !row["is_private"].As<bool>();
      proj["created_at"] = row["created_at"].As<std::chrono::system_clock::time_point>();

      if (!row["last_version_key"].IsNull()) {
        auto key = row["last_version_key"].As<std::string>();
        auto it = urls.find(key);
        if (it != urls.end() && !it->second.empty()) {
          proj["url"] = it->second;
        }
      }

      projects_array.PushBack(std::move(proj));
    }

    builder["projects"] = std::move(projects_array);

    return builder.ExtractValue();

  } catch (const userver::storages::postgres::DataException& e) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["code"] = "INVALID_UUID";
    err["message"] = "Некорректный формат ID пользователя";
    return err.ExtractValue();
  } catch (const std::exception& e) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kInternalServerError);
    userver::formats::json::ValueBuilder err;
    err["code"] = "INTERNAL_ERROR";
    err["message"] = "Внутренняя ошибка сервера";
    return err.ExtractValue();
  }
}

}  // namespace ya_brander::handlers