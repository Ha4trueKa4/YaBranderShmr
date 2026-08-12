#pragma once
#include <string>
#include <userver/components/loggable_component_base.hpp>

namespace ya_brander::components {

    /// @brief Интерфейс объектного хранилища (blob storage).
    /// @details Хранит и отдает произвольные данные по ключу. Реализация может быть
    ///          локальной (файлы) или облачной (S3). Компонент не знает о версиях,
    ///          проектах или базе данных, только put/get байтов по ключу.
    class ObjectStorageComponent : public userver::components::LoggableComponentBase {
    public:
        static constexpr std::string_view kName = "object-storage";

        ObjectStorageComponent(const userver::components::ComponentConfig& config,
                               const userver::components::ComponentContext& context)
            : LoggableComponentBase(config, context) {}

        ~ObjectStorageComponent() override = default;

        /// @brief Сохраняет данные под указанным ключом.
        /// @param key Ключ объекта.
        /// @param data Данные для сохранения.
        virtual void Put(const std::string& key, const std::string& data) = 0;

        /// @brief Возвращает данные по ключу.
        /// @param key Ключ объекта.
        /// @return Данные или пустая строка, если объект не найден.
        virtual std::string Get(const std::string& key) = 0;

        /// @brief Возвращает ссылку на svg в s3 БД, для прямого доступа (в основном для чтения).
        /// Прямая ссылка если проект публичный или временная ссылка если проект приватный.
        /// @param key Ключ объекта.
        /// @param is_public тип проекта, публичный/приватный.
        virtual std::string GetUrl(const std::string& key, bool is_public) = 0;  // URL, for preview

    };

}  // namespace ya_brander::components