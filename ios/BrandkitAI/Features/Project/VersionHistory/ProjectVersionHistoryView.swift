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
    NavigationStack {
        ProjectVersionHistoryView()
    }
}
