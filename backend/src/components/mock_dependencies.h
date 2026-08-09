#pragma once

#include "dependencies_stubs.h"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::components {


class MockVersionStorageComponent final : public VersionStorageComponent {
public:
    MockVersionStorageComponent(const userver::components::ComponentConfig& config,
                                const userver::components::ComponentContext& context)
        : VersionStorageComponent(config, context) {}

    std::string SaveVersion(const std::string& project_id, const userver::formats::json::Value& data) override {
        return "s3://fake-bucket/projects/" + project_id + "/latest.json";
    }

    userver::formats::json::Value GetLatestVersionData(const std::string& project_id) override {
        return userver::formats::json::FromString(
            R"({"width": 1024, "height": 1024, "background": "#FFFFFF", "layers": []})"
        );
    }
};

class MockLLMClientComponent final : public LLMClientComponent {
public:
    MockLLMClientComponent(const userver::components::ComponentConfig& config,
                           const userver::components::ComponentContext& context)
        : LLMClientComponent(config, context) {}

    userver::formats::json::Value SendPrompt(const std::string& prompt) override {
        return userver::formats::json::FromString(
            R"({"width": 1024, "height": 1024, "background": "#FFFFFF", "layers": [{"type": "rect", "color": "#000000"}]})"
        );
    }
};


class MockPromptConstructorComponent final : public PromptConstructorComponent {
public:
    MockPromptConstructorComponent(const userver::components::ComponentConfig& config,
                                   const userver::components::ComponentContext& context)
        : PromptConstructorComponent(config, context) {}

    std::string BuildEditPrompt(const userver::formats::json::Value& current_state, const std::string& user_message) override {
        return "MOCK_PROMPT: " + user_message;
    }
};

} // namespace ya_brander::components