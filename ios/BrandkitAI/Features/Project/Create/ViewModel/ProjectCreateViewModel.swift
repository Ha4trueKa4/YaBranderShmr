import Foundation


@MainActor
@Observable
final class ProjectCreateViewModel {
    var name = ""
    var brief = ""
    var isGenerating = false

    let title = "New Project"
    let hint = "Our AI will generate brand assets, palettes, and typography presets."

    var canSubmit: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !brief.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    
    func generate() {
        guard canSubmit else { return }
        isGenerating = true
    }
}
