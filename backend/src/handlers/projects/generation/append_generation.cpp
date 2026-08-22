#include "append_generation.hpp"

#include "cancel_generation.hpp"
#include "generate_project.h"
#include "generate_status.hpp"

namespace ya_brander::handlers {

void AppendProjectGeneration(userver::components::ComponentList& component_list) {
  component_list
      .Append<GenerateProjectHandler>()
      .Append<GenerateStatusHandler>()
      .Append<CancelGenerationHandler>();
}

}  // namespace ya_brander::handlers
