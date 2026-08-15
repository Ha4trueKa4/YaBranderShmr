import SwiftUI

struct ProjectVersionHistoryView: View {
    var onBack: (() -> Void)?

    var body: some View {
        VStack(spacing: 16) {
            Text("Version History")
        }
        .customNavigationBar(title: "Version History", onBack: onBack)
    }
}

#Preview {
    NavigationStack {
        ProjectVersionHistoryView()
    }
}
