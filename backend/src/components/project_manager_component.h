#pragma once
#include <optional>
#include <string>
#include <vector>
#include <userver/components/component_context.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/storages/postgres/cluster.hpp>
#include <userver/formats/json/value.hpp>

#include "../models/project.hpp"
#include "dependencies_stubs.h"
#include "LLMClientComponent.hpp"
#include "version_storage//version_storage_component.hpp"
#include "prompt_constructor/prompt_constructor_component.h"

namespace ya_brander::components {

class ProjectManagerComponent final : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "project-manager";

  ProjectManagerComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context);

  models::Project CreateProject(const std::string& author_id, const std::string& name, const std::string& description);

  std::optional<models::Project> GetProject(const std::string& id);

  void UpdateProject(const std::string& id, std::optional<std::string> name,
                     std::optional<bool> is_public, std::string data);

  void DeleteProject(const std::string& id);

  std::string GenerateAI(const std::string& project_id, const std::string& prompt, const std::string& model,
                         const std::string& caller_id,
                         const std::vector<std::string>& reference_project_ids = {});

  std::vector<models::Version> ListVersions(const std::string& project_id);

  std::string GetVersionUrl(const std::string& project_id,
                          const std::string& version_id);

  std::string RestoreVersion(const std::string& project_id,
                           const std::string& version_id);

  std::string GetLatestVersionUrl(const std::string& project_id);

  /// @brief Возвращает историю чата проекта как есть (JSON-массив).
  /// @details Каждый элемент — либо сообщение пользователя ({"role":"user",
  ///          "text":...}), либо ответ модели, представленный ссылкой на
  ///          версию ({"role":"assistant","version_id":...}), а не самим SVG
  ///          и не presigned-ссылкой (та имеет срок годности) — резолвинг
  ///          version_id в ссылку живёт в отдельном (не этом) эндпоинте.
  userver::formats::json::Value GetChatHistory(const std::string& project_id);

private:
  /// @brief Сырой project.chat_history одним запросом (jsonb напрямую, без
  ///        ::text каста и повторного парсинга) — общий код для
  ///        GetChatHistory и GetRecentUserPrompts.
  userver::formats::json::Value FetchChatHistoryJson(const std::string& project_id);

  /// @brief Последние limit текстов запросов пользователя (без ответов
  ///        модели), от старых к новым — контекст для промпта LLM.
  std::vector<std::string> GetRecentUserPrompts(const std::string& project_id, std::size_t limit);

  /// @brief Атомарно добавляет пару сообщений (запрос пользователя + ссылку
  ///        на версию-ответ) в конец project.chat_history.
  void AppendChatExchange(const std::string& project_id, const std::string& user_message,
                          const std::string& version_id);

  /// @brief Резолвит id референс-проектов (не более 2, лишние игнорируются
  ///        вызывающим на уровне хендлера) в содержимое их последних SVG для
  ///        промпта первичной генерации.
  /// @details Молча пропускает (не бросает исключений) id, которые: не
  ///          существуют; существуют, но приватны и не принадлежат caller_id;
  ///          чья последняя версия пуста или равна kDefaultSvg (сюда же
  ///          попадает самоссылка на генерируемый проект — в ветке первичной
  ///          генерации его собственный current_state и так пуст/дефолтен).
  std::vector<std::string> ResolveReferenceSvgs(
      const std::string& caller_id,
      const std::vector<std::string>& reference_project_ids);

  userver::storages::postgres::ClusterPtr pg_cluster_;
  VersionStorageComponent& version_storage_;
  LLMClientComponent& llm_client_;
  PromptConstructorComponent& prompt_constructor_;
};

} // namespace ya_brander::components
