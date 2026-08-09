#pragma once
#include <string>

namespace ya_brender::auth {

class PasswordHasher {
public:
    static std::string Hash(const std::string& password);
    static bool Verify(const std::string& password, const std::string& stored_hash);

private:
    static constexpr int kIterations = 100000;
    static constexpr int kSaltBytes = 16;
    static constexpr int kHashBytes = 32;
};

}  // namespace ya_brender::auth
