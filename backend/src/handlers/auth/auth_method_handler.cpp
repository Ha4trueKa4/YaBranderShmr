#include "auth_method_handler.hpp"

#include <optional>

#include <userver/components/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/formats/serialize/common_containers.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/component.hpp>

#include "../../utils/errors.hpp"

namespace ya_brander::auth {

using utils::MakeError;

AuthMethodHandler::AuthMethodHandler(const userver::components::ComponentConfig& config,
                                      const userver::components::ComponentContext& context)
    : HttpHandlerJsonBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>("postgres-db-1")
                      .GetCluster()) {}

userver::formats::json::Value AuthMethodHandler::HandleRequestJsonThrow(
    const userver::server::http::HttpRequest& request,
    const userver::formats::json::Value&,
    userver::server::request::RequestContext& context) const {
    const auto& user_id = context.GetData<std::string>("user_id");

    const auto result = pg_cluster_->Execute(
        userver::storages::postgres::ClusterHostType::kSlave,
        "SELECT password_hash, provider FROM users WHERE id = $1::uuid", user_id);

    if (result.IsEmpty()) {
        request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
        return MakeError("USER_NOT_FOUND", "User not found");
    }

    const auto row = result.Front();
    const auto password_hash = row["password_hash"].As<std::optional<std::string>>();
    const auto provider = row["provider"].As<std::optional<std::string>>();

    userver::formats::json::ValueBuilder response;
    response["has_password"] = password_hash.has_value();
    response["provider"] = provider;
    return response.ExtractValue();
}

}  // namespace ya_brander::auth
