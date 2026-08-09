import SwiftUI
import UIKit

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
        case .saveMessage:
            saveMessage
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
                .multilineTextAlignment(.center)
                .layoutPriority(1)

            Rectangle()
                .fill(.secondary.opacity(0.25))
                .frame(height: 1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
    }

    private var saveMessage: some View {
        HStack(spacing: 12) {
            Rectangle()
                .stroke(style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                .foregroundStyle(.secondary.opacity(0.25))
                .frame(height: 1)

            Text(message.text)
                .font(.caption.weight(.medium))
                .foregroundStyle(Color(.endGradient))
                .multilineTextAlignment(.center)
                .layoutPriority(1)

            Rectangle()
                .stroke(style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                .foregroundStyle(.secondary.opacity(0.25))
                .frame(height: 1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
    }

    private var messageContent: some View {
        Group {
            if message.type == .user {
                messageContentBody
                    .frame(width: userBubbleWidth, alignment: .leading)
            } else {
                messageContentBody
                    .frame(maxWidth: 270, alignment: .leading)
            }
        }
    }

    private var messageContentBody: some View {
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
    }

    private var userBubbleWidth: CGFloat {
        let horizontalPadding: CGFloat = 28
        let maximumContentWidth = 270 - horizontalPadding
        let textWidth = maximumLineWidth(
            for: message.text,
            maxWidth: maximumContentWidth
        )
        let timeWidth = (timeTitle as NSString).size(
            withAttributes: [.font: UIFont.preferredFont(forTextStyle: .caption2)]
        ).width

        return min(
            max(max(textWidth, timeWidth) + horizontalPadding, 90),
            270
        )
    }

    private func maximumLineWidth(for text: String, maxWidth: CGFloat) -> CGFloat {
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.preferredFont(forTextStyle: .body)
        ]
        var maximumWidth: CGFloat = 0

        for paragraph in text.components(separatedBy: "\n") {
            var currentLine = ""

            for word in paragraph.split(separator: " ", omittingEmptySubsequences: true) {
                let candidate = currentLine.isEmpty
                    ? String(word)
                    : "\(currentLine) \(word)"
                let candidateWidth = (candidate as NSString).size(withAttributes: attributes).width

                if candidateWidth > maxWidth && !currentLine.isEmpty {
                    maximumWidth = max(
                        maximumWidth,
                        (currentLine as NSString).size(withAttributes: attributes).width
                    )
                    currentLine = String(word)
                } else {
                    currentLine = candidate
                }
            }

            maximumWidth = max(
                maximumWidth,
                (currentLine as NSString).size(withAttributes: attributes).width
            )
        }

        return maximumWidth
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
