#pragma once

#include <string>
#include <userver/components/component_list.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/formats/json/value.hpp>

#include "vector_search_component.h"

namespace ya_brander::components {

class PromptConstructorComponent : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "prompt-constructor";

  PromptConstructorComponent(const userver::components::ComponentConfig& config,
                             const userver::components::ComponentContext& context);

  std::string BuildGenerationPrompt(const std::string& user_message) const;

  std::string BuildEditPrompt(const std::string& current_state,
                              const std::string& user_message) const;

  static userver::yaml_config::Schema GetStaticConfigSchema();

private:
  VectorSearchComponent& vector_search_;
};

void AppendPromptConstructor(userver::components::ComponentList& component_list);

} // namespace ya_brander::components