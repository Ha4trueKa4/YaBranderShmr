#include "prompt_constructor_component.h"

#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/formats/json/serialize.hpp>
#include <userver/yaml_config/merge_schemas.hpp>

namespace ya_brander::components {

PromptConstructorComponent::PromptConstructorComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      vector_search_(context.FindComponent<VectorSearchComponent>()) {}

namespace {

void AppendHistory(std::string& prompt, const std::vector<std::string>& history) {
    if (history.empty()) return;
    prompt += "История предыдущих запросов пользователя по этому проекту (от старых к новым, "
              "используй как контекст, не как повторное задание):\n";
    for (const auto& past_message : history) {
        prompt += "- " + past_message + "\n";
    }
    prompt += "\n";
}

}  // namespace

std::string PromptConstructorComponent::BuildGenerationPrompt(
    const std::string& user_message, const std::vector<std::string>& history) const {
    std::vector<std::string> examples = vector_search_.FindSimilar(user_message, 10);

    // 2. Конструируем итоговый промпт
    std::string prompt =
        "Ты — AI-редактор SVG-графики. Твоя задача - сгенерировать изображение по запросу. Технические требования: * Верни только чистый SVG-код в блоке разметки xml. Не пиши никаких вступлений, пояснений и заключений. * Укажи атрибуты xmlns=http://w3.org, а также viewBox=0 0 500 500 (или другой нужный размер). * Используй современные, гармоничные цвета. * Код должен быть аккуратным, сгруппированным с помощью тегов g и готовым к сохранению в svg файл.";

    if (!examples.empty()) {
        prompt += "Ниже представлены примеры хороших работ для сохранения стилистики:\n";
        for (auto example : examples) {
            prompt += example + "\n\n";
        }
    }

    AppendHistory(prompt, history);

    prompt += "Запрос пользователя: " + user_message;

    return prompt;
}

std::string PromptConstructorComponent::BuildEditPrompt(
    const std::string& current_state,
    const std::string& user_message,
    const std::vector<std::string>& history) const {

    std::vector<std::string> examples = vector_search_.FindSimilar(user_message, 10);

    std::string current_state_str = current_state;

    std::string prompt =
        "Ты — AI-редактор SVG-графики. Тебе передано текущее состояние и запрос на его изменение.\n"
        "Твоя задача — применить изменения и вернуть готовый SVG-код (внутри тегов <svg>...</svg>).\n"
        "Технические требования: * Верни только чистый SVG-код в блоке разметки xml. Не пиши никаких вступлений, пояснений и заключений. * Укажи атрибуты xmlns=http://w3.org, а также viewBox=0 0 500 500 (или другой нужный размер). * Используй современные, гармоничные цвета. * Код должен быть аккуратным, сгруппированным с помощью тегов g и готовым к сохранению в svg файл.\n\n";

    if (!examples.empty()) {
        prompt += "Примеры похожих работ:\n";
        for (auto example : examples) {
          prompt += example + "\n\n";
        }
    }

    AppendHistory(prompt, history);

    prompt += "Текущее состояние проекта:\n" + current_state_str + "\n\n";
    prompt += "Что нужно изменить (запрос пользователя): " + user_message;

    return prompt;
}

userver::yaml_config::Schema PromptConstructorComponent::GetStaticConfigSchema() {
    return userver::yaml_config::MergeSchemas<userver::components::LoggableComponentBase>(R"(
type: object
description: Prompt Constructor Component for building LLM queries (including RAG)
additionalProperties: false
properties: {}
    )");
}

void AppendPromptConstructor(userver::components::ComponentList& component_list) {
    component_list.Append<PromptConstructorComponent>();
}

} // namespace ya_brander::components