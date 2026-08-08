import Foundation

enum Route: Hashable {
    case onboarding

    case login
    case register

    case feed

    case projectCreate
    case projectGenerating
    case projectEditor
    case projectChat
    case projectVersionHistory
    case publicProject

    case profile(ProfileTarget)
}

enum ProfileTarget: Hashable {
    case current
    case other
}

enum SheetRoute: Identifiable, Hashable {
    case projectVisibility

    var id: Self { self }
}
