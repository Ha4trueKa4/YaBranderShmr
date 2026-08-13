#include "subscription_storage_component.h"

namespace ya_brander::components {

SubscriptionStorageComponent::SubscriptionStorageComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

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

std::vector<std::string> SubscriptionStorageComponent::GetSubscribers(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      "SELECT subscriber_id::text FROM subscriptions "
      "WHERE target_user_id = $1::uuid "
      "ORDER BY created_at DESC",
      user_id
  );
  return res.AsContainer<std::vector<std::string>>();
}

std::vector<std::string> SubscriptionStorageComponent::GetSubscriptions(const std::string& user_id) const {
  auto res = pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kSlave,
      "SELECT target_user_id::text FROM subscriptions "
      "WHERE subscriber_id = $1::uuid "
      "ORDER BY created_at DESC",
      user_id
  );
  return res.AsContainer<std::vector<std::string>>();
}

void AppendSubscriptionStorage(userver::components::ComponentList& component_list) {
  component_list.Append<SubscriptionStorageComponent>();
}

}  // namespace ya_brander::components