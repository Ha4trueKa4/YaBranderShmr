#pragma once

#include <string>
#include <string_view>

#include <userver/components/component_list.hpp>
#include <string>
#include <userver/components/loggable_component_base.hpp>
#include <userver/formats/json/value.hpp>
#include <userver/clients/http/client.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/server/handlers/http_handler_base.hpp>
#include <userver/components/component_context.hpp>

namespace ya_brander::components {

    class LLMClientComponent : public userver::components::LoggableComponentBase {
    public:
        static constexpr std::string_view kName = "llm-client";

        LLMClientComponent(const userver::components::ComponentConfig& config,
                           const userver::components::ComponentContext& context)
      : LoggableComponentBase(config, context),
      http_client_(context.FindComponent<userver::components::HttpClient>().GetHttpClient()),
      api_key_(std::getenv("YANDEX_AI_API_KEY")) {}

      static userver::yaml_config::Schema GetStaticConfigSchema();

        // Отправляет промпт в LLM и парсит ответ
      std::string SendPrompt(const std::string& prompt,
        const size_t& max_output_tokens = 1000,
        const std::string& Model = "yandexgpt-lite/latest",
        const double& TEMPERATURE = 0) const;
    private:
      userver::clients::http::Client& http_client_;
      std::string api_key_;  //Ключ должен был быть указан в .env
      std::string ExtractSVG(const std::string& text) const;
    };

void AppendLLMClient(userver::components::ComponentList& component_list);

}  // namespace ya_brander::components
