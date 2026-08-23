import Foundation

nonisolated struct GenerationModel: Identifiable, Hashable, Sendable {
    static let defaultID = "deepseek-v4-flash/latest"
    static let fallback = GenerationModel(
        remoteID: defaultID,
        owner: "Yandex AI Studio"
    )

    let remoteID: String
    let owner: String?

    var id: String { generationID }

    /// `GET /models` returns provider-qualified URIs, while the generation
    /// endpoint accepts the part after `gpt://{provider}/`.
    var generationID: String {
        guard remoteID.hasPrefix("gpt://"),
              let components = URLComponents(string: remoteID)
        else { return remoteID }

        let path = components.path.drop(while: { $0 == "/" })
        return path.isEmpty ? remoteID : String(path)
    }

    var title: String {
        switch generationID {
        case "gpt-5.6-luna":
            "GPT-5.6 Luna"
        case "deepseek-v4-flash/latest":
            "DeepSeek V4 Flash"
        case "gpt-oss-120b/latest":
            "GPT OSS 120B"
        case "qwen3-235b-a22b-fp8/latest":
            "Qwen3 235B"
        default:
            Self.readableName(from: generationID)
        }
    }

    var providerTitle: String {
        if generationID == "gpt-5.6-luna" || owner?.localizedCaseInsensitiveContains("openai") == true {
            return "OpenAI"
        }
        return owner?.isEmpty == false ? owner! : "Yandex AI Studio"
    }

    init(remoteID: String, owner: String?) {
        self.remoteID = remoteID
        self.owner = owner
    }

    private static func readableName(from id: String) -> String {
        let base = id.split(separator: "/").first.map(String.init) ?? id
        return base
            .split(separator: "-")
            .map { part in
                let value = String(part)
                return value.allSatisfy(\.isNumber) ? value : value.capitalized
            }
            .joined(separator: " ")
    }
}
