import SwiftUI

struct ProfileView: View {
    @Environment(SessionStore.self) private var session

    var isCurrentUser: Bool = true
    var onBack: (() -> Void)?
    var onOpenProject: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Text("Profile")

            Button("Open project", action: onOpenProject)

            if isCurrentUser {
                Button("Sign out", role: .destructive) {
                    session.signOut()
                }
            }
        }
        .customNavigationBar(title: "Profile", onBack: onBack)
    }
}

#Preview {
    NavigationStack {
        ProfileView()
    }
    .environment(SessionStore())
}
