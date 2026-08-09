import Foundation

@MainActor
@Observable
final class ChatViewModel {
    struct MessageSection: Identifiable {
        let date: Date
        let messages: [ChatMessage]

        var id: Date { date }
    }

    private(set) var messages: [ChatMessage] = []

    var sections: [MessageSection] {
        let calendar = Calendar.current
        let groupedMessages = Dictionary(grouping: messages) {
            calendar.startOfDay(for: $0.date)
        }

        return groupedMessages.keys.sorted().map { date in
            MessageSection(
                date: date,
                messages: groupedMessages[date, default: []].sorted { $0.date < $1.date }
            )
        }
    }

    func load() {
        guard messages.isEmpty else { return }
        messages = Self.mocks
    }

    func dateTitle(for date: Date) -> String {
        let calendar = Calendar.current

        if calendar.isDateInToday(date) {
            return "Today"
        }

        if calendar.isDateInYesterday(date) {
            return "Yesterday"
        }

        return date.formatted(.dateTime.month(.abbreviated).day().year())
    }

    func timeTitle(for date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }

    private static var mocks: [ChatMessage] {
        let calendar = Calendar.current
        let now = Date()

        func today(hour: Int, minute: Int) -> Date {
            calendar.date(bySettingHour: hour, minute: minute, second: 0, of: now) ?? now
        }

        return [
            ChatMessage(
                type: .system,
                text: "AI CHAT ASSISTANT",
                date: today(hour: 11, minute: 40)
            ),
            ChatMessage(
                type: .user,
                text: "Хочу создать новый фирменный стиль",
                date: today(hour: 11, minute: 41)
            ),
            ChatMessage(
                type: .assistant,
                text: "Настроил баланс цветов и подготовил первый вариант.",
                date: today(hour: 11, minute: 42),
                attachment: .mockImage
            ),
            ChatMessage(
                type: .saveMessage,
                text: "VERSION 1.2 SAVED",
                date: today(hour: 11, minute: 43)
            )
        ]
    }
}
