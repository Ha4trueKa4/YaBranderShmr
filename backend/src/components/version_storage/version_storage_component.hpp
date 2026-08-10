#pragma once
#include <string>
#include <vector>
#include <userver/components/loggable_component_base.hpp>
#include "models/version.hpp"

namespace ya_brander::components {

/// @brief Интерфейс хранилища версий проектов.
/// @details Определяет контракт для сохранения, чтения, перечисления и
///          восстановления версий. Данные версии передаются как строка (SVG).
///          Метаинформация хранится в таблице project_versions, сами данные
///          в объектном хранилище (S3), и последняя версия в таблице projects.
///          Конкретная реализация регистрируется в списке компонентов.
class VersionStorageComponent : public userver::components::LoggableComponentBase {
 public:
  static constexpr std::string_view kName = "version-storage";

  VersionStorageComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context)
      : LoggableComponentBase(config, context) {}

  ~VersionStorageComponent() override = default;

  /// @brief Сохраняет новую версию проекта.
  /// @param project_id Идентификатор проекта.
  /// @param data Данные версии в виде строки (SVG).
  /// @return Идентификатор созданной версии.
  virtual std::string SaveVersion(const std::string& project_id,
                                  const std::string& data) = 0;

  /// @brief Возвращает данные конкретной версии проекта.
  /// @param project_id Идентификатор проекта.
  /// @param version_id Идентификатор версии.
  /// @return Данные версии в виде строки (SVG) или пустая строка.
  virtual std::string GetVersion(const std::string& project_id,
                                 const std::string& version_id) = 0;

  /// @brief Возвращает список всех версий проекта.
  /// @param project_id Идентификатор проекта.
  /// @return Список метаданных версий, упорядоченный по номеру версии.
  virtual std::vector<models::Version> ListVersions(
      const std::string& project_id) = 0;

  /// @brief Восстанавливает состояние проекта из указанной версии.
  /// @details Данные выбранной версии сохраняются как новая версия, история
  ///          изменений остается неизменной.
  /// @param project_id Идентификатор проекта.
  /// @param version_id Идентификатор восстанавливаемой версии.
  /// @return Идентификатор новой версии, созданной при восстановлении.
  virtual std::string RestoreVersion(const std::string& project_id,
                                     const std::string& version_id) = 0;

  /// @brief Возвращает данные последней версии проекта.
  /// @param project_id Идентификатор проекта.
  /// @return Данные последней версии в виде строки (SVG) или пустая строка.
  virtual std::string GetLatestVersionData(const std::string& project_id) = 0;
};

}  // namespace ya_brander::components