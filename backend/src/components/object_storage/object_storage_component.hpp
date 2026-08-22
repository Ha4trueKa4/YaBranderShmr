#pragma once
#include <cstddef>
#include <string>
#include <string_view>
#include <vector>
#include <chrono>
#include <userver/components/loggable_component_base.hpp>


namespace ya_brander::components {


/// @brief Ключ объекта в хранилище вместе с временем последней модификации.
/// last_modified нужен orphan-GC: свежие объекты (только что загруженные, но чья
/// строка ещё не закоммичена) нельзя считать мусором.
struct ObjectKey {
    std::string key;
    std::chrono::system_clock::time_point last_modified;
};

/// @brief Интерфейс объектного хранилища (blob storage).
/// @details Хранит и отдаёт произвольные данные по ключу. Компонент не знает о
///          версиях, проектах или БД, только байты по ключу и ACL объекта.
///          Единственная реализация сейчас это S3ObjectStorageComponent, но
///          интерфейс сохранён, чтобы вызывающий код зависел от абстракции
///          (тесты подменяют фейком, бэкенд можно поменять без правок вызовов).
class ObjectStorageComponent : public userver::components::LoggableComponentBase {
public:
    static constexpr std::string_view kName = "object-storage";

    ObjectStorageComponent(const userver::components::ComponentConfig& config,
                           const userver::components::ComponentContext& context)
        : LoggableComponentBase(config, context) {}

    virtual ~ObjectStorageComponent() = default;

    /// @brief Сохраняет данные под ключом и сразу задаёт ACL объекта.
    /// @param make_public true -> public-read (объект публичного проекта),
    ///        false -> private. ACL ставится в момент загрузки, отдельного
    ///        запроса не нужно.
    virtual void Put(const std::string& key, const std::string& data,
                     bool make_public) = 0;

    /// @brief Возвращает данные по ключу.
    /// @return Данные или пустая строка, если объекта нет.
    /// @details Серверный доступ к байтам: промпт редактирования для LLM,
    ///          восстановление версии. Для отдачи фронтенду используйте GetUrl.
    virtual std::string Get(const std::string& key) = 0;

    /// @brief Ссылка на объект для фронтенда (для чтения/превью).
    /// @param is_public видимость проекта.
    /// @details Публичный проект -> постоянная прямая ссылка (объект public-read,
    ///          кешируется, дружелюбна к будущему CDN). Приватный проект ->
    ///          временная (presigned) ссылка с коротким TTL. Авторизация «кто
    ///          вообще имеет право на ссылку» происходит выше, на уровне API.
    virtual std::string GetUrl(const std::string& key, bool is_public) = 0;

    /// @brief Меняет ACL уже существующего объекта (public-read / private).
    /// @details Используется при смене видимости проекта. По инварианту публичным
    ///          может быть только объект текущей (последней) версии, поэтому это
    ///          всегда один вызов, а не проход по всем версиям.
    virtual void SetAcl(const std::string& key, bool make_public) = 0;

    /// @brief Удаляет объект. Удаление несуществующего ключа — no-op.
    /// @return true, если объект реально был удалён (200/204); false, если
    ///         его уже не было (404) — вызов всё равно считается успешным.
    virtual bool Delete(const std::string& key) = 0;

    /// @brief Удаляет набор объектов (best-effort). Ошибки по отдельным ключам
    ///        не прерывают остальные; лишь пропускаются.
    /// @return Сколько объектов реально удалено.
    virtual std::size_t DeleteMany(const std::vector<std::string>& keys) = 0;

    /// @brief Перечисляет объекты в бакете по префиксу (с пагинацией внутри).
    /// @details Возвращает ключ и время последней модификации каждого объекта.
    ///          Используется orphan-GC для сверки бакета с БД. Список авторизован
    ///          (доступен только сервису), наружу не выставляется.
    /// @param prefix Префикс ключей (например "projects/").
    /// @return Список объектов под префиксом.
    virtual std::vector<ObjectKey> ListKeys(const std::string& prefix) = 0;

};

}  // namespace ya_brander::components