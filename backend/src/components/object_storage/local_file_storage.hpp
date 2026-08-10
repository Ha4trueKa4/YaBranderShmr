#pragma once
#include <string>
#include <string_view>
#include "object_storage_component.hpp"


namespace ya_brander::components {

    /// @brief Локальная файловая реализация объектного хранилища.
    /// @details Хранит объекты как файлы в каталоге mock_s3/. Ключ трактуется как
    ///          имя файла. Временная реализация, позже заменяется на S3.
    class LocalFileStorage final : public ObjectStorageComponent {
    public:
        static constexpr std::string_view kName = "object-storage";

        LocalFileStorage(const userver::components::ComponentConfig& config,
                         const userver::components::ComponentContext& context);

        void Put(const std::string& key, const std::string& data) override;

        std::string Get(const std::string& key) override;

        std::string GetUrl(const std::string& key, bool is_public) override;

    private:
        static constexpr std::string_view kStorageDir = "mock_s3";
        static std::string FullPath(const std::string& key);
        userver::engine::TaskProcessor& fs_task_processor_;

    };

}  // namespace ya_brander::components