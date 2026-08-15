#include "user_subscriptions_handlers.h"

#include <userver/formats/json/serialize.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/storages/postgres/exceptions.hpp>

namespace ya_brander::handlers {

GetUserSubscribersHandler::GetUserSubscribersHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      subscription_storage_(context.FindComponent<components::SubscriptionStorageComponent>()) {}

std::string GetUserSubscribersHandler::HandleRequestThrow(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext& req_context) const {
  const auto& target_id = request.GetPathArg("id");

  std::string* auth_user_id_ptr = req_context.GetDataOptional<std::string>("user_id");
  std::string auth_user_id = auth_user_id_ptr ? *auth_user_id_ptr : "";

  try {
    auto subscribers = subscription_storage_.GetSubscribers(target_id, auth_user_id);

    if (subscribers.empty() && !subscription_storage_.IsUserExists(target_id)) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return R"({"code": "USER_NOT_FOUND", "message": "Пользователь не найден"})";
    }

    userver::formats::json::ValueBuilder response_array(userver::formats::common::Type::kArray);
    for (const auto& user : subscribers) {
      userver::formats::json::ValueBuilder user_obj;
      user_obj["id"] = user.id;
      user_obj["name"] = user.name;

      if (user.is_subscribed.has_value()) {
        user_obj["is_subscribed"] = *user.is_subscribed;
      }

      response_array.PushBack(std::move(user_obj));
    }

    return userver::formats::json::ToString(response_array.ExtractValue());

  } catch (const userver::storages::postgres::DataException&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"code": "INVALID_UUID", "message": "Некорректный формат ID пользователя"})";
  }
}


GetUserSubscriptionsHandler::GetUserSubscriptionsHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      subscription_storage_(context.FindComponent<components::SubscriptionStorageComponent>()) {}

std::string GetUserSubscriptionsHandler::HandleRequestThrow(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext& req_context) const {
  const auto& target_id = request.GetPathArg("id");

  std::string* auth_user_id_ptr = req_context.GetDataOptional<std::string>("user_id");
  std::string auth_user_id = auth_user_id_ptr ? *auth_user_id_ptr : "";

  try {
    auto subscriptions = subscription_storage_.GetSubscriptions(target_id, auth_user_id);

    if (subscriptions.empty() && !subscription_storage_.IsUserExists(target_id)) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return R"({"code": "USER_NOT_FOUND", "message": "Пользователь не найден"})";
    }

    userver::formats::json::ValueBuilder response_array(userver::formats::common::Type::kArray);
    for (const auto& user : subscriptions) {
      userver::formats::json::ValueBuilder user_obj;
      user_obj["id"] = user.id;
      user_obj["name"] = user.name;

      if (user.is_subscribed.has_value()) {
        user_obj["is_subscribed"] = *user.is_subscribed;
      }

      response_array.PushBack(std::move(user_obj));
    }

    return userver::formats::json::ToString(response_array.ExtractValue());

  } catch (const userver::storages::postgres::DataException&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return R"({"code": "INVALID_UUID", "message": "Некорректный формат ID пользователя"})";
  }
}

}  // namespace ya_brander::handlers