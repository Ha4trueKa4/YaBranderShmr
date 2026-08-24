import SwiftUI

struct ProfileView: View {
    private let session: SessionStore
    @State private var viewModel: ProfileViewModel

    var isCurrentUser: Bool
    var onBack: (() -> Void)?
    var onOpenSettings: () -> Void
    var onOpenProject: (UUID, String, URL?) -> Void
    var onOpenConnections: (UUID, ProfileConnectionsKind) -> Void

    init(
        session: SessionStore,
        isCurrentUser: Bool = true,
        userID: UUID? = nil,
        onBack: (() -> Void)? = nil,
        onOpenSettings: @escaping () -> Void = {},
        onOpenProject: @escaping (UUID, String, URL?) -> Void = { _, _, _ in },
        onOpenConnections: @escaping (UUID, ProfileConnectionsKind) -> Void = { _, _ in }
    ) {
        self.session = session
        self.isCurrentUser = isCurrentUser
        self.onBack = onBack
        self.onOpenSettings = onOpenSettings
        self.onOpenProject = onOpenProject
        self.onOpenConnections = onOpenConnections
        _viewModel = State(initialValue: ProfileViewModel(
            currentUserService: session.currentUserService,
            userProfileService: session.userProfileService,
            userID: isCurrentUser ? nil : userID
        ))
    }

    @ViewBuilder
    var body: some View {
        if let onBack {
            profileContent
                .customNavigationBar(title: "", onBack: onBack)
                .toolbarBackground(Color.background, for: .navigationBar)
                .toolbarBackground(.visible, for: .navigationBar)
        } else {
            profileContent
        }
    }

    private var profileContent: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                profileHeader
                
                projectsSection
            }
        }
        .brandRefresher {
            await viewModel.load()
        }
        .scrollIndicators(.hidden)
        .task(id: session.state) { await viewModel.load() }
    }

    private var profileHeader: some View {
        VStack(spacing: 0) {
            cover
            
            VStack(spacing: 12) {
                avatar
                    .offset(y: -32)
                    .padding(.bottom, -32)
                
                if viewModel.name.isEmpty {
                    if viewModel.isLoading {
                        ShimmerPlaceholder()
                            .frame(maxWidth: 150, idealHeight: 20)
                            .clipShape(Capsule())
                    } else if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 20))
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                } else {
                    Text(viewModel.name)
                        .font(.system(size: 20, weight: .bold))
                }

                stats

                if viewModel.showsFollowButton {
                    followButton
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 18)
        }
        .overlay(alignment: .bottom) { separator }
    }

    private var cover: some View {
        let baseHeight = CGFloat(100)
        
        return GeometryReader { proxy in
            LinearGradient(
                colors: [Color(.startGradient), Color(.profileEndGradient)],
                startPoint: .leading,
                endPoint: .trailing
            )
            .mask {
                LinearGradient(
                    colors: [Color(.black).opacity(viewModel.isLoading ? 0.25 : max(0.25, 1 - proxy.frame(in: .scrollView).origin.y * 0.01)), Color(.black)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .frame(height: baseHeight + max(0, onBack == nil ? proxy.frame(in: .global).origin.y : proxy.frame(in: .scrollView).origin.y))
            .accessibilityHidden(true)
            .overlay(alignment: .trailing) {
                if isCurrentUser {
                    settingsButton
                        .padding(.top, 48)
                        .padding(.trailing, 12)
                }
            }
            .offset(y: -max(0, onBack == nil ? proxy.frame(in: .global).origin.y : proxy.frame(in: .scrollView).origin.y))
        }
        .frame(minHeight: baseHeight)
    }

    private var settingsButton: some View {
        CoverSettingsButton(action: onOpenSettings)
    }

    private var avatar: some View {
        AvatarView(initials: viewModel.initials, size: 64, strokeWidth: 3)
            .accessibilityLabel("Profile picture")
    }

    private var followButton: some View {
        FollowButton(
            isSubscribed: viewModel.isSubscribed,
            isBusy: viewModel.isFollowBusy,
            action: {
                session.performAuthenticated { [viewModel] in
                    viewModel.requestFollowToggle()
                }
            }
        )
    }

    private var stats: some View {
        HStack(spacing: 8) {
            stat(value: viewModel.projectsCount, title: "Projects")
            Text("·").foregroundStyle(.secondary)
            connectionStat(
                value: viewModel.followersCount,
                title: "Followers",
                kind: .followers,
                animated: true
            )
            Text("·").foregroundStyle(.secondary)
            connectionStat(
                value: viewModel.followingCount,
                title: "Following",
                kind: .following
            )
        }
        .font(.system(size: 13))
    }

    private func connectionStat(
        value: Int,
        title: String,
        kind: ProfileConnectionsKind,
        animated: Bool = false
    ) -> some View {
        Button {
            guard let profileID = viewModel.profileID else { return }
            onOpenConnections(profileID, kind)
        } label: {
            stat(value: value, title: title, animated: animated)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(viewModel.profileID == nil)
        .accessibilityHint("Opens \(title.lowercased()) list")
    }

    private func stat(value: Int, title: String, animated: Bool = false) -> some View {
        HStack(spacing: 3) {
            if animated {
                Text(value.formatted())
                    .fontWeight(.bold)
                    .contentTransition(.numericText(value: Double(value)))
                    .animation(.easeInOut(duration: 0.2), value: value)
            } else {
                Text(value.formatted())
                    .fontWeight(.bold)
            }
            Text(title)
                .foregroundStyle(.secondary)
        }
    }

    private static let gridColumns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    private var projectsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PROJECTS")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.secondary)

            LazyVGrid(columns: Self.gridColumns, spacing: 12) {
                ForEach(viewModel.projects) { project in
                    projectCard(project)
                }
            }
        }
        .padding(16)
    }

    private func projectCard(_ project: ProfileViewModel.Project) -> some View {
        FeedListCardView(
            title: project.title,
            previewURL: project.previewURL,
            coverColors: project.previewURL == nil ? project.colors : nil,
            coverHeight: project.coverHeight,
            onTap: { onOpenProject(project.id, project.title, project.previewURL) }
        )
    }

    private var separator: some View {
        Rectangle()
            .fill(Color(.border))
            .frame(height: 1)
    }
}

#Preview {
    NavigationStack {
        ProfileView(
            session: SessionStore(
                currentUserService: MockCurrentUserService(),
                userProfileService: MockUserProfileService()
            )
        )
    }
}

#Preview {
    NavigationStack {
        ProfileView(
            session: SessionStore(
                currentUserService: MockCurrentUserService(
                    id: UUID(uuidString: "00000000-0000-0000-0000-000000000001")
                ),
                userProfileService: MockUserProfileService(profile: UserProfile(
                    id: UUID(uuidString: "123e4567-e89b-12d3-a456-426614174000") ?? UUID(),
                    name: "Alex Morgan",
                    subscribersCount: 340,
                    subscriptionsCount: 28,
                    isSubscribed: false,
                    projects: [
                        UserProject(id: UUID(), name: "Orion Labs", url: nil),
                        UserProject(id: UUID(), name: "Cyberpunk Identity", url: nil),
                        UserProject(id: UUID(), name: "Aether Brand", url: nil)
                    ]
                ))
            ),
            isCurrentUser: false,
            userID: UUID(uuidString: "123e4567-e89b-12d3-a456-426614174000"),
            onBack: {}
        )
    }
}
