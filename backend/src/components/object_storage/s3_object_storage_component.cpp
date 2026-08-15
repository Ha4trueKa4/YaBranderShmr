#include "s3_object_storage_component.hpp"

#include <chrono>
#include <cstdlib>
#include <stdexcept>
#include <string>

#include <userver/clients/http/component.hpp>
#include <userver/components/component_context.hpp>
#include <userver/crypto/hash.hpp>
#include <userver/formats/json/value.hpp>
#include <userver/logging/log.hpp>
#include <userver/utils/datetime.hpp>
#include <userver/yaml_config/merge_schemas.hpp>

namespace ya_brander::components {

namespace {

std::string GetScheme(std::string_view endpoint) {
  if (endpoint.find("http://") == 0 || endpoint.find("https://") == 0) {
    return ""; // Протокол уже есть в строке (например, от mockserver)
  }
  return "https://"; // По умолчанию для продакшена
}

namespace hash = userver::crypto::hash;

constexpr std::string_view kAlgorithm = "AWS4-HMAC-SHA256";
constexpr std::string_view kService = "s3";
constexpr std::string_view kUnsignedPayload = "UNSIGNED-PAYLOAD";
constexpr std::string_view kEmptyPayloadSha256 =
    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

std::string UriEncode(std::string_view input, bool encode_slash) {
  static constexpr char kHexDigits[] = "0123456789ABCDEF";
  std::string out;
  out.reserve(input.size() * 3);
  for (const unsigned char c : input) {
    const bool unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
                            (c >= '0' && c <= '9') || c == '-' || c == '_' ||
                            c == '.' || c == '~';
    if (unreserved) {
      out.push_back(static_cast<char>(c));
    } else if (c == '/' && !encode_slash) {
      out.push_back('/');
    } else {
      out.push_back('%');
      out.push_back(kHexDigits[c >> 4]);
      out.push_back(kHexDigits[c & 0x0F]);
    }
  }
  return out;
}

std::string SigningKey(std::string_view secret, std::string_view datestamp,
                       std::string_view region) {
  const auto k_date = hash::HmacSha256("AWS4" + std::string{secret}, datestamp,
                                       hash::OutputEncoding::kBinary);
  const auto k_region =
      hash::HmacSha256(k_date, region, hash::OutputEncoding::kBinary);
  const auto k_service =
      hash::HmacSha256(k_region, kService, hash::OutputEncoding::kBinary);
  return hash::HmacSha256(k_service, "aws4_request",
                          hash::OutputEncoding::kBinary);
}

int StatusOf(const std::shared_ptr<userver::clients::http::Response>& resp) {
  return static_cast<int>(resp->status_code());
}

}  // namespace

S3ObjectStorageComponent::S3ObjectStorageComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : ObjectStorageComponent(config, context),
      http_client_(context.FindComponent<userver::components::HttpClient>()
                       .GetHttpClient()),
      endpoint_(config["endpoint"].As<std::string>("storage.yandexcloud.net")),
      region_(config["region"].As<std::string>("ru-central1")),
      bucket_(config["bucket"].As<std::string>()),
      presigned_ttl_seconds_(
          config["presigned-url-ttl-seconds"].As<std::int64_t>(900)) {
  const char* key_id = std::getenv("S3_ACCESS_KEY_ID");
  const char* secret = std::getenv("S3_SECRET_KEY");
  if (key_id == nullptr || secret == nullptr) {
    throw std::runtime_error(
        "S3 credentials missing: set S3_ACCESS_KEY_ID and S3_SECRET_KEY");
  }
  access_key_id_ = key_id;
  secret_key_ = secret;
}

std::string S3ObjectStorageComponent::CanonicalUri(const std::string& key) const {
  return "/" + bucket_ + "/" + UriEncode(key, /*encode_slash=*/false);
}

std::string S3ObjectStorageComponent::ObjectUrl(const std::string& key) const {
  std::string url = GetScheme(endpoint_) + endpoint_;
  if (!url.empty() && url.back() == '/') url.pop_back(); // Убираем слэш от mockserver
  return url + CanonicalUri(key);
}

userver::http::headers::HeaderMap S3ObjectStorageComponent::SignRequest(
    std::string_view method, std::string_view canonical_uri,
    std::string_view canonical_query, std::string_view payload_hash,
    std::map<std::string, std::string> extra_signed_headers) const {
  const auto now = userver::utils::datetime::Now();
  const auto amz_date =
      userver::utils::datetime::Timestring(now, "UTC", "%Y%m%dT%H%M%SZ");
  const auto datestamp =
      userver::utils::datetime::Timestring(now, "UTC", "%Y%m%d");

  std::map<std::string, std::string> signed_map = std::move(extra_signed_headers);
  signed_map["host"] = endpoint_;
  signed_map["x-amz-content-sha256"] = std::string{payload_hash};
  signed_map["x-amz-date"] = amz_date;

  std::string canonical_headers;
  std::string signed_headers;
  for (const auto& [name, value] : signed_map) {
    canonical_headers += name + ':' + value + '\n';
    if (!signed_headers.empty()) signed_headers += ';';
    signed_headers += name;
  }

  const std::string canonical_request =
      std::string{method} + '\n' + std::string{canonical_uri} + '\n' +
      std::string{canonical_query} + '\n' + canonical_headers + '\n' +
      signed_headers + '\n' + std::string{payload_hash};

  const std::string credential_scope =
      datestamp + '/' + region_ + '/' + std::string{kService} + "/aws4_request";

  const std::string string_to_sign =
      std::string{kAlgorithm} + '\n' + amz_date + '\n' + credential_scope +
      '\n' + hash::Sha256(canonical_request, hash::OutputEncoding::kHex);

  const auto signing_key = SigningKey(secret_key_, datestamp, region_);
  const auto signature = hash::HmacSha256(signing_key, string_to_sign,
                                          hash::OutputEncoding::kHex);

  const std::string authorization =
      std::string{kAlgorithm} + " Credential=" + access_key_id_ + '/' +
      credential_scope + ", SignedHeaders=" + signed_headers +
      ", Signature=" + signature;

  userver::http::headers::HeaderMap out;
  for (const auto& [name, value] : signed_map) {
    if (name == "host") continue;  // x-amz-date, x-amz-content-sha256, content-type, x-amz-acl
    out[name] = value;
  }
  const std::string auth_header = "Authorization";
  out[auth_header] = authorization;
  return out;
}

void S3ObjectStorageComponent::Put(const std::string& key,
                                   const std::string& data, bool make_public) {
  const auto payload_hash = hash::Sha256(data, hash::OutputEncoding::kHex);

  std::map<std::string, std::string> signed_headers{
      {"content-type", "image/svg+xml"}};
  if (make_public) signed_headers["x-amz-acl"] = "public-read";

  auto headers =
      SignRequest("PUT", CanonicalUri(key), "", payload_hash, signed_headers);

  auto response = http_client_.CreateRequest()
                      .put(ObjectUrl(key))
                      .headers(std::move(headers))
                      .data(data)
                      .timeout(std::chrono::seconds{30})
                      .perform();

  const int code = StatusOf(response);
  if (code != 200) {
    LOG_ERROR() << "S3 Put failed key=" << key << " status=" << code
                << " body=" << response->body_view();
    throw std::runtime_error("object storage Put failed, status=" +
                             std::to_string(code));
  }
}

std::string S3ObjectStorageComponent::Get(const std::string& key) {
  auto headers =
      SignRequest("GET", CanonicalUri(key), "", kEmptyPayloadSha256, {});

  auto response = http_client_.CreateRequest()
                      .get(ObjectUrl(key))
                      .headers(std::move(headers))
                      .timeout(std::chrono::seconds{30})
                      .perform();

  const int code = StatusOf(response);
  if (code == 404) return {};
  if (code != 200) {
    LOG_ERROR() << "S3 Get failed key=" << key << " status=" << code
                << " body=" << response->body_view();
    throw std::runtime_error("object storage Get failed, status=" +
                             std::to_string(code));
  }
  return response->body();
}

void S3ObjectStorageComponent::SetAcl(const std::string& key, bool make_public) {
  const std::string acl = make_public ? "public-read" : "private";

  auto headers = SignRequest("PUT", CanonicalUri(key), "acl=",
                             kEmptyPayloadSha256, {{"x-amz-acl", acl}});

  auto response = http_client_.CreateRequest()
                      .put(ObjectUrl(key) + "?acl")
                      .headers(std::move(headers))
                      .timeout(std::chrono::seconds{30})
                      .perform();

  const int code = StatusOf(response);
  if (code != 200) {
    LOG_ERROR() << "S3 SetAcl failed key=" << key << " acl=" << acl
                << " status=" << code << " body=" << response->body_view();
    throw std::runtime_error("object storage SetAcl failed, status=" +
                             std::to_string(code));
  }
}

void S3ObjectStorageComponent::Delete(const std::string& key) {
  auto headers =
      SignRequest("DELETE", CanonicalUri(key), "", kEmptyPayloadSha256, {});

  auto response = http_client_.CreateRequest()
                      .delete_method(ObjectUrl(key))
                      .headers(std::move(headers))
                      .timeout(std::chrono::seconds{30})
                      .perform();

  const int code = StatusOf(response);
  if (code != 204 && code != 200 && code != 404) {
    LOG_ERROR() << "S3 Delete failed key=" << key << " status=" << code
                << " body=" << response->body_view();
    throw std::runtime_error("object storage Delete failed, status=" +
                             std::to_string(code));
  }
}

void S3ObjectStorageComponent::DeleteMany(
    const std::vector<std::string>& keys) {
  for (const auto& key : keys) {
    Delete(key);
  }
}

std::string S3ObjectStorageComponent::GetUrl(const std::string& key,
                                             bool is_public) {
  if (is_public) {
    return ObjectUrl(key);
  }
  return MakePresignedGetUrl(key);
}

std::string S3ObjectStorageComponent::MakePresignedGetUrl(
    const std::string& key) const {
  const auto now = userver::utils::datetime::Now();
  const auto amz_date =
      userver::utils::datetime::Timestring(now, "UTC", "%Y%m%dT%H%M%SZ");
  const auto datestamp =
      userver::utils::datetime::Timestring(now, "UTC", "%Y%m%d");

  const std::string canonical_uri = CanonicalUri(key);
  const std::string credential_scope =
      datestamp + '/' + region_ + '/' + std::string{kService} + "/aws4_request";
  const std::string credential = access_key_id_ + '/' + credential_scope;

  std::map<std::string, std::string> params;
  params["X-Amz-Algorithm"] = std::string{kAlgorithm};
  params["X-Amz-Credential"] = credential;
  params["X-Amz-Date"] = amz_date;
  params["X-Amz-Expires"] = std::to_string(presigned_ttl_seconds_);
  params["X-Amz-SignedHeaders"] = "host";

  std::string canonical_query;
  for (const auto& [k, v] : params) {
    if (!canonical_query.empty()) canonical_query += '&';
    canonical_query += UriEncode(k, true) + '=' + UriEncode(v, true);
  }

  const std::string canonical_request =
      std::string{"GET"} + '\n' + canonical_uri + '\n' + canonical_query + '\n' +
      "host:" + endpoint_ + '\n' + '\n' + "host" + '\n' +
      std::string{kUnsignedPayload};

  const std::string string_to_sign =
      std::string{kAlgorithm} + '\n' + amz_date + '\n' + credential_scope +
      '\n' + hash::Sha256(canonical_request, hash::OutputEncoding::kHex);

  const auto signing_key = SigningKey(secret_key_, datestamp, region_);
  const auto signature = hash::HmacSha256(signing_key, string_to_sign,
                                          hash::OutputEncoding::kHex);

  std::string url = GetScheme(endpoint_) + endpoint_;
  if (!url.empty() && url.back() == '/') url.pop_back();

  return url + canonical_uri + '?' + canonical_query +
         "&X-Amz-Signature=" + signature;
}

userver::yaml_config::Schema S3ObjectStorageComponent::GetStaticConfigSchema() {
  return userver::yaml_config::MergeSchemas<
      userver::components::LoggableComponentBase>(R"(
type: object
description: S3-compatible object storage (Yandex Cloud Object Storage by default)
additionalProperties: false
properties:
    endpoint:
        type: string
        description: S3 endpoint host, without scheme
        defaultDescription: storage.yandexcloud.net
    region:
        type: string
        description: S3 region
        defaultDescription: ru-central1
    bucket:
        type: string
        description: bucket name
    presigned-url-ttl-seconds:
        type: integer
        description: lifetime of presigned GET URLs for private projects, seconds (max 604800)
        defaultDescription: '900'
)");
}

}  // namespace ya_brander::components