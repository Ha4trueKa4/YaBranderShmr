#pragma once

#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

namespace ya_brander::components {

class SvgValidationError final : public std::runtime_error {
 public:
  explicit SvgValidationError(std::vector<std::string> errors);

  const std::vector<std::string>& Errors() const noexcept;

 private:
  std::vector<std::string> errors_;
};

std::vector<std::string> ValidateSvg(std::string_view svg);
void ValidateSvgOrThrow(std::string_view svg);

}  // namespace ya_brander::components
