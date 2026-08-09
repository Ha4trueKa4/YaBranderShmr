import SwiftUI

struct ChatMessageView: View {
    let message: ChatMessage
    let timeTitle: String
    private let assistantContent: AnyView

    init(message: ChatMessage, timeTitle: String) {
        self.init(message: message, timeTitle: timeTitle) {
            EmptyView()
        }
    }

    init<Content: View>(
        message: ChatMessage,
        timeTitle: String,
        @ViewBuilder assistantContent: () -> Content
    ) {
        self.message = message
        self.timeTitle = timeTitle
        self.assistantContent = AnyView(assistantContent())
    }

    var body: some View {
        switch message.type {
        case .user:
            userMessage
        case .assistant:
            assistantMessage
        case .system:
            systemMessage
        }
    }

    private var userMessage: some View {
        HStack {
            Spacer(minLength: 56)

            messageContent
                .background(.tint, in: userBubbleShape)
        }
    }

    private var assistantMessage: some View {
        HStack {
            messageContent
                .background(Color(.secondarySystemBackground), in: assistantBubbleShape)

            Spacer(minLength: 40)
        }
    }

    private var systemMessage: some View {
        HStack(spacing: 12) {
            Rectangle()
                .fill(.secondary.opacity(0.25))
                .frame(height: 1)

            Text(message.text)
                .font(.caption.weight(.medium))
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: true, vertical: false)

            Rectangle()
                .fill(.secondary.opacity(0.25))
                .frame(height: 1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
    }

    private var messageContent: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(message.text)
                .foregroundStyle(message.type == .user ? .white : .primary)

            if message.type == .assistant {
                assistantContent
            }

            Text(timeTitle)
                .font(.caption2)
                .foregroundStyle(message.type == .user ? .black.opacity(0.75) : .secondary)
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding(14)
        .frame(maxWidth: 270, alignment: .leading)
    }

    private var userBubbleShape: UnevenRoundedRectangle {
        UnevenRoundedRectangle(
            topLeadingRadius: 16,
            bottomLeadingRadius: 16,
            bottomTrailingRadius: 4,
            topTrailingRadius: 16
        )
    }

    private var assistantBubbleShape: UnevenRoundedRectangle {
        UnevenRoundedRectangle(
            topLeadingRadius: 16,
            bottomLeadingRadius: 4,
            bottomTrailingRadius: 16,
            topTrailingRadius: 16
        )
    }
}
