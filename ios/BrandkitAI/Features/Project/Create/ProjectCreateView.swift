import SwiftUI

struct ProjectCreateView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("New Project")

            NavigationLink("Generate Brandkit") {
                ProjectGeneratingView()
            }
        }
        .navigationTitle("New Project")
    }
}

#Preview {
    NavigationStack {
        ProjectCreateView()
    }
}
