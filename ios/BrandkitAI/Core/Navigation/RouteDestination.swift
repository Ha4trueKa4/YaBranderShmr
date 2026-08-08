import SwiftUI

struct RouteDestination: View {
    let route: Route
    
    var body: some View {
        switch route {
        case .onboarding:
            OnboardingView()
            
        case .login:
            LoginView()
            
        case .register:
            RegisterView()
            
        case .feed:
            FeedView()
            
        case .projectCreate:
            ProjectCreateView()
            
        case .projectGenerating:
            ProjectGeneratingView()
            
        case .projectEditor:
            ProjectEditorView()
            
        case .projectVersionHistory:
            ProjectVersionHistoryView()
            
        case .publicProject:
            PublicProjectView()
            
        case let .profile(target):
            ProfileView(target: target)
        }
    }
    
    struct SheetDestination: View {
        let route: SheetRoute
        
        var body: some View {
            switch route {
            case .projectVisibility:
                ProjectVisibilityView()
            }
        }
    }
}
