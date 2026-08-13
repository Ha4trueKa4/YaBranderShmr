#include "subscription_storage_component.h"

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

std::vector<UserShortProfile> SubscriptionStorageComponent::GetSubscribers(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      R"(
      SELECT u.id::text, u.login as name
      FROM subscriptions s
      JOIN users u ON u.id = s.subscriber_id
      WHERE s.target_user_id = $1::uuid
      ORDER BY s.created_at DESC
      )",
      user_id
  );

  std::vector<UserShortProfile> result;
  result.reserve(res.Size());
  for (const auto& row : res) {
    result.push_back({
        row["id"].As<std::string>(),
        row["name"].As<std::string>()
    });
  }
  return result;
}

std::vector<UserShortProfile> SubscriptionStorageComponent::GetSubscriptions(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      R"(
      SELECT u.id::text, u.login as name
      FROM subscriptions s
      JOIN users u ON u.id = s.target_user_id
      WHERE s.subscriber_id = $1::uuid
      ORDER BY s.created_at DESC
      )",
      user_id
  );

  std::vector<UserShortProfile> result;
  result.reserve(res.Size());
  for (const auto& row : res) {
    result.push_back({
        row["id"].As<std::string>(),
        row["name"].As<std::string>()
    });
  }
  return result;
}


}  // namespace ya_brander::components