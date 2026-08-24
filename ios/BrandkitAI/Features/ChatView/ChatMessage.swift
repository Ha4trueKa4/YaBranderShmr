import Foundation

struct ChatMessage: Identifiable {
    let id: UUID
    let type: ChatMessageType
    let text: String
    let date: Date
    let attachment: ChatMessageAttachment
    let jobID: String?
    var isPending: Bool
    var isFailed: Bool

    init(
        id: UUID = UUID(),
        type: ChatMessageType,
        text: String,
        date: Date,
        attachment: ChatMessageAttachment = .none,
        jobID: String? = nil,
        isPending: Bool = false,
        isFailed: Bool = false
    ) {
        self.id = id
        self.type = type
        self.text = text
        self.date = date
        self.attachment = attachment
        self.jobID = jobID
        self.isPending = isPending
        self.isFailed = isFailed
    }
}
