import SwiftUI

struct ProjectPreviewView: View {
    @State private var viewModel: ProjectPreviewViewModel

    var onBack: (() -> Void)?
    var onOpenAuthor: (UUID) -> Void = { _ in }
    var onOpenProject: (UUID) -> Void = { _ in }
    var onDownloadPNG: () -> Void = {}
    var onDownloadSVG: () -> Void = {}

    init(
        viewModel: ProjectPreviewViewModel,
        onBack: (() -> Void)? = nil,
        onOpenAuthor: @escaping (UUID) -> Void = { _ in },
        onOpenProject: @escaping (UUID) -> Void = { _ in },
        onDownloadPNG: @escaping () -> Void = {},
        onDownloadSVG: @escaping () -> Void = {}
    ) {
        _viewModel = State(initialValue: viewModel)
        self.onBack = onBack
        self.onOpenAuthor = onOpenAuthor
        self.onOpenProject = onOpenProject
        self.onDownloadPNG = onDownloadPNG
        self.onDownloadSVG = onDownloadSVG
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                imagePlaceholder
                projectDetails
            }
        }
        .scrollIndicators(.hidden)
        .customNavigationBar(onBack: onBack)
        .toolbarBackground(Color.background, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task { await viewModel.load() }
    }

    private var imagePlaceholder: some View {
        Group {
            if let versionID = viewModel.versionID {
                SVGDocumentView(
                    source: .url(
                        nil,
                        cacheKey: SVGCache.versionKey(projectID: viewModel.projectID, versionID: versionID),
                        isPrivate: true
                    ),
                    refreshURL: { try await viewModel.versionImageURL() }
                )
            } else if let url = viewModel.imageURL {
                if case .owned = viewModel.mode {
                    SVGDocumentView(
                        source: .url(url, isPrivate: viewModel.imageIsPrivate),
                        refreshURL: { try await viewModel.refreshOwnedImageURL() }
                    )
                } else {
                    SVGDocumentView(source: .url(url))
                }
            } else {
                pendingImage
            }
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Project image")
    }

    private var pendingImage: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.47, green: 0.18, blue: 0.96),
                    Color(red: 0.02, green: 0.73, blue: 0.82),
                    Color(red: 0.91, green: 0.27, blue: 0.60)
                ],
                startPoint: .leading,
                endPoint: .trailing
            )

            if viewModel.isLoading {
                ProgressView().tint(.white)
            } else if viewModel.loadFailed {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 32, weight: .light))
                    .foregroundStyle(.white.opacity(0.72))
            } else {
                Image(systemName: "photo")
                    .font(.system(size: 44, weight: .light))
                    .foregroundStyle(.white.opacity(0.72))
            }
        }
    }

    private var projectDetails: some View {
        VStack(alignment: .leading, spacing: 0) {
            if viewModel.showsAuthorSection {
                authorSection.padding(.top, 18)
            }

            Text(viewModel.title)
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(.primary)
                .padding(.top, viewModel.showsAuthorSection ? 22 : 24)

            Text(viewModel.dateText)
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .padding(.top, 6)

            downloadButton(
                title: "Download PNG",
                systemImage: "square.and.arrow.down",
                isPrimary: true,
                action: onDownloadPNG
            )
            .padding(.top, 20)

            downloadButton(
                title: "Download SVG",
                systemImage: "chevron.left.forwardslash.chevron.right",
                isPrimary: false,
                action: onDownloadSVG
            )
            .padding(.top, 12)

            if viewModel.showsAuthorSection && !viewModel.moreProjects.isEmpty {
                Divider().padding(.vertical, 20)

                Text("More from this creator")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.secondary)

                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: 12),
                        GridItem(.flexible(), spacing: 12)
                    ],
                    spacing: 12
                ) {
                    ForEach(viewModel.moreProjects) { project in
                        FeedListCardView(
                            title: project.title,
                            previewURL: project.previewURL,
                            accountName: viewModel.authorName,
                            onTap: { onOpenProject(project.id) }
                        )
                    }
                }
                .padding(.top, 12)
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 28)
    }

    private var authorSection: some View {
        HStack(spacing: 12) {
            if viewModel.isLoading {
                authorShimmer
            } else {
                Button {
                    if let authorID = viewModel.authorID {
                        onOpenAuthor(authorID)
                    }
                } label: {
                    HStack(spacing: 10) {
                        AvatarView(initials: viewModel.authorInitials, size: 42)
                        Text(viewModel.authorName)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.primary)
                            .lineLimit(1)
                    }
                }
                .buttonStyle(.plain)

                Spacer()

                if viewModel.canFollow {
                    followButton
                }
            }
        }
    }

    private var authorShimmer: some View {
        HStack(spacing: 10) {
            ShimmerPlaceholder()
                .frame(width: 42, height: 42)
                .clipShape(Circle())

            ShimmerPlaceholder()
                .frame(width: 120, height: 15)
                .clipShape(Capsule())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .trailing) {
            ShimmerPlaceholder()
                .frame(width: 96, height: 36)
                .clipShape(Capsule())
        }
    }

    private var followButton: some View {
        FollowButton(
            isSubscribed: viewModel.isSubscribed,
            isBusy: viewModel.isFollowBusy,
            action: viewModel.toggleFollowing
        )
    }

    private func downloadButton(
        title: String,
        systemImage: String,
        isPrimary: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.system(size: 15, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .foregroundStyle(isPrimary ? Color.white : Color.secondary)
                .background {
                    RoundedRectangle(cornerRadius: 9)
                        .fill(isPrimary ? Color.accentColor : Color(.secondarySystemBackground))
                }
                .overlay {
                    if !isPrimary {
                        RoundedRectangle(cornerRadius: 9).stroke(Color(.separator), lineWidth: 1)
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

#if DEBUG
private nonisolated final class PreviewProjectService: ProjectServiceProtocol {
    func fetchVersionURL(projectID: UUID, versionID: UUID) async throws -> URL {
        URL(string: "https://brandkit.ai/preview/sample-version.svg")!
    }
    func fetchProject(id: UUID) async throws -> Project {
        Project(id: id, authorID: UUID(), name: "Nova Studio Brand",
                description: "", isPublic: true, createdAt: Date().addingTimeInterval(-86400 * 3), url: nil)
    }
    func fetchProjects(for userID: UUID) async throws -> [Project] { [] }
    func fetchChat(projectID: UUID) async throws -> [RemoteChatMessage] { [] }
    func generate(projectID: UUID, prompt: String) async throws -> String { UUID().uuidString }
    func generateStatus(projectID: UUID, jobID: String) async throws -> GenerationStatus { .done(url: URL(string: "https://brandkit.ai/preview/sample-version.svg")!) }
    func cancelGeneration(projectID: UUID, jobID: String) async throws -> GenerationStatus { .cancelled }
    func deleteProject(id: UUID) async throws {}
    func createProject(name: String, description: String) async throws -> Project { fatalError() }
    func updateVisibility(projectID: UUID, isPublic: Bool) async throws -> Project { fatalError() }
}
#endif

#Preview {
    NavigationStack {
        ProjectPreviewView(
            viewModel: ProjectPreviewViewModel(
                projectID: UUID(),
                mode: .publicProject,
                currentUserService: MockCurrentUserService(),
                userProfileService: MockUserProfileService(),
                title: "Nova Studio Brand",
                projectService: PreviewProjectService()
            )
        )
        .brandBackground()
        .preferredColorScheme(.dark)
    }
}
