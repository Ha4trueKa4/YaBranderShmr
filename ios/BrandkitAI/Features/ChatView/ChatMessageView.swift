import SwiftUI
import UIKit

struct ChatMessageView: View {
    let message: ChatMessage
    let timeTitle: String
    private let assistantContent: AnyView
    private let onRetry: (() -> Void)?

    /// Кеш ширин статус-бейджей доставки: вариантов текста всего два
    /// («Pending»/«Not sent»), а замер через NSString/UIImage раньше
    /// выполнялся на каждый ререндер каждого баббла. Ключ включает кегль
    /// шрифта, чтобы кеш инвалидовался при смене Dynamic Type.
    private struct DeliveryStatusWidthKey: Hashable {
        let title: String
        let systemImage: String
        let fontPointSize: CGFloat
    }

    private static var deliveryStatusWidthCache: [DeliveryStatusWidthKey: CGFloat] = [:]

    init(
        message: ChatMessage,
        timeTitle: String,
        onRetry: (() -> Void)? = nil
    ) {
        self.init(message: message, timeTitle: timeTitle, onRetry: onRetry) {
            EmptyView()
        }
    }

    init<Content: View>(
        message: ChatMessage,
        timeTitle: String,
        onRetry: (() -> Void)? = nil,
        @ViewBuilder assistantContent: () -> Content
    ) {
        self.message = message
        self.timeTitle = timeTitle
        self.onRetry = onRetry
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
            if case .error = message.attachment {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "exclamationmark.circle")
                        .foregroundStyle(.red)
                    Text(message.text)
                        .foregroundStyle(.primary)
                }
            } else if case .cancelled = message.attachment {
                HStack(alignment: .top, spacing: 6) {
                    Image(systemName: "stop.circle")
                        .foregroundStyle(.secondary)
                    Text(message.text)
                        .foregroundStyle(.secondary)
                }
            } else {
                Text(message.text)
                    .foregroundStyle(message.type == .user ? .white : .primary)
            }

            if message.isPending {
                HStack(spacing: 4) {
                    Image(systemName: "clock.arrow.circlepath")
                    Text("Pending")
                }
                .font(.caption2.weight(.medium))
                .foregroundStyle(message.type == .user ? .white.opacity(0.8) : .secondary)
            } else if message.isFailed {
                Button {
                    onRetry?()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "exclamationmark.circle")
                        Text("Not sent")
                            .lineLimit(1)
                    }
                    .fixedSize(horizontal: true, vertical: false)
                }
                .buttonStyle(.plain)
                .disabled(onRetry == nil)
                .font(.caption2.weight(.medium))
                .foregroundStyle(message.type == .user ? .white.opacity(0.8) : .red)
                .accessibilityHint("Retries sending this message")
            }

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
        let statusWidth = deliveryStatusWidth

        return min(
            max(max(max(textWidth, timeWidth), statusWidth) + horizontalPadding, 90),
            270
        )
    }

    private var deliveryStatusWidth: CGFloat {
        let title: String
        let systemImage: String

        if message.isFailed {
            title = "Not sent"
            systemImage = "exclamationmark.circle"
        } else if message.isPending {
            title = "Pending"
            systemImage = "clock.arrow.circlepath"
        } else {
            return 0
        }

        let preferredFont = UIFont.preferredFont(forTextStyle: .caption2)
        let font = UIFont.systemFont(ofSize: preferredFont.pointSize, weight: .medium)
        let key = DeliveryStatusWidthKey(
            title: title,
            systemImage: systemImage,
            fontPointSize: font.pointSize
        )
        if let cached = Self.deliveryStatusWidthCache[key] {
            return cached
        }

        let textWidth = (title as NSString).size(withAttributes: [.font: font]).width
        let symbolConfiguration = UIImage.SymbolConfiguration(font: font)
        let iconWidth = UIImage(
            systemName: systemImage,
            withConfiguration: symbolConfiguration
        )?.size.width ?? font.pointSize

        let width = iconWidth + 4 + textWidth
        Self.deliveryStatusWidthCache[key] = width
        return width
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
