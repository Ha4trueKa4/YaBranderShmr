import Foundation
import Observation

@MainActor
@Observable
final class CurrentUserStore: CurrentUserService {
    private enum Key {
        static let id = "currentUser.id"
        static let name = "currentUser.name"
        static let email = "currentUser.email"
    }

    private(set) var id: UUID? {
        didSet { UserDefaults.standard.set(id?.uuidString, forKey: Key.id) }
    }

    private(set) var name: String? {
        didSet { UserDefaults.standard.set(name, forKey: Key.name) }
    }

    private(set) var email: String? {
        didSet { UserDefaults.standard.set(email, forKey: Key.email) }
    }

    private let tokenStore: TokenStore
    private let userProfileService: any UserProfileService

    init(tokenStore: TokenStore, userProfileService: any UserProfileService) {
        self.tokenStore = tokenStore
        self.userProfileService = userProfileService
        self.id = UserDefaults.standard.string(forKey: Key.id)
            .flatMap(UUID.init(uuidString:))
        self.name = UserDefaults.standard.string(forKey: Key.name)
        self.email = UserDefaults.standard.string(forKey: Key.email)
    }

    func restore() async {
        guard let token = tokenStore.accessToken,
              let tokenUserID = JWTDecoder.userID(from: token) else {
            clear()
            return
        }

        if id != tokenUserID {
            id = tokenUserID
            name = nil
            email = nil
        }

        if name == nil {
            await refreshName(for: tokenUserID)
        }
    }

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

    private func refreshName(for id: UUID) async {
        do {
            let profile = try await userProfileService.profile(for: id)
            guard self.id == id else { return }
            name = profile.name
        } catch {
            // Сбой некритичен: имя будет запрошено экраном профиля отдельным запросом.
        }
    }
}
