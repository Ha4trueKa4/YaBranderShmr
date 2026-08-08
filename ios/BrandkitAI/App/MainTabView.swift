import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            NavigationStack {
                FeedView()
            }
            .tabItem { Label("Feed", systemImage: "square.grid.2x2") }

            NavigationStack {
                ProjectCreateView()
            }
            .tabItem { Label("Create", systemImage: "plus.circle") }

            NavigationStack {
                ProfileView(isCurrentUser: true)
            }
            .tabItem { Label("Profile", systemImage: "person") }
        }
    }
}

#Preview {
    MainTabView()
        .environment(SessionStore())
}
