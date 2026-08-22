#pragma once

#include <string_view>

namespace ya_brander::components {

inline constexpr std::string_view kOpenAIModel = "gpt-5.6-luna";
inline constexpr std::string_view kOpenAIModelUri =
    "gpt://openai/gpt-5.6-luna";

inline bool IsOpenAIModel(std::string_view model) {
  return model == kOpenAIModel;
}

}  // namespace ya_brander::components
