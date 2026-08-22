import SwiftUI

@main
struct BrandkitAIApp: App {
    init() {
        // Бутстрап синглтона: стартует NWPathMonitor, который постит
        // `.networkDidBecomeAvailable` — на него подписан оффлайн-флаш чата.
        _ = NetworkMonitor.shared
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
