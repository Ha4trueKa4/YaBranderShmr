import SwiftUI

/// TODO: заменить содержимое `overlay` на реальный логотип, когда появится ассет:
///       `Image("Logo").resizable().scaledToFit().padding(8)`
struct LogoBadge: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(.tint.opacity(0.2))
            .frame(width: 60, height: 60)
            .overlay {
                Image(systemName: "swift").resizable().scaledToFit().padding(8)
            }
            .accessibilityHidden(true)
    }
}

#Preview {
    LogoBadge()
        .tint(.purple)
        .preferredColorScheme(.dark)
}
