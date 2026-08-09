#pragma once
#include <userver/components/component_list.hpp>

namespace ya_brander {

// Функция для регистрации всех компонентов, связанных с проектами
void AppendProjects(userver::components::ComponentList& component_list);

} // namespace ya_brander