#pragma once

#include <string>
#include <vector>

#include "../llm_prompt.hpp"

namespace ya_brander::components::prompt_template {

/// @param history Предыдущие запросы пользователя по этому проекту (без
///        ответов модели), от старых к новым.
LlmPrompt BuildGenerationPrompt(
    const std::string& user_message,
    const std::vector<std::string>& examples,
    const std::vector<std::string>& history = {});

LlmPrompt BuildEditPrompt(
    const std::string& current_state,
    const std::string& user_message,
    const std::vector<std::string>& examples,
    const std::vector<std::string>& history = {});

}  // namespace ya_brander::components::prompt_template
