import SwiftUI

/// Единственное место, где маршрут превращается в экран и где переходы экранов
/// подключаются к роутеру. Сами экраны о навигации не знают — они лишь зовут
/// свои колбэки.
///
/// - Note: временные `UUID()` ниже — заглушки на месте будущих данных.
///   Все они собраны здесь, чтобы при подключении API править одно место.
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
                onGenerate: { name in
                    router.push(.chat(projectID: UUID(), title: name))
                }
            )

        case .chat(let projectID, let title):
            ChatView(
                title: title,
                onBack: { router.pop() },
                onOpenImage: { router.push(.chatImage(projectID: projectID, title: title)) }
            )

        case .chatImage(let projectID, let title):
            ChatImagePreviewView(
                viewModel: ChatImagePreviewViewModel(projectID: projectID, title: title),
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
                userID: userID.uuidString,
                onBack: { router.pop() },
                onOpenProject: { router.push(.projectEditor(projectID: UUID())) }
            )
        }
    }
}
