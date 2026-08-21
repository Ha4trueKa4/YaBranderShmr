import SwiftUI

struct ProfileView: View {
    private let session: SessionStore
    @State private var viewModel: ProfileViewModel

    var isCurrentUser: Bool
    var onBack: (() -> Void)?
    var onEditProfile: () -> Void
    var onOpenProject: (UUID, String, URL?) -> Void

    init(
        session: SessionStore,
        isCurrentUser: Bool = true,
        userID: UUID? = nil,
        onBack: (() -> Void)? = nil,
        onEditProfile: @escaping () -> Void = {},
        onOpenProject: @escaping (UUID, String, URL?) -> Void = { _, _, _ in }
    ) {
        self.session = session
        self.isCurrentUser = isCurrentUser
        self.onBack = onBack
        self.onEditProfile = onEditProfile
        self.onOpenProject = onOpenProject
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
                .ignoresSafeArea(edges: .top)
                .toolbar(.hidden, for: .navigationBar)
        }
    }

    private var profileContent: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                profileHeader
                projectsSection
            }
        }
        .scrollIndicators(.hidden)
        .task { await viewModel.load() }
        .refreshable { await viewModel.load() }
    }

    private var profileHeader: some View {
        VStack(spacing: 0) {
            cover

            VStack(spacing: 12) {
                avatar
                    .offset(y: -32)
                    .padding(.bottom, -32)

                Text(viewModel.name)
                    .font(.system(size: 20, weight: .bold))

                if viewModel.name.isEmpty {
                    if viewModel.isLoading {
                        ProgressView()
                    } else if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 13))
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                }

                stats

                if isCurrentUser {
                    Button(action: onEditProfile) {
                        Text("Edit Profile")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color.accentColor)
                            .padding(.horizontal, 20)
                            .frame(height: 36)
                            .overlay {
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.accentColor, lineWidth: 1)
                            }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 18)
        }
        .overlay(alignment: .bottom) { separator }
    }

    private var cover: some View {
        LinearGradient(
            colors: [Color(.startGradient), Color(.endGradient)],
            startPoint: .leading,
            endPoint: .trailing
        )
        .frame(height: 132)
        .accessibilityHidden(true)
        .overlay(alignment: .topTrailing) {
            if isCurrentUser {
                signOutButton
                    .padding(.top, 48)
                    .padding(.trailing, 12)
            }
        }
    }

    private var signOutButton: some View {
        Button(role: .destructive) {
            session.signOut()
        } label: {
            Image(systemName: "rectangle.portrait.and.arrow.right")
                .font(.system(size: 16, weight: .semibold))
                .frame(width: 44, height: 44)
        }
        .buttonStyle(.plain)
        .foregroundStyle(Color(.onAccent))
        .accessibilityLabel("Sign out")
    }

    private var avatar: some View {
        AvatarView(initials: viewModel.initials, size: 64, strokeWidth: 3)
            .accessibilityLabel("Profile picture")
    }

    private var stats: some View {
        HStack(spacing: 8) {
            stat(value: viewModel.projectsCount, title: "Projects")
            Text("·").foregroundStyle(.secondary)
            stat(value: viewModel.followersCount, title: "Followers")
            Text("·").foregroundStyle(.secondary)
            stat(value: viewModel.followingCount, title: "Following")
        }
        .font(.system(size: 13))
    }

    private func stat(value: Int, title: String) -> some View {
        HStack(spacing: 3) {
            Text(value.formatted())
                .fontWeight(.bold)
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
                currentUserService: MockCurrentUserService(),
                userProfileService: MockUserProfileService(),
            ),
            onBack: {}
        )
    }
}
