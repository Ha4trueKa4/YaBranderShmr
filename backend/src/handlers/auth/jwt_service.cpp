#include "jwt_service.hpp"

#include <jwt-cpp/jwt.h>

#include <cstdlib>
#include <cstring>
#include <stdexcept>

namespace ya_brander::auth {

JwtService::JwtService() {
    const char* jwt_secret = std::getenv("JWT_SECRET_KEY");
    if (!jwt_secret || std::strlen(jwt_secret) == 0) {
        throw std::runtime_error("JWT_SECRET_KEY environment variable is not set!");
    }
    secret_ = jwt_secret;
}

std::string JwtService::IssueToken(const std::string& user_id, std::chrono::seconds ttl) const {
    const auto now = std::chrono::system_clock::now();
    return jwt::create()
        .set_type("JWT")
        .set_issued_at(now)
        .set_expires_at(now + ttl)
        .set_payload_claim("user_id", jwt::claim(user_id))
        .sign(jwt::algorithm::hs256{secret_});
}

}  // namespace ya_brander::auth
