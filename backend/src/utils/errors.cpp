#include "errors.hpp"

#include <userver/formats/json/value_builder.hpp>

namespace ya_brander::utils {

userver::formats::json::Value MakeError(std::string_view code, std::string_view message, std::string_view field) {
  userver::formats::json::ValueBuilder error;
  error["code"] = std::string(code);
  error["message"] = std::string(message);
  if (!field.empty()) {
    error["field"] = std::string(field);
  }
  return error.ExtractValue();
}

userver::formats::json::Value MakeMessage(std::string_view message) {
  userver::formats::json::ValueBuilder builder;
  builder["message"] = std::string(message);
  return builder.ExtractValue();
}

}  // namespace ya_brander::utils
