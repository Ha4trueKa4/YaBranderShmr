import SwiftUI

struct PublicProjectView: View {
    @State private var viewModel: PublicProjectViewModel
    var onBack: (() -> Void)?
    var onOpenAuthor: () -> Void = {}
    var onOpenProject: (UUID) -> Void = { _ in }
    var onDownloadPNG: () -> Void = {}
    var onDownloadSVG: () -> Void = {}

    init(
        projectID: UUID = UUID(),
        onBack: (() -> Void)? = nil,
        onOpenAuthor: @escaping () -> Void = {},
        onOpenProject: @escaping (UUID) -> Void = { _ in },
        onDownloadPNG: @escaping () -> Void = {},
        onDownloadSVG: @escaping () -> Void = {}
    ) {
        _viewModel = State(initialValue: PublicProjectViewModel(projectID: projectID))
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
    }

    private var imagePlaceholder: some View {
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

            Image(systemName: "photo")
                .font(.system(size: 44, weight: .light))
                .foregroundStyle(.white.opacity(0.72))
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Project image placeholder")
    }

    private var projectDetails: some View {
        VStack(alignment: .leading, spacing: 0) {
            authorSection.padding(.top, 18)

            Text(viewModel.title)
                .font(.system(size: 24, weight: .bold))
                .padding(.top, 22)

            Text(viewModel.publishedText)
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .padding(.top, 6)

            downloadButton(title: "Download PNG", systemImage: "square.and.arrow.down", isPrimary: true, action: onDownloadPNG)
                .padding(.top, 20)
            downloadButton(title: "Download SVG", systemImage: "chevron.left.forwardslash.chevron.right", isPrimary: false, action: onDownloadSVG)
                .padding(.top, 12)

            Divider().padding(.vertical, 20)

            Text("More from this creator")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.secondary)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(viewModel.moreProjects) { project in
                    creatorProjectCard(project)
                }
            }
            .padding(.top, 12)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 28)
    }

    private var authorSection: some View {
        HStack(spacing: 12) {
            Button(action: onOpenAuthor) {
                HStack(spacing: 10) {
                    Text(viewModel.authorInitials)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 42, height: 42)
                        .background(Color(.endGradient), in: Circle())

                    VStack(alignment: .leading, spacing: 3) {
                        Text(viewModel.authorName)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.primary)
                        Text(viewModel.authorRole)
                            .font(.system(size: 12))
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            Button(viewModel.isFollowing ? "Following" : "Follow") {
                viewModel.toggleFollowing()
            }
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .frame(height: 36)
            .background(Color.accentColor, in: Capsule())
        }
    }

    private func downloadButton(title: String, systemImage: String, isPrimary: Bool, action: @escaping () -> Void) -> some View {
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

    private func creatorProjectCard(_ project: PublicProjectViewModel.RelatedProject) -> some View {
        Button { onOpenProject(project.id) } label: {
            VStack(alignment: .leading, spacing: 0) {
                LinearGradient(colors: project.colors, startPoint: .topLeading, endPoint: .bottomTrailing)
                    .frame(height: 104)
                VStack(alignment: .leading) {
                    Text(project.title)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                }
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        PublicProjectView()
            .brandBackground()
            .preferredColorScheme(.dark)
    }
}
