import Foundation

/// Данные экрана списка чатов.
///
/// Сейчас отдаёт моки; когда появится сеть, здесь же будет загрузка,
/// состояния загрузки/ошибки и обновление списка.
@MainActor
@Observable
final class ProjectsViewModel {
    private(set) var items: [ProjectListItem] = ProjectListItem.mocks

    let title = "Projects"

    var isEmpty: Bool { items.isEmpty }

    /// TODO: заменить на загрузку с бэкенда.
    func load() {
        items = ProjectListItem.mocks
    }
}
