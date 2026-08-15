#pragma once

#include <string>
#include <vector>

#include "../llm_prompt.hpp"

namespace ya_brander::components::prompt_template {

LlmPrompt BuildGenerationPrompt(
    const std::string& user_message,
    const std::vector<std::string>& examples);

LlmPrompt BuildEditPrompt(
    const std::string& current_state,
    const std::string& user_message,
    const std::vector<std::string>& examples);

}  // namespace ya_brander::components::prompt_template
