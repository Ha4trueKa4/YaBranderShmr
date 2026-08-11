#pragma once
#include <string>
#include <userver/formats/json/value.hpp>

namespace ya_brander::models {

struct Project {
  std::string id;
  std::string author_id;
  std::string name;
  std::string description;
  bool is_public;
  std::string created_at;
  std::string data;
};

} // namespace ya_brander::models