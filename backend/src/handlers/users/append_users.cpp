#include "append_users.hpp"

#include "subscription_handlers.hpp"

namespace ya_brander {

void AppendUsers(userver::components::ComponentList& component_list) {
  component_list.Append<handlers::SubscribeHandler>().Append<handlers::UnsubscribeHandler>();
}

}  // namespace ya_brander
