#include "item_id.hpp"
#include <charconv>
#include <cctype>
#include <system_error>

namespace code_architecture {

std::variant<ItemId, ParseError> ParseItemId(const std::string& raw) {
    auto pos = raw.find('_');
    if (pos == std::string::npos || pos == 0 || pos == raw.size() - 1) {
        return ParseError::WRONG_CATEGORY;
    }

    std::string type = raw.substr(0, pos);
    for (char& c : type) {
        c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    }

    if (type != "common" && type != "special" && type != "prescription") {
        return ParseError::WRONG_CATEGORY;
    }

    std::string id_str = raw.substr(pos + 1);
    int id = 0;
    auto [ptr, ec] = std::from_chars(id_str.data(), id_str.data() + id_str.size(), id);
    if (ec != std::errc{}) {
        return ParseError::INCORRECT_ITEM_ID;
    }

    return ItemId{std::move(type), id};
}

}  // namespace code_architecture