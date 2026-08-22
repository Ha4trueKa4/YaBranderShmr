import SwiftUI
import UIKit

struct ProjectsView: View {
    private let session: SessionStore

    var onOpenChat: (ProjectListItem) -> Void = { _ in }
    var onNewProject: () -> Void = {}

    @State private var viewModel: ProjectsViewModel
    @Environment(AppRouter.self) private var router

    init(
        session: SessionStore,
        onOpenChat: @escaping (ProjectListItem) -> Void = { _ in },
        onNewProject: @escaping () -> Void = {}
    ) {
        self.session = session
        self.onOpenChat = onOpenChat
        self.onNewProject = onNewProject
        _viewModel = State(initialValue: ProjectsViewModel(
            currentUserService: session.currentUserService
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            content
        }
        .brandBackground()
        .overlay(alignment: .bottomTrailing) {
            NewProjectButton {
                onNewProject()
            }
            .padding(16)
        }
        .toolbar(.hidden, for: .navigationBar)
        .task {
            await viewModel.load()
        }
        .onChange(of: router.paths[.create]?.count ?? 0) { _, count in
            if count == 0 {
                Task { await viewModel.load() }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        Group {
            if viewModel.isEmpty {
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let errorMessage = viewModel.errorMessage {
                    stateMessage(errorMessage)
                } else {
                    stateMessage("No projects yet")
                }
            } else {
                List {
                    ForEach(viewModel.items) { item in
                        deletableProjectRow(item)
                            .listRowInsets(
                                EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16)
                            )
                    }
                    .onDelete(perform: viewModel.delete)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .contentMargins(.top, 6, for: .scrollContent)
                .refreshable {
                    await viewModel.refresh()
                }
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            if let errorMessage = viewModel.errorMessage, !viewModel.isEmpty {
                errorBanner(errorMessage)
            }
        }
    }

    @ViewBuilder
    private func deletableProjectRow(_ item: ProjectListItem) -> some View {
        if #available(iOS 26.0, *) {
            projectRow(item)
        } else {
            projectRow(item)
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) {
                        viewModel.delete(item)
                    } label: {
                        DeleteSwipeActionIcon()
                    }
                    .tint(.clear)
                    .accessibilityLabel("Delete \(item.title)")
                }
        }
    }

    private func projectRow(_ item: ProjectListItem) -> some View {
        ProjectRow(
            item: item,
            refreshPreview: {
                try await viewModel.refreshPreview(for: item.id)
            }
        ) {
            onOpenChat(item)
        }
    }

    private func errorBanner(_ message: String) -> some View {
        Button {
            viewModel.clearError()
            Task { await viewModel.refresh() }
        } label: {
            Label(message, systemImage: "exclamationmark.circle")
                .font(.footnote.weight(.medium))
                .foregroundStyle(.primary)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground), in: Capsule())
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
        }
        .buttonStyle(.plain)
        .accessibilityHint("Retry loading projects")
    }

    private var header: some View {
        VStack(spacing: 0) {
            Text(viewModel.title)
                .font(.title3.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.bottom, 12)

            Divider()
        }
    }

    private func stateMessage(_ message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button("Retry") {
                Task { await viewModel.refresh() }
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }
}

private struct DeleteSwipeActionIcon: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Image(uiImage: Self.image(for: colorScheme))
            .renderingMode(.original)
    }

    private static let lightImage = makeImage(userInterfaceStyle: .light)
    private static let darkImage = makeImage(userInterfaceStyle: .dark)

    private static func image(for colorScheme: ColorScheme) -> UIImage {
        colorScheme == .dark ? darkImage : lightImage
    }

    private static func makeImage(userInterfaceStyle: UIUserInterfaceStyle) -> UIImage {
        let canvasSize = CGSize(width: 48, height: 64)
        let renderer = UIGraphicsImageRenderer(size: canvasSize)

        return renderer.image { _ in
            UIColor.systemRed.setFill()
            UIBezierPath(ovalIn: CGRect(x: 0, y: 0, width: 48, height: 48)).fill()

            let configuration = UIImage.SymbolConfiguration(pointSize: 18, weight: .regular)
            guard let symbol = UIImage(
                systemName: "trash",
                withConfiguration: configuration
            )?.withTintColor(.white, renderingMode: .alwaysOriginal) else { return }

            symbol.draw(at: CGPoint(
                x: (canvasSize.width - symbol.size.width) / 2,
                y: (48 - symbol.size.height) / 2
            ))

            let traits = UITraitCollection(userInterfaceStyle: userInterfaceStyle)
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .foregroundColor: UIColor.secondaryLabel.resolvedColor(with: traits)
            ]
            let title = "Delete" as NSString
            let titleSize = title.size(withAttributes: attributes)
            title.draw(
                at: CGPoint(x: (canvasSize.width - titleSize.width) / 2, y: 50),
                withAttributes: attributes
            )
        }
        .withRenderingMode(.alwaysOriginal)
        .withAlignmentRectInsets(UIEdgeInsets(top: 0, left: 0, bottom: 16, right: 0))
    }
}

#Preview {
    ProjectsView(session: SessionStore())
        .environment(AppRouter())
}
