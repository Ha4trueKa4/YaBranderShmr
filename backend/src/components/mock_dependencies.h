#pragma once

#include "dependencies_stubs.h"
#include "LLMClientComponent.hpp"
#include "vector_search_component.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::components {

class MockLLMClientComponent final : public LLMClientComponent {
public:
    MockLLMClientComponent(const userver::components::ComponentConfig& config,
                           const userver::components::ComponentContext& context)
        : LLMClientComponent(config, context) {}

  std::string SendPrompt(const std::string& prompt,
      const size_t& max_output_tokens,
      const std::string& Model,
      const double& TEMPERATURE) const override {
      return R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
  <g>
    <circle cx="250" cy="250" r="200" fill="yellow"/>
    <rect x="150" y="150" width="100" height="100" fill="red"/>
  </g>
</svg>)SVG";
    }
};


class MockVectorSearchComponent final : public VectorSearchComponent {
public:
  MockVectorSearchComponent(const userver::components::ComponentConfig& config,
                            const userver::components::ComponentContext& context)
      : VectorSearchComponent(config, context) {}

  std::vector<std::string> FindSimilar(const std::string& /*description*/, int /*limit*/) const override {
    return {};
  }
};

} // namespace ya_brander::components