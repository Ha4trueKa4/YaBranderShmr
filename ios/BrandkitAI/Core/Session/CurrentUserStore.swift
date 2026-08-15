import Foundation

/// Менеджер хранения информации об авторизованном пользователе в UserDefaults.
///
/// Хранит только id и имя — это не секреты, поэтому UserDefaults, а не Keychain;
/// токен по-прежнему лежит в `TokenStore`. Если хранилище пусто, а токен есть
/// (реальный случай — переустановка приложения), id восстанавливается из JWT,
/// после чего имя дотаскивается из API по этому id.
@MainActor
final class CurrentUserStore: CurrentUserService {
    private enum Key {
        static let id = "currentUser.id"
        static let name = "currentUser.name"
    }

    private(set) var id: String? {
        didSet { UserDefaults.standard.set(id, forKey: Key.id) }
    }

    private(set) var name: String? {
        didSet { UserDefaults.standard.set(name, forKey: Key.name) }
    }

    private let tokenStore: TokenStore
    private let userProfileService: any UserProfileService

    init(tokenStore: TokenStore, userProfileService: any UserProfileService) {
        self.tokenStore = tokenStore
        self.userProfileService = userProfileService
        self.id = UserDefaults.standard.string(forKey: Key.id)
        self.name = UserDefaults.standard.string(forKey: Key.name)
    }

    func restore() async {
        if let id {
            if name == nil {
                await refreshName(for: id)
            }
            return
        }

        guard let token = tokenStore.accessToken,
              let userID = JWTDecoder.userID(from: token) else {
            return
        }

        id = userID
        await refreshName(for: userID)
    }

    func save(id: String, name: String?) {
        self.id = id
        self.name = name
    }

    func clear() {
        id = nil
        name = nil
    }

    private func refreshName(for id: String) async {
        do {
            let profile = try await userProfileService.profile(for: id)
            guard self.id == id else { return }
            name = profile.name
        } catch {
            // Сбой некритичен: имя будет запрошено экраном профиля отдельным запросом.
        }
    }
}
