import SwiftUI

struct ProjectsView: View {
    /// TODO: подставить переходы, когда решится, чем заменить роутер.
    var onOpenChat: (ProjectListItem) -> Void = { _ in }
    var onNewProject: () -> Void = {}

    @State private var viewModel = ProjectsViewModel()

    var body: some View {
        VStack(spacing: 0) {
            header

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.items) { item in
                        ProjectRow(item: item) {
                            onOpenChat(item)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
            }
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
