import SwiftUI

struct ProfileView: View {
    @State private var viewModel = ProfileViewModel()

    var isCurrentUser: Bool = true
    var onEditProfile: () -> Void = {}
    var onOpenProject: () -> Void = {}

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                profileHeader
                projectsSection
            }
        }
        .ignoresSafeArea(edges: .top)
        .scrollIndicators(.hidden)
        .navigationTitle("Profile")
        .toolbar(.hidden, for: .navigationBar)
    }

    private var profileHeader: some View {
        VStack(spacing: 0) {
            cover

            VStack(spacing: 12) {
                avatar
                    .offset(y: -32)
                    .padding(.bottom, -32)

                VStack(spacing: 3) {
                    Text(viewModel.name)
                        .font(.system(size: 20, weight: .bold))

                    Text(viewModel.nickname)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.secondary)
                }

                Text(viewModel.bio)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 320)

                stats

                if isCurrentUser {
                    Button("Edit Profile", action: onEditProfile)
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
    }

    private var avatar: some View {
        Text(viewModel.initials)
            .font(.system(size: 19, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 64, height: 64)
            .background(Color(.endGradient), in: .circle)
            .overlay { Circle().stroke(Color(.background), lineWidth: 3) }
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

    private var projectsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PROJECTS")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.secondary)

            HStack(alignment: .top, spacing: 12) {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.leftProjects) { project in
                        projectCard(project)
                    }
                }

                LazyVStack(spacing: 12) {
                    ForEach(viewModel.rightProjects) { project in
                        projectCard(project)
                    }
                }
            }
        }
        .padding(16)
    }

    private func projectCard(_ project: ProfileViewModel.Project) -> some View {
        FeedListCardView(
            title: project.title,
            coverColors: project.colors,
            coverHeight: project.coverHeight,
            onTap: onOpenProject
        )
    }

    private var separator: some View {
        Rectangle()
            .fill(Color(.tabBarSeparator))
            .frame(height: 1)
    }
}

#Preview {
    NavigationStack {
        ProfileView()
    }
    .environment(SessionStore())
}
