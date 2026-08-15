#pragma once

#include <cstdint>
#include <map>
#include <string>
#include <string_view>
#include <vector>

#include <userver/clients/http/client.hpp>
#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/http/header_map.hpp>
#include <userver/yaml_config/schema.hpp>

#include "object_storage_component.hpp"

namespace ya_brander::components {


class S3ObjectStorageComponent final : public ObjectStorageComponent {
 public:
  S3ObjectStorageComponent(
      const userver::components::ComponentConfig& config,
      const userver::components::ComponentContext& context);

  void Put(const std::string& key, const std::string& data,
           bool make_public) override;
  std::string Get(const std::string& key) override;
  std::string GetUrl(const std::string& key, bool is_public) override;
  void SetAcl(const std::string& key, bool make_public) override;
  void Delete(const std::string& key) override;
  void DeleteMany(const std::vector<std::string>& keys) override;

  static userver::yaml_config::Schema GetStaticConfigSchema();

 private:
  userver::http::headers::HeaderMap SignRequest(
      std::string_view method, std::string_view canonical_uri,
      std::string_view canonical_query, std::string_view payload_hash,
      std::map<std::string, std::string> extra_signed_headers) const;

  std::string MakePresignedGetUrl(const std::string& key) const;
  std::string ObjectUrl(const std::string& key) const;
  std::string CanonicalUri(const std::string& key) const;

  userver::clients::http::Client& http_client_;
  std::string endpoint_;
  std::string region_;
  std::string bucket_;
  std::string access_key_id_;
  std::string secret_key_;
  std::int64_t presigned_ttl_seconds_;
};

}  // namespace ya_brander::components