import SwiftUI

struct ProfileView: View {
    @Environment(SessionStore.self) private var session

    var isCurrentUser: Bool = true

    var body: some View {
        VStack(spacing: 16) {
            Text("Profile")

            NavigationLink("Open project") {
                ProjectEditorView()
            }

            if isCurrentUser {
                Button("Sign out", role: .destructive) {
                    session.signOut()
                }
            }
        }
        .navigationTitle("Profile")
    }
}

#Preview {
    NavigationStack {
        ProfileView()
    }
    .environment(SessionStore())
}
