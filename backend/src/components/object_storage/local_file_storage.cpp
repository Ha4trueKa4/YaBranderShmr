#include "local_file_storage.hpp"

#include <filesystem>
#include <fstream>
#include <iterator>
#include <stdexcept>
#include <userver/components/component_context.hpp>
#include <userver/engine/async.hpp>

namespace ya_brander::components {

    LocalFileStorage::LocalFileStorage(
        const userver::components::ComponentConfig& config,
        const userver::components::ComponentContext& context)
        : ObjectStorageComponent(config, context),
          fs_task_processor_(context.GetTaskProcessor("fs-task-processor")) {
        userver::engine::AsyncNoSpan(fs_task_processor_, [] {
          std::filesystem::create_directories(std::string{kStorageDir});
        }).Get();
    }

    std::string LocalFileStorage::FullPath(const std::string& key) {
        return std::string{kStorageDir} + "/" + key;
    }

    void LocalFileStorage::Put(const std::string& key, const std::string& data) {
        const auto path = FullPath(key);
        userver::engine::AsyncNoSpan(fs_task_processor_, [&path, &data] {
          std::ofstream out{path};
          out << data;
          if (!out) {
            throw std::runtime_error("object storage: failed to write " + path);
          }
        }).Get();
    }

    std::string LocalFileStorage::Get(const std::string& key) {
        const auto path = FullPath(key);
        return userver::engine::AsyncNoSpan(fs_task_processor_, [&path] -> std::string {
          std::ifstream f{path};
          if (!f) return "";
          std::string content{std::istreambuf_iterator<char>(f),
                              std::istreambuf_iterator<char>()};

          if (f.bad()) {
            throw std::runtime_error("object storage: failed to read " + path);
          }
          return content;
        }).Get();
    }

    std::string LocalFileStorage::GetUrl(const std::string& /*key*/, bool /*is_public*/) {
        // TODO: реализуется вместе с S3. Локальное хранилище URL не имеет.
        return "";
    }

}  // namespace ya_brander::components