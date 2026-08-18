import SwiftUI

@main
struct BrandkitAIApp: App {
    init() {
        // SVG-превью кешируются HTTP-кешем URLSession; дефолтные лимиты малы.
        URLCache.shared = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
            diskCapacity: 100 * 1024 * 1024
        )
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
