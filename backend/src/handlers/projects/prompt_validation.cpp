#include "prompt_validation.h"

#include <algorithm>

namespace ya_brander::handlers {

std::size_t Utf8Length(std::string_view value) {
  return static_cast<std::size_t>(
      std::count_if(value.begin(), value.end(), [](const char character) {
        return (static_cast<unsigned char>(character) & 0xC0U) != 0x80U;
      }));
}

}  // namespace ya_brander::handlers
