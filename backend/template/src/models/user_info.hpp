#pragma once
#include <optional>
#include <string>

namespace code_architecture {

enum UserKind {
    Regular,
    Doctor
};

struct UserInfo {
    int id;                         
    UserKind kind;
    std::optional<int> specialty_id;
};

}  // namespace code_architecture