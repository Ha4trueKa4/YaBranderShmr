#include "prompt_constructor_component.h"

#include "prompt_template.hpp"

#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/yaml_config/merge_schemas.hpp>

namespace ya_brander::components {

PromptConstructorComponent::PromptConstructorComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      vector_search_(context.FindComponent<VectorSearchComponent>()) {}

LlmPrompt PromptConstructorComponent::BuildGenerationPrompt(
    const std::string& user_message, const std::vector<std::string>& history) const {
  const auto examples = vector_search_.FindSimilar(user_message, 2);
  return prompt_template::BuildGenerationPrompt(user_message, examples, history);
}

LlmPrompt PromptConstructorComponent::BuildEditPrompt(
    const std::string& current_state,
    const std::string& user_message,
    const std::vector<std::string>& history) const {
  const auto examples = vector_search_.FindSimilar(user_message, 2);
  return prompt_template::BuildEditPrompt(current_state, user_message, examples, history);
}

userver::yaml_config::Schema PromptConstructorComponent::GetStaticConfigSchema() {
  return userver::yaml_config::MergeSchemas<
      userver::components::LoggableComponentBase>(R"(
type: object
description: Prompt Constructor Component for building LLM queries (including RAG)
additionalProperties: false
properties: {}
  )");
}

void AppendPromptConstructor(userver::components::ComponentList& component_list) {
  component_list.Append<PromptConstructorComponent>();
}

}  // namespace ya_brander::components
