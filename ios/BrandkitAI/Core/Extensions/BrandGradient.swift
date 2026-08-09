import SwiftUI

extension ShapeStyle where Self == LinearGradient {
    /// Фирменная заливка главных действий — цвета из ассетов.
    static var brand: LinearGradient {
        LinearGradient(
            colors: [Color(.startGradient), Color(.endGradient)],
            startPoint: .leading,
            endPoint: .trailing
        )
    }
}
