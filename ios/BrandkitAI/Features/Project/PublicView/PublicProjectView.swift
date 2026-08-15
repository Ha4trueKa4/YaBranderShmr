import SwiftUI

struct PublicProjectView: View {
    var onBack: (() -> Void)?
    var onOpenAuthor: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Text("Public Project")

            Button("Author profile", action: onOpenAuthor)
        }
        .customNavigationBar(title: "Public Project", onBack: onBack)
    }
}

#Preview {
    NavigationStack {
        PublicProjectView()
    }
}
