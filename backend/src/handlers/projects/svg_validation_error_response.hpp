#pragma once

#include <string>
#include <string_view>
#include <utility>

#include <userver/formats/json/serialize.hpp>
#include <userver/formats/json/value_builder.hpp>

#include "../../components/svg_validator.hpp"

namespace ya_brander::handlers {

inline std::string BuildSvgValidationErrorResponse(
    const components::SvgValidationError& error, std::string_view message) {
  userver::formats::json::ValueBuilder errors{
      userver::formats::json::Type::kArray};
  for (const auto& validation_error : error.Errors()) {
    errors.PushBack(validation_error);
  }

  userver::formats::json::ValueBuilder response;
  response["message"] = std::string(message);
  response["errors"] = std::move(errors);
  return userver::formats::json::ToString(response.ExtractValue());
}

}  // namespace ya_brander::handlers
