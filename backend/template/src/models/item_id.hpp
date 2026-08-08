#pragma once

#include <string>
#include <variant>
#include <optional>

namespace code_architecture {

struct ItemId {
    std::string type;  
    int id;
};

enum class ParseError {
    WRONG_CATEGORY,
    INCORRECT_ITEM_ID
};


std::variant<ItemId, ParseError> ParseItemId(const std::string& raw);

}  // namespace code_architecture