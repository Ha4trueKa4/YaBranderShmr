#pragma once

#include "dependencies_stubs.h"
#include "LLMClientComponent.hpp"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::components {


class MockVersionStorageComponent final : public VersionStorageComponent {
public:
    MockVersionStorageComponent(const userver::components::ComponentConfig& config,
                                const userver::components::ComponentContext& context)
        : VersionStorageComponent(config, context) {}

    std::string SaveVersion(const std::string& project_id, const std::string& data) override {
        return "s3://fake-bucket/projects/" + project_id + "/latest.json";
    }

    std::string GetLatestVersionData(const std::string& project_id) override {
        return R"SVG(<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
</svg>)SVG";
    }
};

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


class MockPromptConstructorComponent final : public PromptConstructorComponent {
public:
    MockPromptConstructorComponent(const userver::components::ComponentConfig& config,
                                   const userver::components::ComponentContext& context)
        : PromptConstructorComponent(config, context) {}

    std::string BuildEditPrompt(const std::string& current_state, const std::string& user_message) override {
        return "MOCK_PROMPT: " + user_message;
    }
};

} // namespace ya_brander::components