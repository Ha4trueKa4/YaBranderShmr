import SwiftUI

struct PublicProjectView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Public Project")

            NavigationLink("Author profile") {
                ProfileView(isCurrentUser: false)
            }
        }
        .navigationTitle("Public Project")
    }
}

#Preview {
    NavigationStack {
        PublicProjectView()
    }
}
