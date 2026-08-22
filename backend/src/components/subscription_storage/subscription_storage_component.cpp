#include "subscription_storage_component.h"

#include <cstdint>

#include <ya_brander/sql_queries.hpp>

#include "../../utils/postgres_retry.hpp"

namespace ya_brander::components {

SubscriptionStorageComponent::SubscriptionStorageComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

bool SubscriptionStorageComponent::IsUserExists(const std::string& user_id) const {
  auto res = utils::ExecuteSelectWithRetry("check-user-exists", [&] {
    return pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        sql::kCheckUserExists, user_id);
  });
  return !res.IsEmpty();
}

bool SubscriptionStorageComponent::IsSubscribed(const std::string& subscriber_id, const std::string& target_id) const {
  auto res = utils::ExecuteSelectWithRetry(
      "check-subscription-exists", [&] {
        return pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            sql::kCheckSubscriptionExists, subscriber_id, target_id);
      });
  return !res.IsEmpty();
}

size_t SubscriptionStorageComponent::GetSubscribersCount(const std::string& user_id) const {
  auto res = utils::ExecuteSelectWithRetry("count-subscribers", [&] {
    return pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        sql::kCountSubscribers, user_id);
  });
  return res.AsSingleRow<std::int64_t>();
}

size_t SubscriptionStorageComponent::GetSubscriptionsCount(const std::string& user_id) const {
  auto res = utils::ExecuteSelectWithRetry("count-subscriptions", [&] {
    return pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        sql::kCountSubscriptions, user_id);
  });
  return res.AsSingleRow<std::int64_t>();
}

std::vector<UserShortProfile> SubscriptionStorageComponent::GetSubscribers(
    const std::string& user_id, const std::string& auth_user_id) const {

  auto res = utils::ExecuteSelectWithRetry(
      auth_user_id.empty() ? "select-subscribers"
                           : "select-subscribers-with-auth",
      [&] {
        return auth_user_id.empty()
                   ? pg_cluster_->Execute(
                         userver::storages::postgres::ClusterHostType::kSlave,
                         sql::kSelectSubscribers, user_id)
                   : pg_cluster_->Execute(
                         userver::storages::postgres::ClusterHostType::kSlave,
                         sql::kSelectSubscribersWithAuth, user_id,
                         auth_user_id);
      });

  std::vector<UserShortProfile> result;
  result.reserve(res.Size());
  for (const auto& row : res) {
    UserShortProfile profile;
    profile.id = row["id"].As<std::string>();
    profile.name = row["name"].As<std::string>();
    if (!auth_user_id.empty()) {
      profile.is_subscribed = row["is_subscribed"].As<bool>();
    }
    result.push_back(std::move(profile));
  }
  return result;
}

std::vector<UserShortProfile> SubscriptionStorageComponent::GetSubscriptions(
    const std::string& user_id, const std::string& auth_user_id) const {

  auto res = utils::ExecuteSelectWithRetry(
      auth_user_id.empty() ? "select-subscriptions"
                           : "select-subscriptions-with-auth",
      [&] {
        return auth_user_id.empty()
                   ? pg_cluster_->Execute(
                         userver::storages::postgres::ClusterHostType::kSlave,
                         sql::kSelectSubscriptions, user_id)
                   : pg_cluster_->Execute(
                         userver::storages::postgres::ClusterHostType::kSlave,
                         sql::kSelectSubscriptionsWithAuth, user_id,
                         auth_user_id);
      });

  std::vector<UserShortProfile> result;
  result.reserve(res.Size());
  for (const auto& row : res) {
    UserShortProfile profile;
    profile.id = row["id"].As<std::string>();
    profile.name = row["name"].As<std::string>();
    if (!auth_user_id.empty()) {
      profile.is_subscribed = row["is_subscribed"].As<bool>();
    }
    result.push_back(std::move(profile));
  }
  return result;
}


}  // namespace ya_brander::components
