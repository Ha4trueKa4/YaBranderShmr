#pragma once

#include <string>
#include <string_view>

#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/components/component_list.hpp>
#include <userver/server/handlers/http_handler_base.hpp>

#include "../../components/subscription_storage/subscription_storage_component.h"

namespace ya_brander::handlers {

// Хендлер для GET /users/{id}/subscribers
class GetUserSubscribersHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-users-subscribers-get";

  GetUserSubscribersHandler(const userver::components::ComponentConfig& config,
                            const userver::components::ComponentContext& context);

  std::string HandleRequestThrow(const userver::server::http::HttpRequest& request,
                                 userver::server::request::RequestContext&) const override;

private:
  const components::SubscriptionStorageComponent& subscription_storage_;
};

// Хендлер для GET /users/{id}/subscriptions
class GetUserSubscriptionsHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-users-subscriptions-get";

  GetUserSubscriptionsHandler(const userver::components::ComponentConfig& config,
                              const userver::components::ComponentContext& context);

  std::string HandleRequestThrow(const userver::server::http::HttpRequest& request,
                                 userver::server::request::RequestContext&) const override;

private:
  const components::SubscriptionStorageComponent& subscription_storage_;
};

}  // namespace ya_brander::handlers