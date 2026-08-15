#include <userver/clients/dns/component.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/clients/http/component_core.hpp>
#include <userver/clients/http/middlewares/pipeline_component.hpp>
#include <userver/components/minimal_server_component_list.hpp>
#include <userver/server/handlers/ping.hpp>
#include <userver/server/middlewares/cors.hpp>
#include <userver/server/handlers/tests_control.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/testsuite/testsuite_support.hpp>
#include <userver/utils/daemon_run.hpp>
#include "./components/auth_checker.hpp"
#include "./handlers/auth/register_handler.hpp"
#include "./handlers/auth/login_handler.hpp"
#include "handlers/check/check.hpp"
#include "handlers/projects/append_projects.h"

#include "handlers/test_llm_handler/test_llm_handler.hpp"
#include "components/version_storage/version_storage_component_impl.hpp"
#include "components/object_storage/local_file_storage.hpp"
#include "./handlers/users/append_users.hpp"

int main(int argc, char* argv[]) {
    userver::server::handlers::auth::RegisterAuthCheckerFactory<ya_brander::auth::CheckerFactory>();
    auto component_list =
        userver::components::MinimalServerComponentList()
            .Append<userver::server::handlers::Ping>()
            .Append<userver::components::TestsuiteSupport>()
            .Append<userver::components::HttpClient>()
            .Append<userver::components::HttpClientCore>()
            .Append<userver::clients::http::MiddlewarePipelineComponent>()
            .Append<userver::server::handlers::TestsControl>()
            .Append<ya_brander::auth::LoginHandler>()
            .Append<ya_brander::auth::RegisterHandler>()
            // --- shared infrastructure: used by all feature modules ---
            .Append<userver::components::Postgres>("postgres-db-1")
            .Append<userver::clients::dns::Component>()
            .Append<ya_brander::components::VersionStorageComponentImpl>()
            .Append<ya_brander::components::LocalFileStorage>()
            .Append<userver::server::middlewares::CorsFactory>();
    
    // Регистрация модулей когда будут готовы
    ya_brander::AppendCheck(component_list);
    ya_brander::AppendLLM(component_list);
    // ya_brander::AppendAuth(component_list);
    ya_brander::AppendProjects(component_list);
    ya_brander::AppendUsers(component_list);

    return userver::utils::DaemonMain(argc, argv, component_list);
}
