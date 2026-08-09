import Foundation


@MainActor
@Observable
final class ProjectCreateViewModel {
    var name = ""
    var brief = ""

    let title = "New Project"
    let hint = "Our AI will generate brand assets, palettes, and typography presets."

    var canSubmit: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !brief.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
