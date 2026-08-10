#pragma once
#include <string>
#include <vector>
#include <userver/components/component_context.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include "version_storage_component.hpp"
#include "../../models/version.hpp"

namespace ya_brander::components {

    /// @brief Реализация хранилища версий на PostgreSQL и файловом хранилище.
    /// @details Метаданные версий хранятся в таблице project_versions, сами данные
    ///          (SVG) в файлах каталога mock_s3/. Столбец s3_file_key указывает на
    ///          файл. Позже файловое хранилище будет заменено на объектное (S3) без
    ///          изменения структуры таблицы.
    class VersionStorageComponentImpl final : public VersionStorageComponent {
    public:
        static constexpr std::string_view kName = "version-storage";

        VersionStorageComponentImpl(
            const userver::components::ComponentConfig& config,
            const userver::components::ComponentContext& context);

        std::string SaveVersion(const std::string& project_id,
                                const std::string& data) override;
        std::string GetVersion(const std::string& project_id,
                               const std::string& version_id) override;
        std::vector<models::Version> ListVersions(
            const std::string& project_id) override;
        std::string RestoreVersion(const std::string& project_id,
                                   const std::string& version_id) override;
        std::string GetLatestVersionData(const std::string& project_id) override;

    private:
        static constexpr std::string_view kStorageDir = "mock_s3";
        std::string ReadFile(const std::string& key) const;
        std::string WriteFile(const std::string& project_id, int version_num,
                              const std::string& data) const;

        userver::storages::postgres::ClusterPtr pg_cluster_;
    };

}  // namespace ya_brander::components