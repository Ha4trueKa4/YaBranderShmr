#pragma once

#include <string_view>

#include <userver/formats/json/value.hpp>

namespace ya_brander::utils {

// {"code": ..., "message": ..., "field": ...} — field опускается, если пустой.
userver::formats::json::Value MakeError(std::string_view code, std::string_view message, std::string_view field = {});

// {"message": ...} — для ответов без кода ошибки.
userver::formats::json::Value MakeMessage(std::string_view message);

}  // namespace ya_brander::utils
