#pragma once

#include <string>
#include <vector>

#include <userver/components/component_list.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/components/component_context.hpp>
#include <userver/storages/postgres/component.hpp>

namespace ya_brander::components {

struct UserShortProfile {
  std::string id;
  std::string name;
  std::optional<bool> is_subscribed;
};

class SubscriptionStorageComponent final : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "subscription-storage";

  SubscriptionStorageComponent(const userver::components::ComponentConfig& config,
                               const userver::components::ComponentContext& context);

  bool IsUserExists(const std::string& user_id) const;

  bool IsSubscribed(const std::string& subscriber_id, const std::string& target_id) const;

  size_t GetSubscribersCount(const std::string& user_id) const;
  size_t GetSubscriptionsCount(const std::string& user_id) const;

  std::vector<UserShortProfile> GetSubscribers(const std::string& user_id, const std::string& auth_user_id) const;
  std::vector<UserShortProfile> GetSubscriptions(const std::string& user_id, const std::string& auth_user_id) const;

private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
};

}  // namespace ya_brander::components