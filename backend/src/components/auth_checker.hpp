#pragma once

#include <userver/server/handlers/auth/auth_checker_base.hpp>
#include <userver/server/handlers/auth/auth_checker_factory.hpp>
#include <userver/components/component_context.hpp>
#include <jwt-cpp/jwt.h>
#include <memory>

namespace ya_brander::auth {

class AuthCheckerJwt final : public userver::server::handlers::auth::AuthCheckerBase {
public:
    using AuthCheckResult = userver::server::handlers::auth::AuthCheckResult;

    explicit AuthCheckerJwt(
        std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier
    );

    [[nodiscard]] AuthCheckResult CheckAuth(
        const userver::server::http::HttpRequest& request,
        userver::server::request::RequestContext& request_context
    ) const override;

    [[nodiscard]] bool SupportsUserAuth() const noexcept override { return true; }

private:
    std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier_;
};

class CheckerFactory final : public userver::server::handlers::auth::AuthCheckerFactoryBase {
public:
    static constexpr std::string_view kAuthType = "jwt";

    explicit CheckerFactory(const userver::components::ComponentContext& context);

    [[nodiscard]] userver::server::handlers::auth::AuthCheckerBasePtr MakeAuthChecker(
        const userver::server::handlers::auth::HandlerAuthConfig& auth_config
    ) const override;

private:
    std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier_;
};

}  // namespace ya_brander::auth
