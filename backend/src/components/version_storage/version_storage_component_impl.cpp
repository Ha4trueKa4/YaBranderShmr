#include "version_storage_component_impl.hpp"

#include <cstdint>
#include <stdexcept>
#include <unordered_set>
#include <vector>

#include <userver/logging/log.hpp>
#include <userver/storages/postgres/component.hpp>

#include <ya_brander/sql_queries.hpp>

#include "../../utils/postgres_retry.hpp"
#include "../svg_validator.hpp"

namespace ya_brander::components {

namespace pg = userver::storages::postgres;

namespace {
// Произвольная, но стабильная между деплоями метка advisory-lock'а GC.
constexpr std::int64_t kGcAdvisoryLockKey = 727390;
}  // namespace

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

    auto proj = txn.Execute(sql::kLockProjectVersionState, project_id);
    if (proj.IsEmpty()) {
      throw std::runtime_error("SaveVersion: project not found");
    }
    is_public = !proj[0]["is_private"].As<bool>();
    if (!proj[0]["last_version_key"].IsNull()) {
      old_key = proj[0]["last_version_key"].As<std::string>();
    }

    // Резервируем строку версии с NULL, без s3 ключа. Если PUT в S3
    // завершится ошибкой, строка останется с NULL и не попадёт в GET/list.
    auto ins = txn.Execute(sql::kCreateProjectVersion, project_id);
    version_id = ins[0]["id"].As<std::string>();
    new_key = MakeKey(project_id, version_id);

    txn.Commit();
  }

  // При ошибке строка s3_file_key остаётся пустой и фильтруется при чтении.
  storage_.Put(new_key, data, /*make_public=*/is_public);

  {
    auto txn =
        pg_cluster_->Begin(pg::ClusterHostType::kMaster, pg::Transaction::RW);

    // Обновляем ключ версии и last_version_key атомарно. Если транзакция
    // завершится ошибкой, загруженный объект позже удалит orphan-GC.
    txn.Execute(sql::kUpdateProjectVersionKey, version_id, new_key);
    txn.Execute(sql::kUpdateProjectLatestVersionKey, project_id, new_key);

    txn.Commit();
  }

  // Ошибка смены ACL старой версии не должна ломать успешное сохранение.
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
  auto res = utils::ExecuteSelectWithRetry("select-project-version-key", [&] {
    return pg_cluster_->Execute(pg::ClusterHostType::kSlave,
                                sql::kSelectProjectVersionKey, version_id,
                                project_id);
  });
  if (res.IsEmpty() || res[0]["s3_file_key"].IsNull()) return "";
  return storage_.Get(res[0]["s3_file_key"].As<std::string>());
}

std::string VersionStorageComponentImpl::GetLatestVersionData(
    const std::string& project_id) {
  auto res =
      utils::ExecuteSelectWithRetry("select-project-latest-version-key", [&] {
        return pg_cluster_->Execute(pg::ClusterHostType::kSlave,
                                    sql::kSelectProjectLatestVersionKey,
                                    project_id);
      });
  if (res.IsEmpty() || res[0]["last_version_key"].IsNull()) return "";
  return storage_.Get(res[0]["last_version_key"].As<std::string>());
}

std::string VersionStorageComponentImpl::GetLatestVersionUrl(
    const std::string& project_id) {
  auto res =
      utils::ExecuteSelectWithRetry("select-project-latest-version-state", [&] {
        return pg_cluster_->Execute(pg::ClusterHostType::kSlave,
                                    sql::kSelectProjectLatestVersionState,
                                    project_id);
      });
  if (res.IsEmpty() || res[0]["last_version_key"].IsNull()) return "";
  const bool is_public = !res[0]["is_private"].As<bool>();
  return storage_.GetUrl(res[0]["last_version_key"].As<std::string>(),
                         is_public);
}

std::string VersionStorageComponentImpl::GetVersionUrl(
    const std::string& project_id, const std::string& version_id) {
  auto res = utils::ExecuteSelectWithRetry("select-project-version-key", [&] {
    return pg_cluster_->Execute(pg::ClusterHostType::kSlave,
                                sql::kSelectProjectVersionKey, version_id,
                                project_id);
  });
  if (res.IsEmpty() || res[0]["s3_file_key"].IsNull()) return "";
  return storage_.GetUrl(res[0]["s3_file_key"].As<std::string>(),
                         /*is_public=*/false);
}

std::unordered_map<std::string, std::string>
VersionStorageComponentImpl::GetManyUrls(const std::vector<std::string>& keys,
                                         bool is_public) {
  std::unordered_map<std::string, std::string> out;
  out.reserve(keys.size());
  for (const auto& key : keys) {
    out.emplace(key, storage_.GetUrl(key, is_public));
  }
  return out;
}

std::vector<models::Version> VersionStorageComponentImpl::ListVersions(
    const std::string& project_id) {
  const auto res = utils::ExecuteSelectWithRetry("list-project-versions", [&] {
    return pg_cluster_->Execute(pg::ClusterHostType::kSlave,
                                sql::kListProjectVersions, project_id);
  });

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
  auto res =
      utils::ExecuteSelectWithRetry("select-project-latest-version-key", [&] {
        return pg_cluster_->Execute(pg::ClusterHostType::kMaster,
                                    sql::kSelectProjectLatestVersionKey,
                                    project_id);
      });
  if (res.IsEmpty() || res[0]["last_version_key"].IsNull()) return;
  storage_.SetAcl(res[0]["last_version_key"].As<std::string>(), make_public);
}

void VersionStorageComponentImpl::DeleteAllVersions(
    const std::string& project_id) {
  auto res =
      utils::ExecuteSelectWithRetry("select-project-version-keys", [&] {
        return pg_cluster_->Execute(pg::ClusterHostType::kMaster,
                                    sql::kSelectProjectVersionKeys, project_id);
      });

  std::vector<std::string> keys;
  keys.reserve(res.Size());
  for (const auto& row : res) {
    keys.push_back(row["s3_file_key"].As<std::string>());
  }
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
  // Транзакционный advisory-lock не даёт двум инстансам запускать GC вместе.
  auto lock_txn =
      pg_cluster_->Begin(pg::ClusterHostType::kMaster, pg::Transaction::RW);
  const auto lock_res = lock_txn.Execute(
      "SELECT pg_try_advisory_xact_lock($1::bigint)", kGcAdvisoryLockKey);
  if (!lock_res[0][0].As<bool>()) {
    LOG_WARNING()
        << "CollectGarbage: another instance is already running GC, skipping.";
    return 0;
  }

  const auto objects = storage_.ListKeys("projects/");
  if (objects.empty()) {
    LOG_INFO() << "CollectGarbage: no objects found under prefix 'projects/', "
                  "nothing to do.";
    return 0;
  }

  std::unordered_set<std::string> referenced;
  {
    const auto res = utils::ExecuteSelectWithRetry(
        "select-referenced-version-keys", [&] {
          return pg_cluster_->Execute(
              pg::ClusterHostType::kSlave,
              "SELECT s3_file_key AS k FROM project_versions "
              "WHERE s3_file_key IS NOT NULL "
              "UNION "
              "SELECT last_version_key AS k FROM projects WHERE "
              "last_version_key IS NOT NULL");
        });
    referenced.reserve(res.Size());
    for (const auto& row : res) {
      referenced.insert(row["k"].As<std::string>());
    }
  }

  if (referenced.empty()) {
    LOG_ERROR()
        << "CollectGarbage ABORTED: reference set is empty but bucket has "
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
    LOG_ERROR()
        << "CollectGarbage ABORTED: " << orphans.size() << " of "
        << objects.size() << " objects (" << static_cast<int>(ratio * 100)
        << "%) look like orphans, over the "
        << static_cast<int>(max_orphan_ratio * 100)
        << "% safety threshold. Refusing to delete — likely misconfiguration.";
    return 0;
  }

  const std::size_t deleted = storage_.DeleteMany(orphans);
  if (deleted != orphans.size()) {
    LOG_WARNING() << "CollectGarbage: deleted " << deleted << " of "
                  << orphans.size() << " orphans; the rest retry next run.";
  } else {
    LOG_INFO() << "CollectGarbage: reclaimed " << deleted << " orphan objects";
  }

  return deleted;
}

}  // namespace ya_brander::components
