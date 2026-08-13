#pragma once
#include <userver/components/component_list.hpp>

namespace ya_brander {

// Функция для регистрации всех хендлеров, связанных с пользователями
void AppendUsers(userver::components::ComponentList& component_list);

}  // namespace ya_brander
