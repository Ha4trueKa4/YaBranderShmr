#pragma once
#include <chrono>
#include <string>

namespace ya_brander::auth {

class JwtService {
public:
    JwtService();

    std::string IssueToken(const std::string& user_id,
                            std::chrono::seconds ttl = std::chrono::hours(720)) const;
    

private:
    std::string secret_;
};

}  // namespace ya_brander::auth
