import SwiftUI

struct ProjectGeneratingView: View {
    var onOpenEditor: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Text("Generating")

            Button("Open editor", action: onOpenEditor)
        }
        .navigationTitle("Generating")
    }
}

#Preview {
    NavigationStack {
        ProjectGeneratingView()
    }
}
