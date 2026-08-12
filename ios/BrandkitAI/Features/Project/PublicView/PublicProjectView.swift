import SwiftUI

struct PublicProjectView: View {
    var onOpenAuthor: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Text("Public Project")

            Button("Author profile", action: onOpenAuthor)
        }
        .navigationTitle("Public Project")
    }
}

#Preview {
    NavigationStack {
        PublicProjectView()
    }
}
