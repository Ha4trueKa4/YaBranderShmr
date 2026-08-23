#pragma once

#include <string>
#include <vector>
#include <userver/components/component_list.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/formats/json/value.hpp>

#include "../vector_search_component.h"
#include "../llm_prompt.hpp"

namespace ya_brander::components {

class PromptConstructorComponent : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "prompt-constructor";

  PromptConstructorComponent(const userver::components::ComponentConfig& config,
                             const userver::components::ComponentContext& context);

  /// @param history Предыдущие запросы пользователя по этому проекту
  ///        (без ответов модели), от старых к новым.
  /// @param reference_svgs SVG других проектов пользователя, выбранные им как
  ///        референсы (только для первичной генерации).
  LlmPrompt BuildGenerationPrompt(const std::string& user_message,
                                  const std::vector<std::string>& history = {},
                                  const std::vector<std::string>& reference_svgs = {}) const;

  LlmPrompt BuildEditPrompt(const std::string& current_state,
                            const std::string& user_message,
                            const std::vector<std::string>& history = {}) const;

  static userver::yaml_config::Schema GetStaticConfigSchema();

private:
  VectorSearchComponent& vector_search_;
};

void AppendPromptConstructor(userver::components::ComponentList& component_list);

}  // namespace ya_brander::components
