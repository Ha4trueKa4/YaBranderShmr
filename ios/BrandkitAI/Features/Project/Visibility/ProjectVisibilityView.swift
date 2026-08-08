import SwiftUI

struct ProjectVisibilityView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 16) {
            Text("Project Visibility")

            Button("Done") {
                dismiss()
            }
        }
        .presentationDetents([.medium])
    }
}

#Preview {
    ProjectVisibilityView()
}
