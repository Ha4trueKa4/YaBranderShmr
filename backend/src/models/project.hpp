#pragma once
#include <string>
#include <chrono>
#include <userver/formats/json/value.hpp>

namespace ya_brander::models {

struct Project {
  std::string id;
  std::string author_id;
  std::string name;
  std::string description;
  bool is_public;
  std::chrono::system_clock::time_point created_at;
  std::string data;
};

/// @brief Результат генерации/правки SVG через LLM.
struct GenerationResult {
  std::string version_id;
  std::string data;
};

} // namespace ya_brander::models