import SwiftUI

@main
struct BrandkitAIApp: App {
    init() {
        _ = NetworkMonitor.shared
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
