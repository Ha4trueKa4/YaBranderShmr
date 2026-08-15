#include "user_subscriptions_handlers.h"

#include <userver/formats/json/value_builder.hpp>
#include <userver/storages/postgres/exceptions.hpp>

namespace ya_brander::handlers {

namespace {

userver::formats::json::Value MakeInvalidUuidError() {
  userver::formats::json::ValueBuilder err;
  err["code"] = "INVALID_UUID";
  err["message"] = "Некорректный формат ID пользователя";
  return err.ExtractValue();
}

userver::formats::json::Value MakeUserNotFoundError() {
  userver::formats::json::ValueBuilder err;
  err["code"] = "USER_NOT_FOUND";
  err["message"] = "Пользователь не найден";
  return err.ExtractValue();
}

}  // namespace

GetUserSubscribersHandler::GetUserSubscribersHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      subscription_storage_(context.FindComponent<components::SubscriptionStorageComponent>()) {}

userver::formats::json::Value GetUserSubscribersHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& req_context) const {
  const auto& target_id = request.GetPathArg("id");

  std::string* auth_user_id_ptr = req_context.GetDataOptional<std::string>("user_id");
  std::string auth_user_id = auth_user_id_ptr ? *auth_user_id_ptr : "";

  try {
    auto subscribers = subscription_storage_.GetSubscribers(target_id, auth_user_id);

    if (subscribers.empty() && !subscription_storage_.IsUserExists(target_id)) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return MakeUserNotFoundError();
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

    return response_array.ExtractValue();

  } catch (const userver::storages::postgres::DataException&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeInvalidUuidError();
  }
}


GetUserSubscriptionsHandler::GetUserSubscriptionsHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      subscription_storage_(context.FindComponent<components::SubscriptionStorageComponent>()) {}

userver::formats::json::Value GetUserSubscriptionsHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& req_context) const {
  const auto& target_id = request.GetPathArg("id");

  std::string* auth_user_id_ptr = req_context.GetDataOptional<std::string>("user_id");
  std::string auth_user_id = auth_user_id_ptr ? *auth_user_id_ptr : "";

  try {
    auto subscriptions = subscription_storage_.GetSubscriptions(target_id, auth_user_id);

    if (subscriptions.empty() && !subscription_storage_.IsUserExists(target_id)) {
      request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
      return MakeUserNotFoundError();
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

    return response_array.ExtractValue();

  } catch (const userver::storages::postgres::DataException&) {
    request.SetResponseStatus(userver::server::http::HttpStatus::kBadRequest);
    return MakeInvalidUuidError();
  }
}

}  // namespace ya_brander::handlers