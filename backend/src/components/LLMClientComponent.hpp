#pragma once

#include <cstdlib>
#include <stdexcept>
#include <string>
#include <string_view>

#include <userver/clients/http/client.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/components/component_context.hpp>
#include <userver/components/component_list.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/yaml_config/schema.hpp>

#include "llm_prompt.hpp"

namespace ya_brander::components {

  class SVGValidationError : public std::runtime_error {
  public:
    explicit SVGValidationError(const std::string& message)
        : std::runtime_error("SVG validation failed: " + message) {}
  };
  class LLMClientComponent : public userver::components::LoggableComponentBase {
  public:
      static constexpr std::string_view kName = "llm-client";

  LLMClientComponent(const userver::components::ComponentConfig& config,
                     const userver::components::ComponentContext& context)
      : LoggableComponentBase(config, context),
        http_client_(context.FindComponent<userver::components::HttpClient>()
                         .GetHttpClient()),
        api_url_(config["api-url"].As<std::string>()) {
    const char* key = std::getenv("YANDEX_AI_API_KEY");
    if (key == nullptr || std::string_view(key).empty()) {
      throw std::runtime_error("YANDEX_AI_API_KEY is not set");
    }
    api_key_ = key;
  }

  static userver::yaml_config::Schema GetStaticConfigSchema();

  std::string ExtractSVG(const std::string& text) const;
  // Отправляет промпт в LLM и возвращает полный текст ответа.
  virtual std::string SendPrompt(const LlmPrompt& prompt,
                                 std::size_t max_output_tokens,
                                 const std::string& model,
                                 double temperature) const;
 private:
  userver::clients::http::Client& http_client_;
  std::string api_url_;
  std::string api_key_;  // Ключ должен быть указан в .env.
};

void AppendLLMClient(userver::components::ComponentList& component_list);

}  // namespace ya_brander::components
