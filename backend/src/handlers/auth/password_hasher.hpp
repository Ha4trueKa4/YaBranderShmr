#pragma once
#include <string>

namespace ya_brander::auth {

class PasswordHasher {
public:
    static std::string Hash(const std::string& password);
    static bool Verify(const std::string& password, const std::string& stored_hash);
    // 8-128 символов, обязательно хотя бы одна буква и одна цифра.
    static bool IsValidPassword(const std::string& password);

private:
    static constexpr int kIterations = 100000;
    static constexpr int kSaltBytes = 16;
    static constexpr int kHashBytes = 32;
};

}  // namespace ya_brander::auth
