#include "append_projects.h"

#include "../../components/project_manager_component.h"
#include "../../components/version_storage/version_storage_component_impl.hpp"

#include "create_project.h"
#include "get_project.h"
#include "patch_project.h"
#include "delete_project.h"
#include "generate_project.h"
#include "versions/get_version.hpp"
#include "versions/list_versions.hpp"
#include "versions/restore_version.hpp"

namespace ya_brander {

void AppendProjects(userver::components::ComponentList& component_list) {
  component_list
      // .Append<components::VersionStorageComponentImpl>()
      .Append<components::LLMClientComponent>()
      .Append<components::VectorSearchComponent>() 
      .Append<components::PromptConstructorComponent>()
      //.Append<components::MockVectorSearchComponent>()


      .Append<components::ProjectManagerComponent>()


      .Append<handlers::CreateProjectHandler>()
      .Append<handlers::GetProjectHandler>()
      .Append<handlers::PatchProjectHandler>()
      .Append<handlers::DeleteProjectHandler>()
      .Append<handlers::GenerateProjectHandler>()
      .Append<handlers::GetVersionHandler>()
      .Append<handlers::ListVersionsHandler>()
      .Append<handlers::RestoreVersionHandler>();
}

} // namespace ya_brander
