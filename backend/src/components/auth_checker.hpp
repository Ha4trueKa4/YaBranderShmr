#pragma once

#include <userver/server/handlers/auth/auth_checker_base.hpp>
#include <userver/server/handlers/auth/auth_checker_factory.hpp>
#include <userver/components/component_context.hpp>
#include <jwt-cpp/jwt.h>
#include <memory>

namespace ya_brander::auth {

// Пропускает запрос без Authorization дальше анонимным (user_id просто не
// попадёт в RequestContext) либо в hard-режиме (require_auth) сразу отдаёт
// kUnauthorized, не доходя до хендлера.
class AuthCheckerJwt final : public userver::server::handlers::auth::AuthCheckerBase {
public:
    using AuthCheckResult = userver::server::handlers::auth::AuthCheckResult;

    AuthCheckerJwt(
        std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier,
        bool require_auth
    );

    [[nodiscard]] AuthCheckResult CheckAuth(
        const userver::server::http::HttpRequest& request,
        userver::server::request::RequestContext& request_context
    ) const override;

    [[nodiscard]] bool SupportsUserAuth() const noexcept override { return true; }

private:
    std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier_;
    bool require_auth_;
};

// Общая часть для обеих фабрик: читает JWT_SECRET_KEY и строит verifier.
class JwtCheckerFactoryBase : public userver::server::handlers::auth::AuthCheckerFactoryBase {
public:
    explicit JwtCheckerFactoryBase(const userver::components::ComponentContext& context);

protected:
    std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier_;
};

// "jwt" — soft: анонимные запросы пропускаются, user_id опционален.
class CheckerFactory final : public JwtCheckerFactoryBase {
public:
    static constexpr std::string_view kAuthType = "jwt";

    using JwtCheckerFactoryBase::JwtCheckerFactoryBase;

    [[nodiscard]] userver::server::handlers::auth::AuthCheckerBasePtr MakeAuthChecker(
        const userver::server::handlers::auth::HandlerAuthConfig& auth_config
    ) const override;
};

// "jwt-required" — hard: без валидного токена запрос отклоняется до хендлера,
// хендлеру гарантирован user_id в RequestContext.
class CheckerFactoryRequired final : public JwtCheckerFactoryBase {
public:
    static constexpr std::string_view kAuthType = "jwt-required";

    using JwtCheckerFactoryBase::JwtCheckerFactoryBase;

    [[nodiscard]] userver::server::handlers::auth::AuthCheckerBasePtr MakeAuthChecker(
        const userver::server::handlers::auth::HandlerAuthConfig& auth_config
    ) const override;
};

}  // namespace ya_brander::auth
