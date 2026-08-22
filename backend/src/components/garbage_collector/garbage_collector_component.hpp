#pragma once
#include <chrono>
#include <string>

#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/utils/periodic_task.hpp>
#include <userver/yaml_config/schema.hpp>

#include "../version_storage/version_storage_component.hpp"

namespace ya_brander::components {

    /// @brief Фоновый сборщик "сирот" в объектном хранилище.
    /// @details Периодически вызывает VersionStorageComponent::CollectGarbage.
    ///          ВАЖНО: по умолчанию ВЫКЛЮЧЕН (enabled=false). Включать только там,
    ///          где БД и S3 бакет, одна и та же среда (прод VPS). Локальный запуск с
    ///          дефолтным конфигом теперь не запускает подметание вообще.
    class GarbageCollectorComponent final
        : public userver::components::LoggableComponentBase {
    public:
        static constexpr std::string_view kName = "garbage-collector";

        GarbageCollectorComponent(
            const userver::components::ComponentConfig& config,
            const userver::components::ComponentContext& context);

        ~GarbageCollectorComponent() override;

        static userver::yaml_config::Schema GetStaticConfigSchema();

    private:
        void RunOnce();

        VersionStorageComponent& version_storage_;
        std::chrono::seconds grace_;
        double max_orphan_ratio_;
        userver::utils::PeriodicTask task_;
    };

}  // namespace ya_brander::components