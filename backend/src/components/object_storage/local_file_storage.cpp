#include "local_file_storage.hpp"

#include <filesystem>
#include <fstream>
#include <iterator>
#include <stdexcept>

namespace ya_brander::components {

    LocalFileStorage::LocalFileStorage(
        const userver::components::ComponentConfig& config,
        const userver::components::ComponentContext& context)
        : ObjectStorageComponent(config, context) {
        std::filesystem::create_directories(kStorageDir);
    }

    std::string LocalFileStorage::FullPath(const std::string& key) {
        return std::string{kStorageDir} + "/" + key;
    }

    void LocalFileStorage::Put(const std::string& key, const std::string& data) {
        const auto path = FullPath(key);
        std::ofstream out{path};
        out << data;
        if (!out) {
            throw std::runtime_error("object storage: failed to write " + path);
        }
    }

    std::string LocalFileStorage::Get(const std::string& key) {
        std::ifstream f{FullPath(key)};
        if (!f) return "";
        return std::string{std::istreambuf_iterator<char>(f),
                           std::istreambuf_iterator<char>()};
    }

    // TODO: Мок метода из интерфейса, пока не создан S3.
    std::string LocalFileStorage::GetUrl(const std::string& key,
                                         bool) {
      std::ifstream f{FullPath(key)};
      if (!f) return "";
      return std::string{std::istreambuf_iterator<char>(f),
                         std::istreambuf_iterator<char>()};
    }

    }  // namespace ya_brander::components