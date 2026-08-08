import SwiftUI

struct ProjectVersionHistoryView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Version History")
        }
        .navigationTitle("Version History")
    }
}

#Preview {
    RoutedNavigationStack(router: Router(), root: .projectVersionHistory)
        .environment(AppRouter(flow: .main))
}
