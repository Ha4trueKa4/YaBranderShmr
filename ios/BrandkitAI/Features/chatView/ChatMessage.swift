import Foundation

enum ChatMessageType: Equatable {
    case user
    case assistant
    case system
}

enum ChatMessageAttachment: Equatable {
    case none
    case mockImage
}

struct ChatMessage: Identifiable {
    let id: UUID
    let type: ChatMessageType
    let text: String
    let date: Date
    let attachment: ChatMessageAttachment

    init(
        id: UUID = UUID(),
        type: ChatMessageType,
        text: String,
        date: Date,
        attachment: ChatMessageAttachment = .none
    ) {
        self.id = id
        self.type = type
        self.text = text
        self.date = date
        self.attachment = attachment
    }
}
