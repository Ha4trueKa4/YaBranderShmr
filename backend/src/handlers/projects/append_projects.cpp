#include "append_projects.h"

#include "../../components/project_manager_component.h"
#include "../../components/mock_dependencies.h"

#include "create_project.h"
#include "get_project.h"
#include "patch_project.h"
#include "delete_project.h"
#include "generate_project.h"

namespace ya_brander {

void AppendProjects(userver::components::ComponentList& component_list) {
  component_list
      .Append<components::MockVersionStorageComponent>()
      .Append<components::LLMClientComponent>()
      .Append<components::MockPromptConstructorComponent>()


      .Append<components::ProjectManagerComponent>()


      .Append<handlers::CreateProjectHandler>()
      .Append<handlers::GetProjectHandler>()
      .Append<handlers::PatchProjectHandler>()
      .Append<handlers::DeleteProjectHandler>()
      .Append<handlers::GenerateProjectHandler>();
}

} // namespace ya_brander