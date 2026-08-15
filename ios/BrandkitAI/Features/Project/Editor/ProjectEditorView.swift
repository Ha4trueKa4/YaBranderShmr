import SwiftUI

struct ProjectEditorView: View {
    var onBack: (() -> Void)?
    var onOpenVersionHistory: () -> Void = {}

    @State private var isShowingVisibility = false

    var body: some View {
        VStack(spacing: 16) {
            Text("Editor")

            Button("Version history", action: onOpenVersionHistory)

            Button("Visibility") {
                isShowingVisibility = true
            }
        }
        .customNavigationBar(title: "Editor", onBack: onBack)
        .sheet(isPresented: $isShowingVisibility) {
            ProjectVisibilityView()
        }
    }
}

#Preview {
    NavigationStack {
        ProjectEditorView()
    }
}
