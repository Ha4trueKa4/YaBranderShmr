#include "version_storage_component_impl.hpp"
#include "../object_storage/object_storage_component.hpp"
#include <filesystem>
#include <fstream>
#include <iterator>
#include <userver/storages/postgres/component.hpp>
#include <string_view>


namespace ya_brander::components {

namespace pg = userver::storages::postgres;

VersionStorageComponentImpl::VersionStorageComponentImpl(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : VersionStorageComponent(config, context),
      pg_cluster_(
          context.FindComponent<userver::components::Postgres>("postgres-db-1")
              .GetCluster()),
      storage_(context.FindComponent<ObjectStorageComponent>()) {}

    std::string VersionStorageComponentImpl::MakeKey(
        const std::string& project_id, int version_num) const {
      return project_id + "_" + std::to_string(version_num) + ".svg";  // no dir prefix
    }

    std::string VersionStorageComponentImpl::SaveVersion(
        const std::string& project_id, const std::string& data) {
      auto txn = pg_cluster_->Begin(pg::ClusterHostType::kMaster,
                                    pg::Transaction::RW);

      auto num_res = txn.Execute(
          "SELECT COALESCE(MAX(version_num), 0) + 1 FROM project_versions "
          "WHERE project_id = $1::uuid", project_id);
      const auto version_num = num_res[0][0].As<int>();

      const auto key = MakeKey(project_id, version_num);
      storage_.Put(key, data);                          // was WriteFile

      auto res = txn.Execute(
          "INSERT INTO project_versions (project_id, version_num, s3_file_key) "
          "VALUES ($1::uuid, $2, $3) RETURNING id::text",
          project_id, version_num, key);
      auto id = res[0]["id"].As<std::string>();

      txn.Execute(
          "UPDATE projects SET last_version_key = $2, updated_at = NOW() "
          "WHERE id = $1::uuid", project_id, key);

      txn.Commit();
      return id;
    }

    std::string VersionStorageComponentImpl::GetVersion(
        const std::string& project_id, const std::string& version_id) {
      auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kSlave,
          "SELECT s3_file_key FROM project_versions "
          "WHERE id = $1 AND project_id = $2::uuid", version_id, project_id);
      if (res.IsEmpty()) return "";
      return storage_.Get(res[0]["s3_file_key"].As<std::string>());
    }

    std::string VersionStorageComponentImpl::GetLatestVersionData(
        const std::string& project_id) {
      auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kSlave,
          "SELECT last_version_key FROM projects WHERE id = $1::uuid", project_id);
      if (res.IsEmpty() || res[0]["last_version_key"].IsNull()) return "";

      return storage_.Get(res[0]["last_version_key"].As<std::string>());
    }

    std::vector<models::Version> VersionStorageComponentImpl::ListVersions(
        const std::string& project_id) {

      const auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kSlave,
          "SELECT id, project_id, version_num, created_at, s3_file_key "  // no ::text now
          "FROM project_versions WHERE project_id = $1::uuid "
          "ORDER BY version_num DESC",
          project_id);

        std::vector<models::Version> versions;
        versions.reserve(res.Size());

        for (const auto& row : res) {
            versions.push_back(models::Version{
                row["id"].As<std::string>(),
                row["project_id"].As<std::string>(),
                row["version_num"].As<std::int32_t>(),
                row["created_at"].As<userver::storages::postgres::TimePointTz>(),
                row["s3_file_key"].As<std::string>(),
            });
        }

        return versions;
    }

    std::unordered_map<std::string, std::string> VersionStorageComponentImpl::GetVersionUrls(
        const std::vector<std::string>& /*keys*/, bool /*is_public*/) {
      // ЗАГЛУШКА, ТРЕБУЕТСЯ ФУНКЦИЯ
      return {};
    }

    std::string VersionStorageComponentImpl::RestoreVersion(const std::string& project_id,
                               const std::string& version_id) {
      const auto data = GetVersion(project_id, version_id);
    if (data.empty()) {
        throw std::runtime_error("cannot restore: version not found");
    }
    return SaveVersion(project_id, data);
}

}  // namespace ya_brander::components