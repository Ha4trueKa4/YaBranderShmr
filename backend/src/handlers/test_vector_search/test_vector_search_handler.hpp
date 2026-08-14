#pragma once

#include <userver/server/handlers/http_handler_base.hpp>

#include "components/vector_search_component.h"

namespace ya_brander::handlers {

class TestVectorSearchHandler final : public userver::server::handlers::HttpHandlerBase {
public:
    static constexpr std::string_view kName = "handler-test-vector-search";

    TestVectorSearchHandler(const userver::components::ComponentConfig& config,
                            const userver::components::ComponentContext& context);

    std::string HandleRequestThrow(
        const userver::server::http::HttpRequest& request,
        userver::server::request::RequestContext& context) const override;

private:
    ya_brander::components::VectorSearchComponent& vector_search_;
};

} // namespace ya_brander::handlers