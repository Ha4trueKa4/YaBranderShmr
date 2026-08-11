#include "LLMClientComponent.hpp"

#include <nlohmann/json.hpp>
#include <userver/clients/http/client.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/components/component_context.hpp>
#include <userver/server/handlers/http_handler_base.hpp>
#include <userver/yaml_config/schema.hpp>
#include <userver/yaml_config/merge_schemas.hpp>
namespace ya_brander::components {

  std::string LLMClientComponent::ExtractSVG(const std::string& text) const {
    size_t start = text.find("<svg");
    if (start == std::string::npos) {
      throw std::runtime_error("No SVG content found in LLM response");
    }

    size_t end = text.find("</svg>", start);
    if (end == std::string::npos) {
      throw std::runtime_error("Wrong SVG content in LLM response");
    }
    return text.substr(start, end - start + 6); // +6 для "</svg>"
  }

  std::string LLMClientComponent::SendPrompt(const std::string& prompt,
    const size_t& max_output_tokens,
    const std::string& Model,
    const double& TEMPERATURE) const {
    if (prompt.empty()) {
      throw std::invalid_argument("Empty prompt provided");
    }
        userver::formats::json::ValueBuilder request_body;
        request_body["model"] = "gpt://b1gttnvmtpd0tagdlb7o/" + Model;
        request_body["temperature"] = TEMPERATURE;
        request_body["max_output_tokens"] = max_output_tokens;
        request_body["input"] = prompt;

        auto json_value = request_body.ExtractValue();
        std::string json_data = userver::formats::json::ToString(json_value);

        auto request = http_client_.CreateRequest();
        request.method(userver::clients::http::HttpMethod::kPost);
        request.url(api_url_);
        request.headers({
          {"Authorization", "Api-Key " + api_key_},
          {"Content-Type", "application/json"}
        });

        // Увеличиваем таймауты для SSL
        request.timeout(60000);  // 60 секунд

        request.data(json_data);

        auto response = request.perform();
        std::string response_body = response->body();
        if (response->status_code() != 200) {
          std::string error_msg;
          try {
            auto error_json = nlohmann::json::parse(response_body);
            if (error_json.contains("error")) {
              auto error = error_json["error"];
              error_msg = "API Error: " + error.value("code", "Unknown") +
                         " - " + error.value("message", "Unknown error");
            } else if (error_json.contains("message")) {
              error_msg = "API Error: " + error_json["message"].get<std::string>();
            } else {
              error_msg = "HTTP Error " + std::to_string(response->status_code());
            }
          } catch (const std::exception& e) {
            error_msg = "HTTP Error " + std::to_string(response->status_code()) +
                       ", body: " + response_body.substr(0, 200);
          }
          throw std::runtime_error(error_msg);
        }
        auto response_json = nlohmann::json::parse(response->body());

        if (!response_json.contains("output") || !response_json["output"].is_array() || response_json["output"].empty()) {
          throw std::runtime_error("LLM response does not contain output");
        }

        const auto& first_output = response_json["output"][0];
        if (!first_output.contains("content") || !first_output["content"].is_array() || first_output["content"].empty()) {
          throw std::runtime_error("LLM response does not contain content");
        }

        const auto& first_content = first_output["content"][0];
        if (!first_content.contains("text") || !first_content["text"].is_string()) {
          throw std::runtime_error("LLM response does not contain text");
        }

        return ExtractSVG(first_content["text"].get<std::string>());
  }

userver::yaml_config::Schema LLMClientComponent::GetStaticConfigSchema() {
    return userver::yaml_config::MergeSchemas<
        userver::components::LoggableComponentBase
    >(R"(
type: object
description: LLM client component for Yandex AI Studio
additionalProperties: false
properties:
    api-url:
        type: string
        description: URL for Yandex GPT API
    api-key:
        type: string
        description: API Key
    )");
  }

void AppendLLMClient(userver::components::ComponentList& component_list) {
    component_list.Append<components::LLMClientComponent>();
  }

}  // namespace ya_brander::components