import SwiftUI

struct AppRouteView: View {
    @Environment(AppRouter.self) private var router
    @Environment(SessionStore.self) private var session

    let route: AppRoute

    var body: some View {
        destination
            .brandBackground()
    }

    @ViewBuilder
    private var destination: some View {
        switch route {
        case .projectCreate:
            ProjectCreateView(
                onBack: { router.pop() },
                onGenerate: { project in
                    router.popToRoot(.create)
                    router.push(.chat(projectID: project.id, title: project.name), in: .create)
                }
            )

        case .chat(let projectID, let title):
            ChatView(
                projectID: projectID,
                title: title,
                onBack: { router.pop() },
                onOpenImage: { versionID in
                    router.push(.chatImage(
                        projectID: projectID,
                        title: title,
                        versionID: versionID
                    ))
                }
            )

        case .chatImage(let projectID, let title, let versionID):
            ChatImagePreviewView(
                viewModel: ChatImagePreviewViewModel(
                    projectID: projectID,
                    title: title,
                    versionID: versionID
                ),
                onBack: { router.pop() }
            )

        case .projectEditor(let projectID):
            ProjectEditorView(
                onBack: { router.pop() },
                onOpenVersionHistory: { router.push(.versionHistory(projectID: projectID)) }
            )

        case .versionHistory:
            ProjectVersionHistoryView(onBack: { router.pop() })

        case .publicProject(let projectID):
            PublicProjectView(
                projectID: projectID,
                onBack: { router.pop() },
                onOpenAuthor: { router.push(.profile(userID: UUID())) },
                onOpenProject: { router.push(.publicProject(projectID: $0)) }
            )

        case .profile(let userID):
            ProfileView(
                session: session,
                isCurrentUser: false,
                userID: userID,
                onBack: { router.pop() },
                onOpenProject: { router.push(.projectEditor(projectID: UUID())) }
            )
        }
    }
}
