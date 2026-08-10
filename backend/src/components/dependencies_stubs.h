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


} // namespace ya_brander::components