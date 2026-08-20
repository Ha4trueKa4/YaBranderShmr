import SwiftUI

struct ChatImagePreviewView: View {
    let viewModel: ChatImagePreviewViewModel
    var onBack: () -> Void = {}
    var onDownloadPNG: () -> Void = {}
    var onDownloadSVG: () -> Void = {}

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                imagePlaceholder

                VStack(alignment: .leading, spacing: 0) {
                    Text(viewModel.title)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(.primary)
                        .padding(.top, 24)

                    Text(viewModel.publishedText)
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
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .scrollIndicators(.hidden)
        .customNavigationBar(onBack: onBack)
        .toolbarBackground(Color.background, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private var imagePlaceholder: some View {
        SVGDocumentView(
            source: .url(
                nil,
                cacheKey: SVGCache.versionKey(projectID: viewModel.projectID, versionID: viewModel.versionID),
                isPrivate: true
            ),
            refreshURL: {
                try await viewModel.versionURL()
            }
        )
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Generated project image")
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
                .foregroundStyle(isPrimary ? .white : .secondary)
                .background {
                    RoundedRectangle(cornerRadius: 9)
                        .fill(isPrimary ? Color.accentColor : Color(.secondarySystemBackground))
                }
                .overlay {
                    if !isPrimary {
                        RoundedRectangle(cornerRadius: 9)
                            .stroke(Color(.separator), lineWidth: 1)
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

private nonisolated final class PreviewProjectService: ProjectServiceProtocol {
    func fetchVersionURL(projectID: UUID, versionID: UUID) async throws -> URL {
        URL(string: "https://brandkit.ai/preview/sample-version.svg")!
    }

    func fetchProjects(for userID: UUID) async throws -> [Project] { [] }
    func fetchChat(projectID: UUID) async throws -> [RemoteChatMessage] { [] }
    func generate(projectID: UUID, prompt: String) async throws -> URL? { nil }
    func deleteProject(id: UUID) async throws {}
    func fetchProject(id: UUID) async throws -> Project { fatalError("unused in preview") }
    func createProject(name: String, description: String) async throws -> Project { fatalError("unused in preview") }
    func updateVisibility(projectID: UUID, isPublic: Bool) async throws -> Project { fatalError("unused in preview") }
}

#Preview {
    NavigationStack {
        ChatImagePreviewView(
            viewModel: ChatImagePreviewViewModel(
                projectID: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!,
                title: "Nova Studio Brand",
                versionID: UUID(uuidString: "00000000-0000-0000-0000-000000000002")!,
                service: PreviewProjectService()
            )
        )
        .brandBackground()
        .preferredColorScheme(.dark)
    }
}
