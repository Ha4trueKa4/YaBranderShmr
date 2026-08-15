#pragma once
#include <userver/server/handlers/http_handler_json_base.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include "../../components/version_storage/version_storage_component_impl.hpp"

namespace ya_brander::handlers {

class GetFeedHandler final : public userver::server::handlers::HttpHandlerJsonBase {
public:
  static constexpr std::string_view kName = "handler-feed";

  GetFeedHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context);

  userver::formats::json::Value HandleRequestJsonThrow(
      const userver::server::http::HttpRequest& request,
      const userver::formats::json::Value& request_json,
      userver::server::request::RequestContext& context) const override;

private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
  components::VersionStorageComponentImpl& version_storage_;
};

}  // namespace ya_brander::handlers
