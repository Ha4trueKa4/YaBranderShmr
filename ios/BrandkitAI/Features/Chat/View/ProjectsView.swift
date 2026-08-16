import SwiftUI

struct ProjectsView: View {
    /// TODO: подставить переходы, когда решится, чем заменить роутер.
    var onOpenChat: (ProjectListItem) -> Void = { _ in }
    var onNewProject: () -> Void = {}

    @State private var viewModel = ProjectsViewModel()

    var body: some View {
        VStack(spacing: 0) {
            header

            List {
                ForEach(viewModel.items) { item in
                    ProjectRow(item: item) {
                        onOpenChat(item)
                    }
                    .swipeActions(
                        edge: .trailing,
                        allowsFullSwipe: true
                    ) {
                        Button(role: .destructive) {
                            viewModel.delete(item)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                    .listRowInsets(
                        EdgeInsets(
                            top: 6,
                            leading: 16,
                            bottom: 6,
                            trailing: 16
                        )
                    )
                }
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .contentMargins(.top, 6, for: .scrollContent)
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
            viewModel.load()
        }
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
}

#Preview {
    ProjectsView()
}
