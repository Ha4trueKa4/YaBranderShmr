import SwiftUI

@main
struct BrandkitAIApp: App {
    init() {
        _ = NetworkMonitor.shared
        YandexAuthService.activate()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .onOpenURL { url in
                    YandexAuthService.handleOpenURL(url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    YandexAuthService.handleUserActivity(activity)
                }
        }
    }
}
