import SwiftUI

struct ProfileConnectionsView: View {
    private let session: SessionStore
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
        self.session = session
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
            .task(id: session.state) { await viewModel.load() }
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
        HStack(spacing: 12) {
            Button {
                onOpenProfile(user.id)
            } label: {
                HStack(spacing: 12) {
                    AvatarView(initials: user.name.initials, size: 56)

                    Text(user.name)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if viewModel.showsFollowButton(for: user) {
                FollowButton(
                    isSubscribed: user.isSubscribed ?? false,
                    isBusy: viewModel.busyUserIDs.contains(user.id),
                    action: {
                        guard session.requireAuthentication() else { return }
                        viewModel.toggleFollowing(userID: user.id)
                    }
                )
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 88)
        .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 14))
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
