#include "append_versions.hpp"

#include "get_version.hpp"
#include "list_versions.hpp"
#include "restore_version.hpp"

namespace ya_brander::handlers {

void AppendProjectVersions(userver::components::ComponentList& component_list) {
  component_list
      .Append<GetVersionHandler>()
      .Append<ListVersionsHandler>()
      .Append<RestoreVersionHandler>();
}

}  // namespace ya_brander::handlers
