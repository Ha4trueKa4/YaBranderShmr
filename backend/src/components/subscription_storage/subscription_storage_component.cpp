#include "subscription_storage_component.h"

#include <cstdint>

namespace ya_brander::components {

SubscriptionStorageComponent::SubscriptionStorageComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

bool SubscriptionStorageComponent::IsUserExists(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      "SELECT 1 FROM users WHERE id = $1::uuid",
      user_id
  );
  return !res.IsEmpty();
}

bool SubscriptionStorageComponent::IsSubscribed(const std::string& subscriber_id, const std::string& target_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      "SELECT 1 FROM subscriptions WHERE subscriber_id = $1::uuid AND target_user_id = $2::uuid",
      subscriber_id,
      target_id
  );
  return !res.IsEmpty();
}

size_t SubscriptionStorageComponent::GetSubscribersCount(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      "SELECT COUNT(*) FROM subscriptions WHERE target_user_id = $1::uuid",
      user_id
  );
  return res.AsSingleRow<std::int64_t>();
}

size_t SubscriptionStorageComponent::GetSubscriptionsCount(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      "SELECT COUNT(*) FROM subscriptions WHERE subscriber_id = $1::uuid",
      user_id
  );
  return res.AsSingleRow<std::int64_t>();
}

std::vector<UserShortProfile> SubscriptionStorageComponent::GetSubscribers(
    const std::string& user_id, const std::string& auth_user_id) const {

  auto res = auth_user_id.empty()
      ? pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            R"(
            SELECT u.id::text, u.login as name
            FROM subscriptions s
            JOIN users u ON u.id = s.subscriber_id
            WHERE s.target_user_id = $1::uuid
            ORDER BY s.created_at DESC
            )",
            user_id)
      : pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            R"(
            SELECT u.id::text, u.login as name,
                   EXISTS (
                       SELECT 1 FROM subscriptions auth_sub
                       WHERE auth_sub.subscriber_id = $2::uuid AND auth_sub.target_user_id = u.id
                   ) as is_subscribed
            FROM subscriptions s
            JOIN users u ON u.id = s.subscriber_id
            WHERE s.target_user_id = $1::uuid
            ORDER BY s.created_at DESC
            )",
            user_id, auth_user_id);

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

  auto res = auth_user_id.empty()
      ? pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            R"(
            SELECT u.id::text, u.login as name
            FROM subscriptions s
            JOIN users u ON u.id = s.target_user_id
            WHERE s.subscriber_id = $1::uuid
            ORDER BY s.created_at DESC
            )",
            user_id)
      : pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            R"(
            SELECT u.id::text, u.login as name,
                   EXISTS (
                       SELECT 1 FROM subscriptions auth_sub
                       WHERE auth_sub.subscriber_id = $2::uuid AND auth_sub.target_user_id = u.id
                   ) as is_subscribed
            FROM subscriptions s
            JOIN users u ON u.id = s.target_user_id
            WHERE s.subscriber_id = $1::uuid
            ORDER BY s.created_at DESC
            )",
            user_id, auth_user_id);

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