import SwiftUI

extension View {
    func brandBackground() -> some View {
        frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.background).ignoresSafeArea())
    }
}
