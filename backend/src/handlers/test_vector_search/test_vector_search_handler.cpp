#include "test_vector_search_handler.hpp"

#include <string>

#include <userver/components/component_context.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/formats/serialize/common_containers.hpp>

namespace ya_brander::handlers {

TestVectorSearchHandler::TestVectorSearchHandler(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      vector_search_(context.FindComponent<ya_brander::components::VectorSearchComponent>()) {}

std::string TestVectorSearchHandler::HandleRequestThrow(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext&) const {

    const std::string description{request.GetArg("description")};
    const auto limit_arg = request.GetArg("limit");
    const int limit = limit_arg.empty() ? 3 : std::stoi(std::string{limit_arg});

    const auto results = vector_search_.FindSimilar(description, limit);

    userver::formats::json::ValueBuilder builder;
    builder["description"] = description;
    builder["count"] = static_cast<int>(results.size());
    builder["results"] = results;

    return userver::formats::json::ToString(builder.ExtractValue());
}

} // namespace ya_brander::handlers