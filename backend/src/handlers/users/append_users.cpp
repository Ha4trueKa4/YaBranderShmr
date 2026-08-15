#include "append_users.hpp"

#include "subscription_handlers.hpp"
#include "get_user_profile.h"
#include "user_subscriptions_handlers.h"

namespace ya_brander {

void AppendUsers(userver::components::ComponentList& component_list) {
  component_list.Append<handlers::SubscribeHandler>().Append<handlers::UnsubscribeHandler>();
  component_list.Append<handlers::GetUserProfileHandler>();
  component_list.Append<handlers::GetUserSubscribersHandler>();
  component_list.Append<handlers::GetUserSubscriptionsHandler>();
}

}  // namespace ya_brander
