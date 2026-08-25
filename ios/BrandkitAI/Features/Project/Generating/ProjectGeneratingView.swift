import SwiftUI

struct ProjectGeneratingView: View {
    var onBack: (() -> Void)?
    var onOpenEditor: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Text("Generating")

            Button("Open editor", action: onOpenEditor)
        }
        .customNavigationBar(title: String(localized: "Generating"), onBack: onBack)
    }
}

#Preview {
    NavigationStack {
        ProjectGeneratingView()
    }
}
