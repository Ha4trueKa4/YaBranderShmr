import Foundation

enum ChatMessageAttachment: Equatable {
    case none
    case version(id: UUID, url: URL?)
    case error(message: String)
    case cancelled
}
