#pragma once

#include <string>
#include <userver/components/loggable_component_base.hpp>
#include <userver/formats/json/value.hpp>

namespace ya_brander::components {

// ==========================================================
// Компонент для работы с объектным хранилищем (S3) и версиями. ЗАГЛУШКА ДЛЯ project_manager_component
// ==========================================================
class VersionStorageComponent : public userver::components::LoggableComponentBase {
public:
    static constexpr std::string_view kName = "version-storage";

    VersionStorageComponent(const userver::components::ComponentConfig& config,
                            const userver::components::ComponentContext& context)
        : LoggableComponentBase(config, context) {}

    // Сохраняет векторный граф в S3, записывает строку в project_versions, возвращает ключ S3
    virtual std::string SaveVersion(const std::string& project_id, const std::string& data) = 0;
    
    // Загружает данные актуальной (последней) версии из S3
    virtual std::string GetLatestVersionData(const std::string& project_id) = 0;
};

// ==========================================================
// Компонент для формирования промптов (включая RAG) ЗАГЛУШКА ДЛЯ project_manager_component
// ==========================================================
class PromptConstructorComponent : public userver::components::LoggableComponentBase {
public:
    static constexpr std::string_view kName = "prompt-constructor";

    PromptConstructorComponent(const userver::components::ComponentConfig& config,
                               const userver::components::ComponentContext& context)
        : LoggableComponentBase(config, context) {}

    // Формирует итоговый промпт на основе текущего графа проекта и нового запроса пользователя
    virtual std::string BuildEditPrompt(const std::string& current_state, const std::string& user_message) = 0;
};

} // namespace ya_brander::components