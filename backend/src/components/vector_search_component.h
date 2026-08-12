#pragma once

#include <string>
#include <userver/components/loggable_component_base.hpp>

namespace ya_brander::components {

class VectorSearchComponent : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "vector-search";

  VectorSearchComponent(const userver::components::ComponentConfig& config,
                        const userver::components::ComponentContext& context)
      : LoggableComponentBase(config, context) {}

  virtual std::vector<std::string> FindSimilar(const std::string& description, int limit) const = 0;
};

} // namespace ya_brander::components