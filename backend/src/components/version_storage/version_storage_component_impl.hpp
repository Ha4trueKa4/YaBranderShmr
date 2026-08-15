#pragma once
#include <string>
#include <vector>
#include <userver/components/component_context.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include "version_storage_component.hpp"
#include "../../models/version.hpp"
#include "../object_storage/object_storage_component.hpp"

namespace ya_brander::components {

    /// @brief Реализация хранилища версий на PostgreSQL и объектном хранилище (S3).
    /// @details Метаданные версий хранятся в таблице project_versions, сами данные
    ///          (SVG) — в объектном хранилище через инжектированный
    ///          ObjectStorageComponent. Столбец s3_file_key указывает на объект,
    ///          projects.last_version_key — на объект текущей версии. Инвариант:
    ///          публичным (public-read) может быть только объект текущей версии
    ///          публичного проекта; все остальные объекты — private.
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

        std::string GetVersionUrl(
            const std::string& project_id, const std::string& version_id) override;


        /// @brief Возвращает ссылку на объект последней версии для фронтенда.
        /// @details Публичный проект -> постоянная прямая ссылка; приватный ->
        ///          временная presigned-ссылка. Байты через б  экенд не тянет.
        std::string GetLatestVersionUrl(const std::string& project_id) override;

        /// @brief Возвращает мапу с ключами и привязанным к ним готовым
        /// ссылкам для фетча объектов из S3 вызывающим этот метод
        std::unordered_map<std::string, std::string> GetManyUrls(
            const std::vector<std::string>& keys, bool is_public) override;

        /// @brief Ставит нужный ACL объекту текущей версии проекта.
        /// @details Вызывается при смене видимости. Читает last_version_key и
        ///          делегирует storage_.SetAcl. Если версий нет — no-op.
        void SetLatestAcl(const std::string& project_id, bool make_public) override;

        /// @brief Удаляет объекты всех версий проекта из хранилища (best-effort).
        /// @details Вызывается при удалении проекта, ДО удаления его строк (пока
        ///          project_versions ещё содержит ключи). Сами строки удаляются
        ///          каскадом. Ошибки удаления объектов из S3 не бросаются наружу —
        ///          их подберёт фоновый orphan-GC.
        void DeleteAllVersions(const std::string& project_id) override;

    private:
        /// Ключ объекта в хранилище: projects/{project_id}/{version_id}.svg.
        /// version_id — UUID строки project_versions, гарантирует уникальность
        /// ключа и удобный префикс для GC / lifecycle-правил.
        std::string MakeKey(const std::string& project_id,
                            const std::string& version_id) const;

        userver::storages::postgres::ClusterPtr pg_cluster_;
        ObjectStorageComponent& storage_;   // injected — was ReadFile/WriteFile
    };

}  // namespace ya_brander::components