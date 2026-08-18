import Foundation

#if DEBUG
/// Мок менеджера текущего пользователя для превью и тестов.
@MainActor
final class MockCurrentUserService: CurrentUserService {
    var id: UUID?
    var name: String?

    init(
        id: UUID? = UUID(uuidString: "123e4567-e89b-12d3-a456-426614174000"),
        name: String? = "Alex Morgan"
    ) {
        self.id = id
        self.name = name
    }

    func restore() async {}

    func save(id: UUID, name: String?) {
        self.id = id
        self.name = name
    }

    func clear() {
        id = nil
        name = nil
    }
}
#endif
