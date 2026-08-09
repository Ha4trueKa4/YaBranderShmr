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
    private(set) var sections: [MessageSection] = []

    var lastMessageID: ChatMessage.ID? {
        sections.last?.messages.last?.id
    }

    func load() {
        guard messages.isEmpty else { return }
        replaceMessages(Self.mocks)
    }

    func sendMessage(_ text: String) {
        let text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        appendMessage(
            ChatMessage(
                type: .user,
                text: text,
                date: Date()
            )
        )
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

    private func replaceMessages(_ messages: [ChatMessage]) {
        self.messages = messages
        sections = makeSections(from: messages)
    }

    private func appendMessage(_ message: ChatMessage) {
        messages.append(message)

        let calendar = Calendar.current
        let sectionDate = calendar.startOfDay(for: message.date)

        guard let sectionIndex = sections.firstIndex(where: { $0.date == sectionDate }) else {
            sections.append(MessageSection(date: sectionDate, messages: [message]))
            sections.sort { $0.date < $1.date }
            return
        }

        var sectionMessages = sections[sectionIndex].messages
        sectionMessages.append(message)
        sectionMessages.sort { $0.date < $1.date }
        sections[sectionIndex] = MessageSection(
            date: sectionDate,
            messages: sectionMessages
        )
    }

    private func makeSections(from messages: [ChatMessage]) -> [MessageSection] {
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

    private static var mocks: [ChatMessage] {
        let calendar = Calendar.current
        let now = Date()

        func today(hour: Int, minute: Int) -> Date {
            calendar.date(bySettingHour: hour, minute: minute, second: 0, of: now) ?? now
        }

        func yesterday(hour: Int, minute: Int) -> Date {
            calendar.date(byAdding: .day, value: -1, to: today(hour: hour, minute: minute)) ?? now
        }

        return [
            ChatMessage(
                type: .system,
                text: "AI CHAT ASSISTANT",
                date: yesterday(hour: 18, minute: 5)
            ),
            ChatMessage(
                type: .user,
                text: "Подготовь несколько вариантов цветовой палитры",
                date: yesterday(hour: 18, minute: 6)
            ),
            ChatMessage(
                type: .assistant,
                text: "Собрал палитру с акцентом на космические оттенки.",
                date: yesterday(hour: 18, minute: 7),
                attachment: .mockImage
            ),
            ChatMessage(
                type: .saveMessage,
                text: "VERSION 1.1 SAVED",
                date: yesterday(hour: 18, minute: 8)
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
            ),
            ChatMessage(
                type: .user,
                text: "Сделай фон темнее и добавь немного космического фиолетового свечения",
                date: today(hour: 11, minute: 44)
            ),
            ChatMessage(
                type: .assistant,
                text: "Готово. Затемнил фон и добавил мягкое свечение по краям композиции.",
                date: today(hour: 11, minute: 45)
            ),
            ChatMessage(
                type: .user,
                text: "Поменяй шрифт заголовка на современный геометричный sans-serif",
                date: today(hour: 11, minute: 46)
            ),
            ChatMessage(
                type: .assistant,
                text: "Заменил шрифт и обновил расстояние между заголовком и описанием.",
                date: today(hour: 11, minute: 47),
                attachment: .mockImage
            ),
            ChatMessage(
                type: .saveMessage,
                text: "VERSION 1.3 SAVED",
                date: today(hour: 11, minute: 48)
            ),
            ChatMessage(
                type: .user,
                text: "Покажи финальный вариант с более контрастными кнопками",
                date: today(hour: 11, minute: 49)
            ),
            ChatMessage(
                type: .assistant,
                text: "Подготовил финальный вариант с контрастными кнопками и обновлённым состоянием наведения.",
                date: today(hour: 11, minute: 50),
                attachment: .mockImage
            ),
            ChatMessage(
                type: .user,
                text: "Отлично, сохрани этот вариант",
                date: today(hour: 11, minute: 51)
            ),
            ChatMessage(
                type: .assistant,
                text: "Вариант сохранён. Теперь он доступен в истории проекта.",
                date: today(hour: 11, minute: 52)
            ),
            ChatMessage(
                type: .saveMessage,
                text: "VERSION 1.4 SAVED",
                date: today(hour: 11, minute: 53)
            )
        ]
    }
}
