#pragma once

#include <string>

namespace ya_brander::components {

struct LlmPrompt final {
  std::string instructions;
  std::string input;
};

}  // namespace ya_brander::components
