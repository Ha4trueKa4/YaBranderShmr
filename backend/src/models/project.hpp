#pragma once
#include <string>
#include <userver/formats/json/value.hpp>

namespace brandbook::models {

struct Project {
  std::string id;
  std::string author_id;
  std::string name;
  std::string description;
  bool is_public;
  std::string created_at;
  userver::formats::json::Value data;
};

} // namespace brandbook::models