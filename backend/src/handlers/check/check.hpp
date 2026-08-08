#pragma once

#include <string>
#include <string_view>

#include <userver/components/component_list.hpp>

namespace ya_brander {

enum class UserType { kFirstTime, kKnown };
std::string SayHelloTo(std::string_view name, UserType type);

void AppendCheck(userver::components::ComponentList& component_list);

}  // namespace ya_brander
