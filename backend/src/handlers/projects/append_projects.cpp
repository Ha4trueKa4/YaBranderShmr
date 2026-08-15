#include "append_projects.h"

#include "../../components/project_manager_component.h"
#include "../../components/mock_dependencies.h"
#include "../../components/version_storage/version_storage_component_impl.hpp"

#include "create_project.h"
#include "get_project.h"
#include "get_project_chat.h"
#include "patch_project.h"
#include "delete_project.h"
#include "generate_project.h"

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
      .Append<handlers::GetProjectChatHandler>()
      .Append<handlers::PatchProjectHandler>()
      .Append<handlers::DeleteProjectHandler>()
      .Append<handlers::GenerateProjectHandler>();
}

} // namespace ya_brander