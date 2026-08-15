#pragma once
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>
#include <userver/components/loggable_component_base.hpp>
#include "../../models/version.hpp"

namespace ya_brander::components {

/// @brief Интерфейс хранилища версий проектов.
/// @details Определяет контракт для сохранения, чтения, перечисления и
///          восстановления версий. Данные версии передаются как строка (SVG).
///          Метаинформация хранится в таблице project_versions, сами данные —
///          в объектном хранилище (S3), ключ последней версии — в таблице
///          projects (last_version_key). Это единственный слой, обращающийся к
///          ObjectStorageComponent; остальные части сервиса работают только
///          через этот интерфейс и никогда не видят ключи/раскладку S3.
///          Конкретная реализация регистрируется под именем kName.
///
///          Соглашения контракта:
///          - version_id — это Version::id (UUID), а НЕ version_num.
///          - Методы чтения возвращают пустую строку / пустой список, если
///            сущности нет (проект/версия отсутствует, объект не найден).
///          - Любой метод может бросить исключение при сбое инфраструктуры
///            (БД или объектное хранилище). Кроме DeleteAllVersions:
///            он best-effort и не пробрасывает ошибки хранилища наружу.
///          - Методы в реализации можно будет вызывать конкурентно
///            из разных корутин.
class VersionStorageComponent : public userver::components::LoggableComponentBase {
 public:
    static constexpr std::string_view kName = "version-storage";

    VersionStorageComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context)
      : LoggableComponentBase(config, context) {}

    ~VersionStorageComponent() override = default;

    /// @brief Сохраняет новую версию проекта.
    /// @details Создаёт новый объект в хранилище и переводит last_version_key на
    ///          него. ACL нового объекта выставляется по видимости проекта; по
    ///          инварианту предыдущая версия при этом становится private.
    /// @param project_id Идентификатор проекта.
    /// @param data Данные версии в виде строки (SVG).
    /// @return Идентификатор (UUID) созданной версии.
    virtual std::string SaveVersion(const std::string& project_id,
                                  const std::string& data) = 0;

    /// @brief Возвращает данные конкретной версии проекта (байты).
    /// @details Серверный доступ к содержимому (например, восстановление).
    ///          Для прямой отдачи фронтенду используйте GetVersionUrl.
    /// @param project_id Идентификатор проекта.
    /// @param version_id Идентификатор версии (Version::id, UUID).
    /// @return Данные версии в виде строки (SVG) или пустая строка.
    virtual std::string GetVersion(const std::string& project_id,
                                 const std::string& version_id) = 0;

    /// @brief Возвращает список всех версий проекта.
    /// @param project_id Идентификатор проекта.
    /// @return Список метаданных версий, упорядоченный по version_num (DESC).
    virtual std::vector<models::Version> ListVersions(
      const std::string& project_id) = 0;

    /// @brief Восстанавливает состояние проекта из указанной версии.
    /// @details Данные выбранной версии сохраняются как новая версия, история
    ///          изменений остаётся неизменной. Новая версия получает ACL по
    ///          текущей видимости проекта.
    /// @param project_id Идентификатор проекта.
    /// @param version_id Идентификатор восстанавливаемой версии (UUID).
    /// @return Идентификатор новой версии, созданной при восстановлении.
    virtual std::string RestoreVersion(const std::string& project_id,
                                     const std::string& version_id) = 0;

    /// @brief Возвращает данные последней версии проекта (байты).
    /// @details Серверный путь: построение промпта редактирования для LLM.
    ///          Для отдачи фронтенду используйте GetLatestVersionUrl.
    /// @param project_id Идентификатор проекта.
    /// @return Данные последней версии в виде строки (SVG) или пустая строка.
    virtual std::string GetLatestVersionData(const std::string& project_id) = 0;

    /// @brief Возвращает ссылку на объект последней версии для фронтенда.
    /// @details Тип ссылки определяется видимостью проекта, которую метод читает
    ///          сам из projects (is_private) — вызывающему НЕ нужно её передавать.
    ///          Публичный проект -> постоянная прямая ссылка;
    ///          приватный -> временная presigned-ссылка.
    ///          Байты через бэкенд не тянет.
    /// @param project_id Идентификатор проекта.
    /// @return URL или пустая строка, если версий ещё нет.
    virtual std::string GetLatestVersionUrl(const std::string& project_id) = 0;

    /// @brief Возвращает ссылку на объект КОНКРЕТНОЙ версии для фронтенда.
    /// @details Любая отдельная версия отдаётся временной (presigned) ссылкой:
    ///          история приватна по дефолту, а presigned работает и в случае,
    ///          если это текущая публичная версия. Для последней версии удобнее
    ///          GetLatestVersionUrl.
    /// @param project_id Идентификатор проекта.
    /// @param version_id Идентификатор версии (UUID).
    /// @return URL или пустая строка, если версия не найдена.
    virtual std::string GetVersionUrl(const std::string& project_id,
                                      const std::string& version_id) = 0;

    /// @brief Батч-конструирование ссылок по уже готовым ключам объектов.
    /// @details НЕ обращается к БД: ключи и видимость передаёт вызывающий (обычно
    ///          он уже получил last_version_key при выборке ленты/профиля). Тип
    ///          ссылки один на весь вызов, поэтому вызывающий группирует ключи по
    ///          видимости и делает не более двух вызовов: один для публичных
    ///          (is_public=true), один для приватных (is_public=false).
    ///          ВНИМАНИЕ: is_public обязан совпадать с реальной видимостью объекта;
    ///          неверное значение даст нерабочую ссылку (для публичного ключа с
    ///          is_public=false будет presigned, что валидно; для приватного ключа
    ///          с is_public=true будет постоянная ссылка -> 403). Разделение
    ///          по видимости это ответственность вызывающего этот метод.
    /// @param keys Ключи объектов.
    /// @param is_public true -> постоянные прямые ссылки, false -> presigned.
    /// @return Отображение key -> URL.
    virtual std::unordered_map<std::string, std::string> GetManyUrls(
        const std::vector<std::string>& keys, bool is_public) = 0;

    /// @brief Устанавливает ACL объекта текущей версии проекта.
    /// @details МЕХАНИЗМ смены видимости, НЕ самостоятельная операция. Меняет ACL
    ///          ровно одного объекта (last_version_key). ОБЯЗАН вызываться в паре
    ///          с обновлением projects.is_private и в правильном порядке (расширять
    ///          доступ до попадания в ленту, сужать — после выхода из ленты); иначе
    ///          инвариант «публичен только текущий объект» рассинхронизируется.
    ///          Штатный вызывающий это поток смены видимости проекта. Если у проекта
    ///          нет версий (last_version_key IS NULL) — no-op.
    /// @param project_id Идентификатор проекта.
    /// @param make_public true -> public-read, false -> private.
    virtual void SetLatestAcl(const std::string& project_id, bool make_public) = 0;

    /// @brief Удаляет объекты всех версий проекта из хранилища (best-effort).
    /// @details Вызывается при удалении проекта, ДО удаления строк проекта (пока
    ///          project_versions ещё содержит ключи). Сами строки удаляются
    ///          каскадом при DELETE FROM projects. Ошибки удаления объектов из S3
    ///          не фатальны и не бросаются наружу, их подберёт фоновый orphan-GC
    ///          когда будет готов.
    virtual void DeleteAllVersions(const std::string& project_id) = 0;
};

}  // namespace ya_brander::components
