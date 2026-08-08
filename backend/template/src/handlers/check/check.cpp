#include "check.hpp"

#include <userver/server/handlers/http_handler_base.hpp>

namespace code_architecture {

  namespace {

    class Check final : public userver::server::handlers::HttpHandlerBase {
    public:
      static constexpr std::string_view kName = "handler-check";

      using HttpHandlerBase::HttpHandlerBase;   // inherit the base constructor

      std::string HandleRequestThrow(
          const userver::server::http::HttpRequest&,
          userver::server::request::RequestContext&) const override {
        return R"({"status":"ok"})";
      }
    };

  }  // namespace

  void AppendCheck(userver::components::ComponentList& component_list) {
    component_list.Append<Check>();
  }

}  // namespace code_architecture