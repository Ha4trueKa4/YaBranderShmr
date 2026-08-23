#pragma once

#include <cstddef>
#include <string_view>

namespace ya_brander::handlers {

// Общие для CreateProjectHandler (первичная генерация) и GenerateProjectHandler
// (правка) лимиты запроса на генерацию — вынесены сюда, чтобы не разъезжались
// между двумя хендлерами.
constexpr std::size_t kMaxPromptLength = 5000;
constexpr std::size_t kMaxReferences = 2;

// Длина строки в UTF-8 символах (не байтах) — для валидации kMaxPromptLength.
std::size_t Utf8Length(std::string_view value);

}  // namespace ya_brander::handlers
