import SwiftUI

/// Единственное место, где маршрут превращается в экран и где переходы экранов
/// подключаются к роутеру. Сами экраны о навигации не знают — они лишь зовут
/// свои колбэки.
///
/// - Note: временные `UUID()` ниже — заглушки на месте будущих данных.
///   Все они собраны здесь, чтобы при подключении API править одно место.
struct AppRouteView: View {
    @Environment(AppRouter.self) private var router

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
                onGenerate: { router.push(.projectGenerating(projectID: UUID())) }
            )

        case .projectGenerating(let projectID):
            ProjectGeneratingView(
                onOpenEditor: { router.push(.projectEditor(projectID: projectID)) }
            )

        case .projectEditor(let projectID):
            ProjectEditorView(
                onOpenVersionHistory: { router.push(.versionHistory(projectID: projectID)) }
            )

        case .versionHistory:
            ProjectVersionHistoryView()

        case .publicProject:
            PublicProjectView(
                onOpenAuthor: { router.push(.profile(userID: UUID())) }
            )

        case .profile:
            ProfileView(
                isCurrentUser: false,
                onOpenProject: { router.push(.projectEditor(projectID: UUID())) }
            )
        }
    }
}
