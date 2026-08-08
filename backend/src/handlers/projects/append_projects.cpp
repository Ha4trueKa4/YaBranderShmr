#include "append_projects.hpp"

#include "../../components/project_manager_component.hpp"

#include "create_project.hpp"
#include "get_project.hpp"
#include "patch_project.hpp"
#include "delete_project.hpp"
#include "generate_project.hpp"

namespace ya_brander {

void AppendProjects(userver::components::ComponentList& component_list) {
  component_list
      // 1. Регистрируем зависимости менеджера проектов (LLM, S3, RAG).
      // ВАЖНО: Здесь должны передаваться конкретные реализации компонентов (например, моки),
      // так как userver не умеет инстанцировать чисто абстрактные классы-интерфейсы.
      // .Append<components::MockVersionStorageComponent>()
      // .Append<components::MockLLMClientComponent>()
      // .Append<components::MockPromptConstructorComponent>()

      .Append<components::ProjectManagerComponent>()
      .Append<handlers::CreateProjectHandler>()
      .Append<handlers::GetProjectHandler>()
      .Append<handlers::PatchProjectHandler>()
      .Append<handlers::DeleteProjectHandler>()
      .Append<handlers::GenerateProjectHandler>();
}

} // namespace ya_brander