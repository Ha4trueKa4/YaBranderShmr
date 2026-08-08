#include "check.hpp"
#include "services/cart_checker.hpp"

#include <userver/components/component_config.hpp>
#include <userver/components/component_context.hpp>
#include <userver/server/handlers/http_handler_base.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/clients/dns/component.hpp>
#include <userver/formats/json.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/utils/assert.hpp>



namespace code_architecture {

namespace {

class Check final : public userver::server::handlers::HttpHandlerBase {
 public:
  static constexpr std::string_view kName = "handler-check";

  Check(const userver::components::ComponentConfig& config,
        const userver::components::ComponentContext& component_context)
      : HttpHandlerBase(config, component_context),
        pg_cluster_(
            component_context
                .FindComponent<userver::components::Postgres>("postgres-db-1")
                .GetCluster()),
        cart_checker_(pg_cluster_) {}

  std::string HandleRequestThrow(
      const userver::server::http::HttpRequest& request,
      userver::server::request::RequestContext&) const override {
    const auto& user_id_str = request.GetArg("user_id");
    int user_id = 0;
    user_id = std::stoi(user_id_str);

    const auto& item_ids = request.GetArgVector("item_id");
    std::vector<std::string> items(item_ids.begin(), item_ids.end());

    auto problems = cart_checker_.Check(user_id, items);

    userver::formats::json::ValueBuilder builder;
    builder = userver::formats::json::Type::kArray;
    for (const auto& p : problems) {
      userver::formats::json::ValueBuilder item;
      item["item_id"] = p.item_id;
      item["problem"] = p.problem;
      builder.PushBack(std::move(item));
    }

    return userver::formats::json::ToString(builder.ExtractValue());
  }

 private:
  userver::storages::postgres::ClusterPtr pg_cluster_;
  mutable CartChecker cart_checker_;
};

}  // namespace

void AppendCheck(userver::components::ComponentList& component_list) {
  component_list.Append<Check>();
  component_list.Append<userver::components::Postgres>("postgres-db-1");
  component_list.Append<userver::clients::dns::Component>();
}

}  // namespace code_architecture