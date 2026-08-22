import Foundation
import Observation

@MainActor
@Observable
final class CurrentUserStore: CurrentUserService {
    private enum Key {
        static let id = "currentUser.id"
        static let name = "currentUser.name"
        static let legacyEmail = "currentUser.email"
    }

    private(set) var id: UUID? {
        didSet { UserDefaults.standard.set(id?.uuidString, forKey: Key.id) }
    }

    private(set) var name: String? {
        didSet { UserDefaults.standard.set(name, forKey: Key.name) }
    }

    private(set) var email: String? {
        didSet { KeychainItem.userEmail.value = email }
    }

    private let tokenStore: TokenStore
    private let userProfileService: any UserProfileService

    init(tokenStore: TokenStore, userProfileService: any UserProfileService) {
        self.tokenStore = tokenStore
        self.userProfileService = userProfileService
        self.id = UserDefaults.standard.string(forKey: Key.id)
            .flatMap(UUID.init(uuidString:))
        self.name = UserDefaults.standard.string(forKey: Key.name)
        self.email = KeychainItem.userEmail.value
            ?? UserDefaults.standard.string(forKey: Key.legacyEmail)

        migrateLegacyEmailIfNeeded()
    }

    private func migrateLegacyEmailIfNeeded() {
        guard KeychainItem.userEmail.value == nil, let email else { return }
        KeychainItem.userEmail.value = email
        UserDefaults.standard.removeObject(forKey: Key.legacyEmail)
    }

    func restore() async {
        guard let token = tokenStore.accessToken,
              let tokenUserID = JWTDecoder.userID(from: token) else {
            clear()
            return
        }

        if let id, id != tokenUserID {
            name = nil
            email = nil
        }
        id = tokenUserID

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
        UserDefaults.standard.removeObject(forKey: Key.legacyEmail)
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
