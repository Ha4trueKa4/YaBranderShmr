import SwiftUI

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
                        ProjectRow(
                            item: item,
                            refreshPreview: {
                                try await viewModel.refreshPreview(for: item.id)
                            }
                        ) {
                            onOpenChat(item)
                        }
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

#Preview {
    ProjectsView(session: SessionStore())
        .environment(AppRouter())
}
