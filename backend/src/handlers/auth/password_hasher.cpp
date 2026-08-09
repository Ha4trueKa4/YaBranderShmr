#include "password_hasher.hpp"

#include <openssl/evp.h>
#include <openssl/rand.h>

#include <iomanip>
#include <sstream>
#include <stdexcept>
#include <vector>

namespace ya_brander::auth {
namespace {

std::string ToHex(const unsigned char* data, size_t len) {
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (size_t i = 0; i < len; ++i) {
        oss << std::setw(2) << static_cast<int>(data[i]);
    }
    return oss.str();
}

std::vector<unsigned char> FromHex(const std::string& hex) {
    std::vector<unsigned char> result(hex.size() / 2);
    for (size_t i = 0; i < result.size(); ++i) {
        result[i] = static_cast<unsigned char>(std::stoi(hex.substr(i * 2, 2), nullptr, 16));
    }
    return result;
}

std::vector<std::string> Split(const std::string& s, char delim) {
    std::vector<std::string> parts;
    std::string cur;
    for (char c : s) {
        if (c == delim) {
            parts.push_back(cur);
            cur.clear();
        } else {
            cur.push_back(c);
        }
    }
    parts.push_back(cur);
    return parts;
}

}  // namespace

std::string PasswordHasher::Hash(const std::string& password) {
    unsigned char salt[kSaltBytes];
    if (RAND_bytes(salt, kSaltBytes) != 1) {
        throw std::runtime_error("Failed to generate salt");
    }

    unsigned char hash[kHashBytes];
    if (PKCS5_PBKDF2_HMAC(password.c_str(), static_cast<int>(password.size()), salt, kSaltBytes,
                           kIterations, EVP_sha256(), kHashBytes, hash) != 1) {
        throw std::runtime_error("Failed to hash password");
    }

    std::ostringstream oss;
    oss << "pbkdf2$" << kIterations << "$" << ToHex(salt, kSaltBytes) << "$"
        << ToHex(hash, kHashBytes);
    return oss.str();
}

bool PasswordHasher::Verify(const std::string& password, const std::string& stored_hash) {
    const auto parts = Split(stored_hash, '$');
    if (parts.size() != 4 || parts[0] != "pbkdf2") {
        return false;
    }

    int iterations = 0;
    try {
        iterations = std::stoi(parts[1]);
    } catch (const std::exception&) {
        return false;
    }

    const auto salt = FromHex(parts[2]);
    const auto expected_hash = FromHex(parts[3]);

    std::vector<unsigned char> actual_hash(expected_hash.size());
    if (PKCS5_PBKDF2_HMAC(password.c_str(), static_cast<int>(password.size()), salt.data(),
                           static_cast<int>(salt.size()), iterations, EVP_sha256(),
                           static_cast<int>(actual_hash.size()), actual_hash.data()) != 1) {
        return false;
    }

    if (actual_hash.size() != expected_hash.size()) return false;

    // Сравнение за константное время, чтобы не утекала инфа по таймингу
    unsigned char diff = 0;
    for (size_t i = 0; i < actual_hash.size(); ++i) {
        diff |= actual_hash[i] ^ expected_hash[i];
    }
    return diff == 0;
}

}  // namespace ya_brander::auth
