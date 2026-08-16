import SwiftUI

struct FeedListView: View {
    var isActive: Bool = true
    var onOpenProject: (UUID) -> Void

    @State private var viewModel: FeedListViewModel

    init(
        scope: FeedScope,
        isActive: Bool = true,
        onOpenProject: @escaping (UUID) -> Void = { _ in }
    ) {
        self.isActive = isActive
        self.onOpenProject = onOpenProject
        _viewModel = State(initialValue: FeedListViewModel(scope: scope))
    }

    var body: some View {
        content
            .task(id: isActive) {
                guard isActive else { return }
                await viewModel.loadInitial()
            }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isEmpty {
            if viewModel.isLoading {
                skeletonGrid
            } else if let message = viewModel.errorMessage {
                stateMessage(message, actionTitle: "Retry")
            } else {
                stateMessage("No projects yet", actionTitle: "Refresh")
            }
        } else {
            list
        }
    }

    private static let gridColumns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]
    private static let skeletonCardCount = 6

    private var skeletonGrid: some View {
        ScrollView {
            LazyVGrid(columns: Self.gridColumns, spacing: 12) {
                ForEach(0..<Self.skeletonCardCount, id: \.self) { _ in
                    FeedListCardSkeleton()
                }
            }
            .padding(16)
        }
        .scrollDisabled(true)
    }

    private var list: some View {
        ScrollView {
            LazyVGrid(columns: Self.gridColumns, spacing: 12) {
                ForEach(viewModel.items) { item in
                    FeedListCardView(
                        title: item.title,
                        previewURL: item.previewURL,
                        accountAvatar: Image("image.placeholder"),
                        accountName: item.authorName,
                        onTap: { onOpenProject(item.id) }
                    )
                    .onAppear {
                        viewModel.loadMoreIfNeeded(after: item)
                    }
                }
            }
            .padding(16)

            if viewModel.isLoadingMore {
                ProgressView()
                    .padding(.bottom, 16)
            }
        }
        .refreshable {
            await viewModel.refresh()
        }
    }

    private func stateMessage(_ message: String, actionTitle: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button(actionTitle) {
                Task { await viewModel.refresh() }
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }
}

#Preview {
    FeedListView(scope: .discover)
}
