#include "version_storage_component_impl.hpp"

#include <filesystem>
#include <fstream>
#include <iterator>
#include <userver/storages/postgres/component.hpp>

namespace ya_brander::components {

namespace pg = userver::storages::postgres;

VersionStorageComponentImpl::VersionStorageComponentImpl(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : VersionStorageComponent(config, context),
      pg_cluster_(
          context.FindComponent<userver::components::Postgres>("postgres-db-1")
              .GetCluster()) {}

std::string VersionStorageComponentImpl::WriteFile(
    const std::string& project_id, int version_num,
    const std::string& data) const {
  std::filesystem::create_directories("mock_s3");
  auto key =
      "mock_s3/" + project_id + "_" + std::to_string(version_num) + ".svg";
  std::ofstream out{key};
  out << data;
  return key;
}

std::string VersionStorageComponentImpl::ReadFile(const std::string& key) const {
  std::ifstream f{key};
  if (!f) return "";
  return std::string{std::istreambuf_iterator<char>(f),
                     std::istreambuf_iterator<char>()};
}

std::string VersionStorageComponentImpl::SaveVersion(
    const std::string& project_id, const std::string& data) {
  // Следующий номер версии для проекта. Ограничение UNIQUE(project_id,
  // version_num) защищает от коллизий при одновременных записях.
  auto num_res = pg_cluster_->Execute(
      pg::ClusterHostType::kMaster,
      "SELECT COALESCE(MAX(version_num), 0) + 1 FROM project_versions "
      "WHERE project_id = $1",
      project_id);
  const auto version_num = num_res[0][0].As<int>();

  const auto key = WriteFile(project_id, version_num, data);

  auto res = pg_cluster_->Execute(
      pg::ClusterHostType::kMaster,
      "INSERT INTO project_versions (project_id, version_num, s3_file_key) "
      "VALUES ($1, $2, $3) RETURNING id",
      project_id, version_num, key);

  return res[0]["id"].As<std::string>();
}

std::string VersionStorageComponentImpl::GetVersion(
    const std::string& project_id, const std::string& version_id) {
  auto res = pg_cluster_->Execute(
      pg::ClusterHostType::kSlave,
      "SELECT s3_file_key FROM project_versions "
      "WHERE id = $1 AND project_id = $2",
      version_id, project_id);
  if (res.IsEmpty()) return "";
  return ReadFile(res[0]["s3_file_key"].As<std::string>());
}

std::string VersionStorageComponentImpl::GetLatestVersionData(
    const std::string& project_id) {
  auto res = pg_cluster_->Execute(
      pg::ClusterHostType::kSlave,
      "SELECT s3_file_key FROM project_versions "
      "WHERE project_id = $1 ORDER BY version_num DESC LIMIT 1",
      project_id);
  if (res.IsEmpty()) return "";
  return ReadFile(res[0]["s3_file_key"].As<std::string>());
}

std::vector<models::Version> VersionStorageComponentImpl::ListVersions(
    const std::string& project_id) {
    
    auto res = pg_cluster_->Execute(
      pg::ClusterHostType::kSlave,
      "SELECT id, project_id, version_num, created_at, s3_file_key "  // no ::text now
      "FROM project_versions WHERE project_id = $1 "
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

std::string VersionStorageComponentImpl::RestoreVersion(
    const std::string& project_id, const std::string& version_id) {
  return SaveVersion(project_id, GetVersion(project_id, version_id));
}

}  // namespace ya_brander::components