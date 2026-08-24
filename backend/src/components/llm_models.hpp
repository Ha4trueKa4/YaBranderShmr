#pragma once

#include <array>
#include <string_view>

namespace ya_brander::components {

inline constexpr std::string_view kOpenAIModel = "gpt-5.6-luna";
inline constexpr std::string_view kOpenAIModelUri =
    "gpt://openai/gpt-5.6-luna";

inline constexpr std::array<std::string_view, 1> kRecraftModels = {
    "recraftv2_vector",
};

inline constexpr std::string_view kRecraftModelUriPrefix = "gpt://recraft/";

inline bool IsOpenAIModel(std::string_view model) {
  return model == kOpenAIModel;
}

inline bool IsRecraftModel(std::string_view model) {
  for (const auto supported_model : kRecraftModels) {
    if (model == supported_model) {
      return true;
    }
  }
  return false;
}

}  // namespace ya_brander::components
