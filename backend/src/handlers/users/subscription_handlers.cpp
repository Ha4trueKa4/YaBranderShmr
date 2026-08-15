#include "subscription_handlers.hpp"

#include <userver/components/component.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

namespace ya_brander::handlers {

SubscribeHandler::SubscribeHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

std::string SubscribeHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
  std::string* user_id = context.GetDataOptional<std::string>("user_id");
  if (!user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  const auto& target_id = request.GetPathArg("id");
  if (target_id == *user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"message": "Cannot subscribe to yourself"})";
  }

  try {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "INSERT INTO subscriptions (subscriber_id, target_user_id) VALUES ($1::uuid, $2::uuid) ON CONFLICT DO NOTHING",
        *user_id, target_id);
  } catch (const userver::storages::postgres::ForeignKeyViolation&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return R"({"message": "User not found"})";
  }

  return R"({"message": "Subscribed successfully"})";
}

UnsubscribeHandler::UnsubscribeHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

std::string UnsubscribeHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
  std::string* user_id = context.GetDataOptional<std::string>("user_id");
  if (!user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return {};
  }

  const auto& target_id = request.GetPathArg("id");

  pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kMaster,
      "DELETE FROM subscriptions WHERE subscriber_id = $1::uuid AND target_user_id = $2::uuid",
      *user_id, target_id);

  return R"({"message": "Unsubscribed successfully"})";
}

}  // namespace ya_brander::handlers
