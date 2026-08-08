import SwiftUI

struct FeedView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Feed")

            NavigationLink("Open public project") {
                PublicProjectView()
            }
        }
        .navigationTitle("Feed")
    }
}

#Preview {
    NavigationStack {
        FeedView()
    }
}
