import Foundation
import Observation

#if DEBUG
/// Мок менеджера текущего пользователя для превью и тестов.
@MainActor
@Observable
final class MockCurrentUserService: CurrentUserService {
    var id: UUID?
    var name: String?
    var email: String?

    init(
        id: UUID? = UUID(uuidString: "123e4567-e89b-12d3-a456-426614174000"),
        name: String? = "Alex Morgan",
        email: String? = "alex@brandkit.ai"
    ) {
        self.id = id
        self.name = name
        self.email = email
    }

    func restore() async {}

    func save(id: UUID, name: String?, email: String?) {
        self.id = id
        self.name = name
        self.email = email
    }

    func update(name: String) {
        self.name = name
    }

    func clear() {
        id = nil
        name = nil
        email = nil
    }
}
#endif
