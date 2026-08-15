#pragma once
#include <userver/server/handlers/http_handler_base.hpp>
#include <userver/storages/postgres/cluster.hpp>

namespace ya_brander::handlers {

class SubscribeHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-users-subscribe";

  SubscribeHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context);

  std::string HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const override;

private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
};

class UnsubscribeHandler final : public userver::server::handlers::HttpHandlerBase {
public:
  static constexpr std::string_view kName = "handler-users-unsubscribe";

  UnsubscribeHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context);

  std::string HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const override;

private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
};

}  // namespace ya_brander::handlers
