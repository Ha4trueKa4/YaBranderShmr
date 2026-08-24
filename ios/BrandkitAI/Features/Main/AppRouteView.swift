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
                onGenerate: { result in
                    router.popToRoot(.create)
                    router.push(
                        .chat(projectID: result.project.id, title: result.project.name),
                        in: .create
                    )
                }
            )

        case .chat(let projectID, let title):
            ChatView(
                projectID: projectID,
                title: title,
                onBack: { router.pop() },
                onOpenImage: { versionID, createdAt in
                    router.push(.chatImage(
                        projectID: projectID,
                        title: title,
                        versionID: versionID,
                        createdAt: createdAt
                    ))
                }
            )

        case .chatImage(let projectID, let title, let versionID, let createdAt):
            ProjectPreviewView(
                viewModel: ProjectPreviewViewModel(
                    projectID: projectID,
                    mode: .version(versionID: versionID, createdAt: createdAt),
                    currentUserService: session.currentUserService,
                    userProfileService: session.userProfileService,
                    title: title
                ),
                onBack: { router.pop() }
            )

        case .projectImage(let projectID, let title, let imageURL):
            ProjectPreviewView(
                viewModel: ProjectPreviewViewModel(
                    projectID: projectID,
                    mode: .owned(imageURL: imageURL),
                    currentUserService: session.currentUserService,
                    userProfileService: session.userProfileService,
                    title: title
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
            ProjectPreviewView(
                viewModel: ProjectPreviewViewModel(
                    projectID: projectID,
                    mode: .publicProject,
                    currentUserService: session.currentUserService,
                    userProfileService: session.userProfileService
                ),
                onBack: { router.pop() },
                onOpenAuthor: { authorID in router.push(.profile(userID: authorID)) },
                onOpenProject: { router.push(.publicProject(projectID: $0)) }
            )

        case .profile(let userID):
            ProfileView(
                session: session,
                isCurrentUser: false,
                userID: userID,
                onBack: { router.pop() },
                onOpenProject: { projectID, _, _ in
                    router.push(.publicProject(projectID: projectID))
                },
                onOpenConnections: { userID, kind in
                    router.push(.profileConnections(userID: userID, kind: kind))
                }
            )

        case .profileConnections(let userID, let kind):
            ProfileConnectionsView(
                session: session,
                userID: userID,
                kind: kind,
                onBack: { router.pop() },
                onOpenProfile: { router.push(.profile(userID: $0)) }
            )

        case .settings:
            SettingsView(session: session, onBack: { router.pop() })
        }
    }
}
