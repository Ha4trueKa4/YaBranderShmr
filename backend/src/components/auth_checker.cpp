#include "auth_checker.hpp"

#include <userver/server/handlers/exceptions.hpp>
#include <userver/server/http/http_request.hpp>
#include <userver/http/common_headers.hpp>
#include <jwt-cpp/jwt.h>
#include <cstdlib>
#include <stdexcept>

namespace ya_brender::auth {

namespace {
    std::optional<std::string> ParseUserIdFromJwt(
        const std::string& token,
        const std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>>& verifier
    ) {
        try {
            auto decoded = jwt::decode(token);
            verifier->verify(decoded);
            
            if (decoded.has_payload_claim("user_id")) {
                auto claim = decoded.get_payload_claim("user_id");
                return claim.as_string();
            }
            return std::nullopt;
        } catch (const std::exception&) {
            return std::nullopt;
        }
    }

    std::optional<std::string> ExtractToken(const userver::server::http::HttpRequest& request) {
        const auto& auth_value = request.GetHeader(userver::http::headers::kAuthorization);
        if (auth_value.empty()) {
            return std::nullopt;
        }

        const auto bearer_sep_pos = auth_value.find(' ');
        if (bearer_sep_pos == std::string::npos || 
            std::string_view{auth_value.data(), bearer_sep_pos} != "Bearer") {
            return std::nullopt;
        }

        return std::string(auth_value.data() + bearer_sep_pos + 1);
    }
}

AuthCheckerJwt::AuthCheckerJwt(
    std::shared_ptr<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>> verifier
)
    : verifier_(std::move(verifier)) {}

AuthCheckerJwt::AuthCheckResult AuthCheckerJwt::CheckAuth(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext& request_context
) const {
    const auto& auth_value = request.GetHeader(userver::http::headers::kAuthorization);
    if (auth_value.empty()) {
        return {};
    }

    const auto token = ExtractToken(request);
    if (!token.has_value()) {
        return AuthCheckResult{
            AuthCheckResult::Status::kTokenNotFound,
            {},
            "Invalid Authorization header format. Expected: Bearer <token>",
            userver::server::handlers::HandlerErrorCode::kUnauthorized
        };
    }

    auto user_id_opt = ParseUserIdFromJwt(*token, verifier_);
    if (!user_id_opt.has_value()) {
        return AuthCheckResult{
            AuthCheckResult::Status::kForbidden,
            {},
            "Invalid or expired JWT token",
            userver::server::handlers::HandlerErrorCode::kForbidden
        };
    }
    request_context.SetData("user_id", user_id_opt.value());
    return {};
}

// Фабрика
CheckerFactory::CheckerFactory(const userver::components::ComponentContext& /*context*/) {
    const char* jwt_secret = std::getenv("JWT_SECRET_KEY");
    
    if (!jwt_secret || std::strlen(jwt_secret) == 0) {
        throw std::runtime_error("JWT_SECRET_KEY environment variable is not set!");
    }

    verifier_ = std::make_shared<jwt::verifier<jwt::default_clock, jwt::traits::kazuho_picojson>>(
        jwt::verify()
            .allow_algorithm(jwt::algorithm::hs256{std::string(jwt_secret)})
    );
}

userver::server::handlers::auth::AuthCheckerBasePtr CheckerFactory::MakeAuthChecker(
    const userver::server::handlers::auth::HandlerAuthConfig& /*auth_config*/
) const {
    return std::make_shared<AuthCheckerJwt>(verifier_);
}

}  // namespace ya_brender::auth
