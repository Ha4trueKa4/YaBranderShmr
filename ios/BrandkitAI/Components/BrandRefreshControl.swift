import Refresher
import SwiftUI

extension ScrollView {
    func brandRefresher(action: @escaping AsyncRefreshAction) -> some View {
        refresher(
            style: .overlay,
            action: action
        )
    }
}

extension View {
    func brandListRefresher(action: @escaping AsyncRefreshAction) -> some View {
        refreshable {
            await action()
        }
    }
}
