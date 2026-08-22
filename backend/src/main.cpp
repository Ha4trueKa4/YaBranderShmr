#include <userver/clients/dns/component.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/clients/http/component_core.hpp>
#include <userver/clients/http/middlewares/pipeline_component.hpp>
#include <userver/components/minimal_server_component_list.hpp>
#include <userver/server/handlers/ping.hpp>
#include <userver/server/handlers/tests_control.hpp>
#include <userver/server/middlewares/cors.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/testsuite/testsuite_support.hpp>
#include <userver/utils/daemon_run.hpp>
#include "./components/auth_checker.hpp"
#include "./handlers/auth/login_handler.hpp"
#include "./handlers/auth/register_handler.hpp"
#include "./handlers/auth/yandex_oauth_handler.hpp"
#include "handlers/check/check.hpp"
#include "handlers/get_models/get_models.hpp"
#include "handlers/projects/append_projects.h"

#include "./handlers/users/append_users.hpp"
#include "components/object_storage/s3_object_storage_component.hpp"
#include "components/subscription_storage/subscription_storage_component.h"
#include "components/vector_search_component.h"
#include "components/version_storage/version_storage_component_impl.hpp"
#include "components/prompt_constructor/prompt_polisher_component.h"
#include "handlers/test_llm_handler/test_polisher_handler.h"
#include "handlers/feed/get_feed.h"
#include "handlers/test_llm_handler/test_llm_handler.hpp"
#include "handlers/test_vector_search/test_vector_search_handler.hpp"

int main(int argc, char* argv[]) {
    userver::server::handlers::auth::RegisterAuthCheckerFactory<ya_brander::auth::CheckerFactory>();
    userver::server::handlers::auth::RegisterAuthCheckerFactory<ya_brander::auth::CheckerFactoryRequired>();
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
            .Append<ya_brander::auth::YandexOAuthHandler>()
            // --- shared infrastructure: used by all feature modules ---
            .Append<userver::components::Postgres>("postgres-vector-db")
            .Append<userver::components::Postgres>("postgres-db-1")
            .Append<userver::clients::dns::Component>()
            .Append<userver::server::middlewares::CorsFactory>()
            .Append<ya_brander::handlers::TestVectorSearchHandler>()
            .Append<ya_brander::TestPolisherHandler>()
            .Append<ya_brander::handlers::GetFeedHandler>()
            // Регистрация модулей когда будут готовы
            .Append<ya_brander::components::S3ObjectStorageComponent>()
            .Append<ya_brander::components::VersionStorageComponentImpl>()
            .Append<ya_brander::components::SubscriptionStorageComponent>()
            .Append<ya_brander::components::PromptPolisherComponent>();

    ya_brander::AppendCheck(component_list);
    ya_brander::AppendLLM(component_list);
    ya_brander::handlers::AppendGetModels(component_list);
    // ya_brander::AppendAuth(component_list);
    ya_brander::AppendProjects(component_list);
    ya_brander::AppendUsers(component_list);

    return userver::utils::DaemonMain(argc, argv, component_list);
}
