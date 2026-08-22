#include "subscription_handlers.hpp"

#include <algorithm>
#include <cctype>
#include <string_view>

#include <userver/components/component.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include "../../utils/errors.hpp"

namespace ya_brander::handlers {

using utils::MakeMessage;

namespace {

bool AreSameUuid(std::string_view lhs, std::string_view rhs) {
  return lhs.size() == rhs.size() &&
         std::equal(lhs.begin(), lhs.end(), rhs.begin(),
                    [](unsigned char left, unsigned char right) {
                      return std::tolower(left) == std::tolower(right);
                    });
}

}  // namespace

SubscribeHandler::SubscribeHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

userver::formats::json::Value SubscribeHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
  const auto& user_id = context.GetData<std::string>("user_id");

  const auto& target_id = request.GetPathArg("id");
  if (AreSameUuid(target_id, user_id)) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeMessage("Cannot subscribe to yourself");
  }

  try {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "INSERT INTO subscriptions (subscriber_id, target_user_id) VALUES ($1::uuid, $2::uuid) ON CONFLICT DO NOTHING",
        user_id, target_id);
  } catch (const userver::storages::postgres::ForeignKeyViolation&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return MakeMessage("User not found");
  }

  return MakeMessage("Subscribed successfully");
}

UnsubscribeHandler::UnsubscribeHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

userver::formats::json::Value UnsubscribeHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
  const auto& user_id = context.GetData<std::string>("user_id");

  const auto& target_id = request.GetPathArg("id");

  pg_cluster_->Execute(
      userver::storages::postgres::ClusterHostType::kMaster,
      "DELETE FROM subscriptions WHERE subscriber_id = $1::uuid AND target_user_id = $2::uuid",
      user_id, target_id);

  return MakeMessage("Unsubscribed successfully");
}

DeleteSubscriberHandler::DeleteSubscriberHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1").GetCluster()) {}

userver::formats::json::Value DeleteSubscriberHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
  const auto& auth_user_id = context.GetData<std::string>("user_id");

  const auto& target_user_id = request.GetPathArg("id");              // пользователь, у которого удаляем подписчика
  const auto& subscriber_id = request.GetPathArg("subscriber_id");    // кого удаляем из подписчиков

  if (auth_user_id != target_user_id) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
    return MakeMessage("Permission denied");
  }

  // Удаляем подписку
  try {
    pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "DELETE FROM subscriptions WHERE subscriber_id = $1::uuid AND target_user_id = $2::uuid",
        subscriber_id, target_user_id);
  } catch (const userver::storages::postgres::ForeignKeyViolation&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
    return MakeMessage("User not found");
  }

  return MakeMessage("Subscriber removed successfully");
}

}  // namespace ya_brander::handlers
