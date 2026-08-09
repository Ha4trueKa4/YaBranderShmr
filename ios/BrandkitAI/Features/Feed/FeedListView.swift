import SwiftUI

struct FeedListView: View {
    var onOpenProject: () -> Void = {}

    var body: some View {
        ScrollView {
            HStack(alignment: .top, spacing: 12) {
                LazyVStack(spacing: 12) {
                    FeedListCardView(
                        title: "Project Alpha",
                        accountImage: Image("image.placeholder"),
                        accountAvatar: Image("image.placeholder"),
                        accountName: "Alice",
                        onTap: onOpenProject
                    )
                    FeedListCardView(
                        title: "Project Gamma with a very long title that should truncate",
                        accountImage: Image("image.placeholder"),
                        accountAvatar: Image("image.placeholder"),
                        accountName: "Charlie",
                        onTap: onOpenProject
                    )
                    FeedListCardView(
                        title: "Project Epsilon",
                        accountImage: Image("image.placeholder"),
                        accountAvatar: Image("image.placeholder"),
                        accountName: "Eve",
                        onTap: onOpenProject
                    )
                }
                .frame(maxWidth: .infinity)

                LazyVStack(spacing: 12) {
                    FeedListCardView(
                        title: "Project Beta",
                        accountImage: Image("image.placeholder"),
                        accountAvatar: Image("image.placeholder"),
                        accountName: "Bob",
                        onTap: onOpenProject
                    )
                    FeedListCardView(
                        title: "Project Delta",
                        accountImage: Image("image.placeholder"),
                        accountAvatar: Image("image.placeholder"),
                        accountName: "Diana",
                        onTap: onOpenProject
                    )
                    FeedListCardView(
                        title: "Project Zeta",
                        accountImage: Image("image.placeholder"),
                        accountAvatar: Image("image.placeholder"),
                        accountName: "Frank",
                        onTap: onOpenProject
                    )
                }
                .frame(maxWidth: .infinity)
            }
            .padding(16)
        }
    }
}

#Preview {
    FeedListView()
}
