import SwiftUI

struct ProjectEditorView: View {
    @State private var isShowingVisibility = false

    var body: some View {
        VStack(spacing: 16) {
            Text("Editor")

            NavigationLink("Version history") {
                ProjectVersionHistoryView()
            }

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
