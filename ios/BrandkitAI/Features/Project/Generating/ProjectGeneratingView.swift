import SwiftUI

struct ProjectGeneratingView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Generating")

            NavigationLink("Open editor") {
                ProjectEditorView()
            }
        }
        .navigationTitle("Generating")
    }
}

#Preview {
    NavigationStack {
        ProjectGeneratingView()
    }
}
