import Foundation

struct ChatMessage: Identifiable {
    let id: UUID
    let type: ChatMessageType
    let text: String
    let date: Date
    let attachment: ChatMessageAttachment
    var isPending: Bool
    var isFailed: Bool

    init(
        id: UUID = UUID(),
        type: ChatMessageType,
        text: String,
        date: Date,
        attachment: ChatMessageAttachment = .none,
        isPending: Bool = false,
        isFailed: Bool = false
    ) {
        self.id = id
        self.type = type
        self.text = text
        self.date = date
        self.attachment = attachment
        self.isPending = isPending
        self.isFailed = isFailed
    }
}
