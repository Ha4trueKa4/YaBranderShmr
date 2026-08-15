#include "get_feed.h"

#include <algorithm>
#include <optional>
#include <userver/components/component.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

namespace {

constexpr int kDefaultLimit = 20;
constexpr int kMaxLimit = 50;

const std::string kDiscoverQuery = R"(
  SELECT p.id::text AS id, p.author_id::text AS author_id, u.login AS author_login,
         p.name, p.created_at, COALESCE(p.last_version_key, '') AS last_version_key
  FROM projects p
  JOIN users u ON u.id = p.author_id
  WHERE p.is_private = FALSE
    AND ($1::timestamptz IS NULL OR (p.created_at, p.id) < ($1::timestamptz, $2::uuid))
  ORDER BY p.created_at DESC, p.id DESC
  LIMIT $3
)";

const std::string kFollowingQuery = R"(
  SELECT p.id::text AS id, p.author_id::text AS author_id, u.login AS author_login,
         p.name, p.created_at, COALESCE(p.last_version_key, '') AS last_version_key
  FROM projects p
  JOIN users u ON u.id = p.author_id
  JOIN subscriptions s ON s.target_user_id = p.author_id
  WHERE p.is_private = FALSE
    AND s.subscriber_id = $4::uuid
    AND ($1::timestamptz IS NULL OR (p.created_at, p.id) < ($1::timestamptz, $2::uuid))
  ORDER BY p.created_at DESC, p.id DESC
  LIMIT $3
)";

std::optional<std::pair<std::string, std::string>> DecodeCursor(const std::string& cursor) {
  const auto sep = cursor.rfind('|');
  if (sep == std::string::npos) return std::nullopt;
  return std::make_pair(cursor.substr(0, sep), cursor.substr(sep + 1));
}

}  // namespace

GetFeedHandler::GetFeedHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()),
      version_storage_(context.FindComponent<components::VersionStorageComponentImpl>()) {}

userver::formats::json::Value GetFeedHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
  const std::string scope = request.HasArg("scope") ? request.GetArg("scope") : "discover";
  if (scope != "discover" && scope != "following") {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    userver::formats::json::ValueBuilder err;
    err["message"] = "scope must be 'discover' or 'following'";
    return err.ExtractValue();
  }

  std::string* user_id = context.GetDataOptional<std::string>("user_id");
  if (scope == "following" && !user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  int limit = kDefaultLimit;
  if (request.HasArg("limit")) {
    try {
      limit = std::stoi(request.GetArg("limit"));
    } catch (const std::exception&) {
      // игнорируем некорректный limit, остаётся значение по умолчанию
    }
  }
  limit = std::clamp(limit, 1, kMaxLimit);

  std::optional<std::string> cursor_created_at;
  std::optional<std::string> cursor_id;
  if (request.HasArg("cursor")) {
    if (auto decoded = DecodeCursor(request.GetArg("cursor"))) {
      cursor_created_at = decoded->first;
      cursor_id = decoded->second;
    }
  }

  const auto res = (scope == "following")
      ? pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            kFollowingQuery, cursor_created_at, cursor_id, limit + 1, *user_id)
      : pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kSlave,
            kDiscoverQuery, cursor_created_at, cursor_id, limit + 1);

  const bool has_more = res.Size() > static_cast<std::size_t>(limit);
  const std::size_t items_count = has_more ? static_cast<std::size_t>(limit) : res.Size();

  std::vector<std::string> keys;
  keys.reserve(items_count);
  for (std::size_t i = 0; i < items_count; ++i) {
    auto key = res[i]["last_version_key"].As<std::string>();
    if (!key.empty()) keys.push_back(key);
  }
  const auto urls = version_storage_.GetManyUrls(keys, true);

  userver::formats::json::ValueBuilder items_builder{userver::formats::json::Type::kArray};
  for (std::size_t i = 0; i < items_count; ++i) {
    const auto row = res[i];
    const auto key = row["last_version_key"].As<std::string>();
    const auto url_it = urls.find(key);

    userver::formats::json::ValueBuilder item;
    item["id"] = row["id"].As<std::string>();
    item["author_id"] = row["author_id"].As<std::string>();
    item["author_login"] = row["author_login"].As<std::string>();
    item["name"] = row["name"].As<std::string>();
    item["created_at"] = userver::utils::datetime::Timestring(
        row["created_at"].As<std::chrono::system_clock::time_point>(),
        userver::utils::datetime::kDefaultTimezone, userver::utils::datetime::kRfc3339Format);
    item["preview_url"] = (url_it != urls.end()) ? url_it->second : "";
    items_builder.PushBack(std::move(item));
  }

  userver::formats::json::ValueBuilder response;
  response["items"] = std::move(items_builder);
  if (has_more) {
    const auto last_row = res[items_count - 1];
    const auto created_at_str = userver::utils::datetime::Timestring(
        last_row["created_at"].As<std::chrono::system_clock::time_point>(),
        userver::utils::datetime::kDefaultTimezone, userver::utils::datetime::kRfc3339Format);
    response["next_cursor"] = created_at_str + "|" + last_row["id"].As<std::string>();
  }

  return response.ExtractValue();
}

}  // namespace ya_brander::handlers
