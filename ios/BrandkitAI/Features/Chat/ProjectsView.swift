import SwiftUI

struct ProjectsView: View {
    /// TODO: подставить переходы, когда решится, чем заменить роутер.
    var onOpenChat: (ProjectListItem) -> Void = { _ in }
    var onNewProject: () -> Void = {}

    private let items = ProjectListItem.mocks

    var body: some View {
        VStack(spacing: 0) {
            header

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(items) { item in
                        ProjectRow(item: item) {
                            onOpenChat(item)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .overlay(alignment: .bottomTrailing) {
            NewProjectButton {
                onNewProject()
            }
            .padding(16)
        }
        .toolbar(.hidden, for: .navigationBar)
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        VStack(spacing: 0) {
            Text("Chats")
                .font(.title3.bold())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.bottom, 12)

            Divider()
        }
    }
}

