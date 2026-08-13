#pragma once

#include <cstdint>
#include <string>
#include <string_view>
#include <vector>


#include <userver/components/component_list.hpp>
#include <userver/components/component_context.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/storages/postgres/component.hpp>

namespace ya_brander::components {

class SubscriptionStorageComponent : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "subscription-storage";

  SubscriptionStorageComponent(const userver::components::ComponentConfig& config,
                               const userver::components::ComponentContext& context);

  size_t GetSubscribersCount(const std::string& user_id) const;
  size_t GetSubscriptionsCount(const std::string& user_id) const;
  std::vector<std::string> GetSubscribers(const std::string& user_id) const;
  std::vector<std::string> GetSubscriptions(const std::string& user_id) const;

private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
};

void AppendSubscriptionStorage(userver::components::ComponentList& component_list);

}  // namespace ya_brander::components