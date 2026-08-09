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
      LOG_ERROR() << "No SVG found in response";
      return "";
    }

    size_t end = text.find("</svg>", start);
    if (end == std::string::npos) {
      LOG_ERROR() << "Incomplete SVG found";
      return "";
    }
    return text.substr(start, end - start + 6); // +6 для "</svg>"
  }

  std::string LLMClientComponent::SendPrompt(const std::string& prompt,
    const size_t& max_output_tokens,
    const std::string& Model,
    const double& TEMPERATURE) const {
    if (prompt.empty()) {
      return {};
    }
      try {
        userver::formats::json::ValueBuilder request_body;
        request_body["model"] = "gpt://b1gttnvmtpd0tagdlb7o/" + Model;
        request_body["temperature"] = TEMPERATURE;
        request_body["max_output_tokens"] = max_output_tokens;
        request_body["input"] = prompt;

        auto json_value = request_body.ExtractValue();
        std::string json_data = userver::formats::json::ToString(json_value);


        LOG_INFO() << "Request URL: https://ai.api.cloud.yandex.net/v1/responses";
        LOG_INFO() << "Request Method: POST";
        LOG_INFO() << "Request Headers:";
        LOG_INFO() << "  Authorization: Api-Key " << (api_key_.empty() ? "EMPTY!" : api_key_.substr(0, 10) + "..." + api_key_.substr(api_key_.length() - 4));
        LOG_INFO() << "  Content-Type: application/json";
        LOG_INFO() << "Request Body (JSON): " << json_data;


        auto request = http_client_.CreateRequest();
        request.method(userver::clients::http::HttpMethod::kPost);
        request.url("https://ai.api.cloud.yandex.net/v1/responses");
        request.headers({
          {"Authorization", "Api-Key " + api_key_},
          {"Content-Type", "application/json"}
        });

        // Увеличиваем таймауты для SSL
        request.timeout(60000);  // 60 секунд
        request.retry(3);      // 3 попытки

        request.data(json_data);

        auto response = request.perform();
        std::string response_body = response->body();
        if (response->status_code() != 200) {
          try {
            auto error_json = nlohmann::json::parse(response_body);
            LOG_ERROR() << "Parsed error JSON: " << error_json.dump(2);

            if (error_json.contains("error")) {
              auto error = error_json["error"];
              std::string error_message = error.value("message", "Unknown error");
              std::string error_code = error.value("code", "Unknown code");
              LOG_ERROR() << "Yandex error code: " << error_code;
              LOG_ERROR() << "Yandex error message: " << error_message;
              return "Error: " + error_code + " - " + error_message;
            }
            if (error_json.contains("message")) {
              return "Error: " + error_json["message"].get<std::string>();
            }
          } catch (const std::exception& e) {
            LOG_ERROR() << "Failed to parse error response: " << e.what();
          }

          return "Error: Yandex Ai Studio responded with code: " +
                 std::to_string(response->status_code()) +
                 ", body: " + response_body;
        }
        auto response_json = nlohmann::json::parse(response->body());
        return ExtractSVG(response_json["output"][0]["content"][0]["text"].get<std::string>());
      } catch (const std::exception& e) {
        return e.what();
      }
  }

userver::yaml_config::Schema LLMClientComponent::GetStaticConfigSchema() {
    return userver::yaml_config::MergeSchemas<
        userver::components::LoggableComponentBase
    >(R"(
type: object
description: LLM client component for Yandex AI Studio
additionalProperties: false
properties:
    api-key:
        type: string
        description: Yandex AI API key for authentication
    )");
  }

void AppendLLMClient(userver::components::ComponentList& component_list) {
    component_list.Append<components::LLMClientComponent>();
  }

}  // namespace ya_brander::components