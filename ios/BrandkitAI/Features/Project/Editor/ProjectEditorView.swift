import SwiftUI

struct ProjectEditorView: View {
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
        .navigationTitle("Editor")
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
