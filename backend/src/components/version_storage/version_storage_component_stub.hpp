#pragma once
#include <filesystem>
#include <fstream>
#include <iterator>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>
#include "version_storage_component.hpp"

namespace ya_brander::components {

/// @brief Демо-заглушка хранилища версий (без S3 и без БД).
/// @details Каждая версия сохраняется в отдельный .svg файл в mock_s3/.
///          GetLatestVersionData возвращает последнюю сохраненную версию.
///          Полноценная реализация будет добавлена позже.
class VersionStorageStub final : public VersionStorageComponent {
 public:
  static constexpr std::string_view kName = "version-storage";
  using VersionStorageComponent::VersionStorageComponent;

  std::string SaveVersion(const std::string& project_id,
                          const std::string& data) override {
    std::lock_guard lock{mutex_};
    std::filesystem::create_directories("mock_s3");
    const int num = ++counter_[project_id];
    auto key = "mock_s3/" + project_id + "_" + std::to_string(num) + ".svg";
    std::ofstream{key} << data;
    return key;
  }

  std::string GetLatestVersionData(const std::string& project_id) override {
    std::lock_guard lock{mutex_};
    auto it = counter_.find(project_id);
    if (it == counter_.end()) return "";
    auto key = "mock_s3/" + project_id + "_" +
               std::to_string(it->second) + ".svg";
    return ReadFile(key);
  }

  std::string GetVersion(const std::string&,
                         const std::string& version_id) override {
    std::lock_guard lock{mutex_};
    return ReadFile(version_id);  // version_id трактуется как ключ (путь к файлу)
  }

  std::vector<models::Version> ListVersions(const std::string&) override {
    return {};  // в демо не используется
  }

  std::string RestoreVersion(const std::string& project_id,
                             const std::string& version_id) override {
    return SaveVersion(project_id, GetVersion(project_id, version_id));
  }

 private:
  static std::string ReadFile(const std::string& key) {
    std::ifstream f{key};
    if (!f) return "";
    return std::string{std::istreambuf_iterator<char>(f),
                       std::istreambuf_iterator<char>()};
  }

  std::mutex mutex_;
  std::unordered_map<std::string, int> counter_;
};

}  // namespace ya_brander::components