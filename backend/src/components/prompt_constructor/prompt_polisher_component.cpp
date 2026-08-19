#include "prompt_polisher_component.h"

#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/yaml_config/merge_schemas.hpp>
#include <userver/formats/json/serialize.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/logging/log.hpp>

#include <nlohmann/json.hpp>
#include <fmt/format.h>
#include <cstdlib>

namespace ya_brander::components {

namespace {
constexpr std::string_view kPolisherGenerateInstructions =
  "Ты — опытный арт-директор и prompt-инженер векторной графики. Твоя задача — преобразовать короткий или неточный запрос пользователя в детальное, структурированное техническое задание (ТЗ) для другой ИИ-модели, которая будет писать чистый SVG-код."
  "Твои задачи при обработке запроса:"
  "1. Раскрыть визуальный стиль: Если пользователь не указал стиль, подбери подходящий для SVG (например: Flat design, Material, Line art, Corporate minimalism, Geometric). Исключи растровые понятия (фотореализм, сложное освещение, размытие)."
  "2. Задать цветовую палитру: Опиши 3-4 гармоничных цвета (можно с HEX-кодами), подходящих к тематике."
  "3. Описать композицию по слоям: Разбей идею на логические группы, чтобы помочь SVG-генератору. Явно выдели:"
  "   - Фон (background)"
  "   - Основной объект (illustration / logo-mark)"
  "   - Текст (если запрашивается)"
  "   - Декор (дополнительные элементы)"
  "4. Векторная специфика: Формулируй ТЗ в терминах простых геометрических примитивов, четких контуров (stroke), сплошных заливок (fill) и базовых градиентов."
  "ПРАВИЛА ОТВЕТА:"
  "- НИКОГДА не генерируй SVG-код самостоятельно. Твой ответ — это только текст ТЗ."
  "- Выведи ТОЛЬКО улучшенный промпт. Никаких приветствий, вступлений («Вот ваш промпт») или заключений."
  "- Пиши четко, емко и директивно, используя списки."
  "Формат вывода:"
  "[Краткое описание концепции]"
  "Стиль: [Название стиля, настроение]"
  "Цвета: [Описания цветов]"
  "Структура:"
  "- Фон: [Описание]"
  "- Главный объект: [Детальное описание геометрии]"
  "- Декор/Текст: [Описание]";


constexpr std::string_view kPolisherEditInstructions =

}

PromptPolisherComponent::PromptPolisherComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      http_client_(context.FindComponent<userver::components::HttpClient>().GetHttpClient()),
      api_url_(config["api-url"].As<std::string>()) {

    const char* key = std::getenv("YANDEX_AI_API_KEY");
    if (key == nullptr || std::string_view(key).empty()) {
        throw std::runtime_error("YANDEX_AI_API_KEY is not set for PromptPolisherComponent");
    }
    api_key_ = key;
}

std::string PromptPolisherComponent::PolishGenerate(const std::string& raw_prompt) const {
    userver::formats::json::ValueBuilder request_body;
    request_body["model"] = "gpt://b1gttnvmtpd0tagdlb7o/yandexgpt-lite/latest";
    request_body["temperature"] = 0.3;
    request_body["max_output_tokens"] = 1000;
    request_body["instructions"] = kPolisherGenerateInstructions;
    request_body["input"] = fmt::format("Исходный запрос: {}", raw_prompt);

    std::string json_data = userver::formats::json::ToString(request_body.ExtractValue());

    auto request = http_client_.CreateRequest();
    request.method(userver::clients::http::HttpMethod::kPost);
    request.url(api_url_);
    request.headers({{"Authorization", "Api-Key " + api_key_},
                     {"Content-Type", "application/json"}});
    request.timeout(15000);

    request.data(json_data);

    try {
        auto response = request.perform();
        if (response->status_code() != 200) {
            LOG_WARNING() << "Polisher API returned non-200 code: " << response->status_code()
                          << ". Falling back to raw prompt.";
            return raw_prompt;
        }

        auto response_json = nlohmann::json::parse(response->body());

        // Извлекаем обычный текст без парсинга SVG
        for (const auto& output_item : response_json["output"]) {
            if (output_item.contains("content") && output_item["content"].is_array() && !output_item["content"].empty()) {
                for (const auto& content_item : output_item["content"]) {
                    if (content_item.contains("text") && content_item["text"].is_string()) {
                        std::string polished = content_item["text"].get<std::string>();
                        LOG_DEBUG() << "Prompt polished successfully: " << polished;
                        return polished;
                    }
                }
            }
        }
    } catch (const std::exception& e) {
        LOG_WARNING() << "Polisher failed (" << e.what() << "). Falling back to raw prompt.";
    }

    return raw_prompt;
}

std::string PromptPolisherComponent::PolishEdit(const std::string& raw_prompt) const {
    userver::formats::json::ValueBuilder request_body;
    request_body["model"] = "gpt://b1gttnvmtpd0tagdlb7o/yandexgpt-lite/latest";
    request_body["temperature"] = 0.3;
    request_body["max_output_tokens"] = 1000;
    request_body["instructions"] = kPolisherEditInstructions;
    request_body["input"] = fmt::format("Исходный запрос: {}", raw_prompt);

    std::string json_data = userver::formats::json::ToString(request_body.ExtractValue());

    auto request = http_client_.CreateRequest();
    request.method(userver::clients::http::HttpMethod::kPost);
    request.url(api_url_);
    request.headers({{"Authorization", "Api-Key " + api_key_},
                     {"Content-Type", "application/json"}});
    request.timeout(15000);

    request.data(json_data);

    try {
        auto response = request.perform();
        if (response->status_code() != 200) {
            LOG_WARNING() << "Polisher API returned non-200 code: " << response->status_code()
                          << ". Falling back to raw prompt.";
            return raw_prompt;
        }

        auto response_json = nlohmann::json::parse(response->body());

        // Извлекаем обычный текст без парсинга SVG
        for (const auto& output_item : response_json["output"]) {
            if (output_item.contains("content") && output_item["content"].is_array() && !output_item["content"].empty()) {
                for (const auto& content_item : output_item["content"]) {
                    if (content_item.contains("text") && content_item["text"].is_string()) {
                        std::string polished = content_item["text"].get<std::string>();
                        LOG_DEBUG() << "Prompt polished successfully: " << polished;
                        return polished;
                    }
                }
            }
        }
    } catch (const std::exception& e) {
        LOG_WARNING() << "Polisher failed (" << e.what() << "). Falling back to raw prompt.";
    }

    return raw_prompt;
}

userver::yaml_config::Schema PromptPolisherComponent::GetStaticConfigSchema() {
    return userver::yaml_config::MergeSchemas<userver::components::LoggableComponentBase>(R"(
type: object
description: Prompt Polisher Component
additionalProperties: false
properties:
    api-url:
        type: string
        description: URL for Yandex GPT API
)");
}

void AppendPromptPolisher(userver::components::ComponentList& component_list) {
    component_list.Append<PromptPolisherComponent>();
}

} // namespace ya_brander::components