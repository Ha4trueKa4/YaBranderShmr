import SwiftUI

struct ProjectVisibilityView: View {
    @Environment(Router.self) private var router

    var body: some View {
        VStack(spacing: 16) {
            Text("Project Visibility")

            Button("Done") {
                router.dismissSheet()
            }
        }
        .presentationDetents([.medium])
    }
}

#Preview {
    ProjectVisibilityView()
        .environment(Router())
}
