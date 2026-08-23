#include "subscription_handlers.hpp"

#include <algorithm>
#include <cctype>
#include <cstddef>
#include <string_view>

#include <userver/components/component.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/storages/postgres/exceptions.hpp>

#include <ya_brander/sql_queries.hpp>

#include "../../utils/errors.hpp"

namespace ya_brander::handlers {

using utils::MakeError;
using utils::MakeMessage;

namespace {

bool IsValidUuid(std::string_view value) {
  if (value.size() != 36) {
    return false;
  }

  for (std::size_t index = 0; index < value.size(); ++index) {
    const bool is_hyphen_position =
        index == 8 || index == 13 || index == 18 || index == 23;
    if (is_hyphen_position) {
      if (value[index] != '-') {
        return false;
      }
    } else if (!std::isxdigit(static_cast<unsigned char>(value[index]))) {
      return false;
    }
  }

  return true;
}

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
        sql::kInsertSubscription,
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
      sql::kDeleteSubscription,
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

  const auto& target_user_id = request.GetPathArg("id");
  const auto& subscriber_id = request.GetPathArg("subscriber_id");

  if (!IsValidUuid(target_user_id)) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeError("INVALID_UUID", "Invalid user ID format", "id");
  }
  if (!IsValidUuid(subscriber_id)) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeError("INVALID_UUID", "Invalid subscriber ID format",
                     "subscriber_id");
  }

  if (!AreSameUuid(auth_user_id, target_user_id)) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
    return MakeMessage("Permission denied");
  }

  try {
    const auto result = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        sql::kDeleteSubscriber,
        subscriber_id, target_user_id);

    if (result.IsEmpty()) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return MakeMessage("Subscription not found");
    }
  } catch (const userver::storages::postgres::DataException&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeError("INVALID_UUID", "Invalid UUID format");
  }

  return MakeMessage("Subscriber removed successfully");
}

}  // namespace ya_brander::handlers
