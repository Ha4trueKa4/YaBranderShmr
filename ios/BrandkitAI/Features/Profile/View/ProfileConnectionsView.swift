import SwiftUI

struct ProfileConnectionsView: View {
    @State private var viewModel: ProfileConnectionsViewModel

    let kind: ProfileConnectionsKind
    let onBack: () -> Void
    let onOpenProfile: (UUID) -> Void

    init(
        session: SessionStore,
        userID: UUID,
        kind: ProfileConnectionsKind,
        onBack: @escaping () -> Void,
        onOpenProfile: @escaping (UUID) -> Void = { _ in }
    ) {
        self.kind = kind
        self.onBack = onBack
        self.onOpenProfile = onOpenProfile
        _viewModel = State(initialValue: ProfileConnectionsViewModel(
            currentUserService: session.currentUserService,
            userProfileService: session.userProfileService,
            userID: userID,
            kind: kind
        ))
    }

    var body: some View {
        content
            .customNavigationBar(title: kind.title, onBack: onBack)
            .toolbarBackground(Color(.background), for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .task { await viewModel.load() }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.users.isEmpty {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage = viewModel.errorMessage, viewModel.users.isEmpty {
            ContentUnavailableView(
                "Unable to Load",
                systemImage: "person.2.slash",
                description: Text(errorMessage)
            )
        } else if viewModel.users.isEmpty {
            ContentUnavailableView(
                "No \(kind.title) Yet",
                systemImage: "person.2",
                description: Text(emptyDescription)
            )
        } else {
            ScrollView(.vertical, showsIndicators: false) {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.users) { user in
                        connectionRow(user)
                    }
                }
                .padding(16)
            }
            .brandRefresher { await viewModel.load() }
        }
    }

    private func connectionRow(_ user: ProfileConnection) -> some View {
        ProfileConnectionRow(
            name: user.name,
            onOpenProfile: { onOpenProfile(user.id) }
        ) {
            if viewModel.canFollow(user), let isSubscribed = user.isSubscribed {
                FollowButton(
                    isSubscribed: isSubscribed,
                    isBusy: viewModel.busyUserIDs.contains(user.id),
                    action: { viewModel.toggleFollowing(userID: user.id) }
                )
            }
        }
    }

    private var emptyDescription: String {
        switch kind {
        case .followers:
            "Followers will appear here."
        case .following:
            "Followed accounts will appear here."
        }
    }
}

#Preview {
    NavigationStack {
        ProfileConnectionsView(
            session: SessionStore(
                currentUserService: MockCurrentUserService(),
                userProfileService: MockUserProfileService()
            ),
            userID: UUID(),
            kind: .followers,
            onBack: {}
        )
    }
}
