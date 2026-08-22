#include "append_projects.h"

#include "../../components/project_manager_component.h"
#include "../../components/version_storage/version_storage_component_impl.hpp"

#include "create_project.h"
#include "get_project.h"
#include "get_project_chat.h"
#include "patch_project.h"
#include "delete_project.h"

namespace ya_brander {

// Регистрирует общую для домена "проекты" инфраструктуру (компоненты) и
// core CRUD-хендлеры. Хендлеры версий и генерации регистрируются отдельно —
// см. AppendProjectVersions и AppendProjectGeneration, вызываемые из main.cpp.
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
      .Append<handlers::DeleteProjectHandler>();
}

} // namespace ya_brander
