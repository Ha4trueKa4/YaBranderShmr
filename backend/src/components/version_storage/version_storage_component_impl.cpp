#include "version_storage_component_impl.hpp"
#include "../svg_validator.hpp"
#include <unordered_set>
#include <cstdint>
#include <stdexcept>
#include <vector>
#include <userver/storages/postgres/component.hpp>
#include <userver/logging/log.hpp>

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
        const std::string& project_id, const std::string& version_id) const {
      return "projects/" + project_id + "/" + version_id + ".svg";
    }

std::string VersionStorageComponentImpl::SaveVersion(
const std::string& project_id, const std::string& data) {

      std::string version_id;
      std::string new_key;
      std::string old_key;
      bool is_public = false;
      {
        auto txn =
            pg_cluster_->Begin(pg::ClusterHostType::kMaster, pg::Transaction::RW);

        auto proj = txn.Execute(
            "SELECT is_private, last_version_key FROM projects "
            "WHERE id = $1::uuid FOR UPDATE",
            project_id);
        if (proj.IsEmpty()) {
          throw std::runtime_error("SaveVersion: project not found");
        }
        is_public = !proj[0]["is_private"].As<bool>();
        if (!proj[0]["last_version_key"].IsNull()) {
          old_key = proj[0]["last_version_key"].As<std::string>();
        }

        // Резервируем строку версии с NULL, без s3 ключа. Если вдруг PUT в S3
        // зафейлится, то строка останется как NULL и вернет 404 если будет как то
        // запрошена. ListVersions теперь исключает строки с NULL.
        auto ins = txn.Execute(
            "INSERT INTO project_versions (project_id, version_num) VALUES "
            "($1::uuid, (SELECT COALESCE(MAX(version_num), 0) + 1 "
            "            FROM project_versions WHERE project_id = $1::uuid)) "
            "RETURNING id::text",
            project_id);
        version_id = ins[0]["id"].As<std::string>();
        new_key = MakeKey(project_id, version_id);

        txn.Commit();
      }

      // Если здесь возникает ошибка, она пробрасывается наверх,
      // а строка s3_file_key просто остается пустой и фильтруется при GET запросах.
      storage_.Put(new_key, data, /*make_public=*/is_public);

      // Обновляем ключ версии на реальный (был NULL).
      pg_cluster_->Execute(
          pg::ClusterHostType::kMaster,
          "UPDATE project_versions SET s3_file_key = $2 WHERE id = $1::uuid",
          version_id, new_key);

      // Обновляем последний ключ на реальный.
      pg_cluster_->Execute(
          pg::ClusterHostType::kMaster,
          "UPDATE projects SET last_version_key = $2, updated_at = NOW() "
          "WHERE id = $1::uuid",
          project_id, new_key);

      // Best-effort demote of the previous latest (never fail the whole save on it).
      if (is_public && !old_key.empty()) {
        try {
          storage_.SetAcl(old_key, /*make_public=*/false);
        } catch (const std::exception& e) {
          LOG_ERROR() << "SaveVersion: failed to demote old version " << old_key
                      << " to private: " << e.what();
        }
      }

      return version_id;
    }

    std::string VersionStorageComponentImpl::GetVersion(
        const std::string& project_id, const std::string& version_id) {
      auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kSlave,
          "SELECT s3_file_key FROM project_versions "
          "WHERE id = $1::uuid AND project_id = $2::uuid",
          version_id, project_id);
      if (res.IsEmpty() || res[0]["s3_file_key"].IsNull()) return "";
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

    std::string VersionStorageComponentImpl::GetLatestVersionUrl(
        const std::string& project_id) {
      auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kSlave,
          "SELECT last_version_key, is_private FROM projects WHERE id = $1::uuid",
          project_id);
      if (res.IsEmpty() || res[0]["last_version_key"].IsNull()) return "";
      const bool is_public = !res[0]["is_private"].As<bool>();
      return storage_.GetUrl(res[0]["last_version_key"].As<std::string>(), is_public);
    }

    std::string VersionStorageComponentImpl::GetVersionUrl(
        const std::string& project_id, const std::string& version_id) {
      auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kSlave,
          "SELECT s3_file_key FROM project_versions "
          "WHERE id = $1::uuid AND project_id = $2::uuid",
          version_id, project_id);
      if (res.IsEmpty() || res[0]["s3_file_key"].IsNull()) return "";
      return storage_.GetUrl(res[0]["s3_file_key"].As<std::string>(),
                             /*is_public=*/false);
    }

        std::unordered_map<std::string, std::string>
    VersionStorageComponentImpl::GetManyUrls(
        const std::vector<std::string>& keys, bool is_public) {

        std::unordered_map<std::string, std::string> out;
        out.reserve(keys.size());
        for (const auto& key : keys) {
            out.emplace(key, storage_.GetUrl(key, is_public));
        }
        return out;
    }

    std::vector<models::Version> VersionStorageComponentImpl::ListVersions(
    const std::string& project_id) {
    const auto res = pg_cluster_->Execute(
        pg::ClusterHostType::kSlave,
        "SELECT id, project_id, version_num, created_at "
        "FROM project_versions "
        "WHERE project_id = $1::uuid AND s3_file_key IS NOT NULL "
        "ORDER BY version_num DESC",
        project_id);

    std::vector<models::Version> versions;
    versions.reserve(res.Size());
    for (const auto& row : res) {
        versions.push_back(models::Version{
            row["id"].As<std::string>(),
            row["project_id"].As<std::string>(),
            row["version_num"].As<std::int32_t>(),
            row["created_at"].As<pg::TimePointTz>(),
        });
    }
    return versions;
}

    std::string VersionStorageComponentImpl::RestoreVersion(
        const std::string& project_id, const std::string& version_id) {
      const auto data = GetVersion(project_id, version_id);
      if (data.empty()) {
        throw std::runtime_error("cannot restore: version not found");
      }

      return SaveVersion(project_id, data);
    }

    void VersionStorageComponentImpl::SetLatestAcl(const std::string& project_id,
                                                   bool make_public) {
    auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kMaster,
          "SELECT last_version_key FROM projects WHERE id = $1::uuid", project_id);
      if (res.IsEmpty() || res[0]["last_version_key"].IsNull()) return;
      storage_.SetAcl(res[0]["last_version_key"].As<std::string>(), make_public);
    }

    void VersionStorageComponentImpl::DeleteAllVersions(const std::string& project_id) {
      auto res = pg_cluster_->Execute(
          pg::ClusterHostType::kMaster,
          "SELECT s3_file_key FROM project_versions "
          "WHERE project_id = $1::uuid AND s3_file_key IS NOT NULL",
          project_id);

      std::vector<std::string> keys;
      keys.reserve(res.Size());
      for (const auto& row : res) keys.push_back(row["s3_file_key"].As<std::string>());
      if (keys.empty()) return;

      try {
        storage_.DeleteMany(keys);
      } catch (const std::exception& e) {
        LOG_ERROR() << "DeleteAllVersions: some objects left for GC, project="
                    << project_id << ": " << e.what();
      }
    }

    std::size_t VersionStorageComponentImpl::CollectGarbage(
    std::chrono::seconds grace, double max_orphan_ratio) {

        const auto objects = storage_.ListKeys("projects/");
        if (objects.empty()) return 0;

        std::unordered_set<std::string> referenced;
        {
        const auto res = pg_cluster_->Execute(
            pg::ClusterHostType::kMaster,
            "SELECT s3_file_key AS k FROM project_versions WHERE s3_file_key IS NOT NULL "
            "UNION "
            "SELECT last_version_key AS k FROM projects WHERE last_version_key IS NOT NULL");
        referenced.reserve(res.Size());
        for (const auto& row : res) referenced.insert(row["k"].As<std::string>());
        }

        if (referenced.empty()) {
            LOG_ERROR() << "CollectGarbage ABORTED: reference set is empty but bucket has "
                        << objects.size()
                        << " objects. Likely wrong DB/bucket pairing (different "
                           "environments). Deleting nothing.";
            return 0;
        }

        const auto cutoff = std::chrono::system_clock::now() - grace;
        std::vector<std::string> orphans;
        for (const auto& obj : objects) {
            if (referenced.count(obj.key) == 0 && obj.last_modified < cutoff) {
                orphans.push_back(obj.key);
            }
        }
        if (orphans.empty()) return 0;

        const double ratio =
          static_cast<double>(orphans.size()) / static_cast<double>(objects.size());
        if (ratio > max_orphan_ratio) {
        LOG_ERROR() << "CollectGarbage ABORTED: " << orphans.size() << " of "
                    << objects.size() << " objects ("
                    << static_cast<int>(ratio * 100)
                    << "%) look like orphans, over the "
                    << static_cast<int>(max_orphan_ratio * 100)
                    << "% safety threshold. Refusing to delete — likely misconfiguration.";
        return 0;
        }

        try {
            storage_.DeleteMany(orphans);
        } catch (const std::exception& e) {
                LOG_ERROR() << "CollectGarbage: delete failed, will retry next run: " << e.what();
            }
            LOG_INFO() << "CollectGarbage: reclaimed " << orphans.size() << " orphan objects";

        return orphans.size();
    }
}  // namespace ya_brander::components
