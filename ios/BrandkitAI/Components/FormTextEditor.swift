import SwiftUI

/// Многострочная версия `FormTextField`: та же подложка и подпись, но текст
/// вводится в `TextEditor`. Плейсхолдера у него нет, поэтому текст лежит
/// подложкой и скрывается при вводе.
struct FormTextEditor: View {
    let label: String
    let placeholder: String
    @Binding var text: String

    var minHeight: CGFloat = 180
    var errorMessage: String?

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.footnote)
                .foregroundStyle(
                    errorMessage == nil
                        ? AnyShapeStyle(.secondary)
                        : AnyShapeStyle(Color(.error))
                )

            TextEditor(text: $text)
                .focused($isFocused)
                .scrollContentBackground(.hidden)
                .frame(minHeight: minHeight, alignment: .topLeading)
                .padding(.horizontal, 12)
                .padding(.vertical, 12)
                .background(alignment: .topLeading) {
                    if text.isEmpty {
                        Text(placeholder)
                            .foregroundStyle(.tertiary)
                            .padding(.horizontal, 17)
                            .padding(.vertical, 20)
                            .allowsHitTesting(false)
                    }
                }
                .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 12))
                .overlay {
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(
                            errorMessage != nil
                                ? AnyShapeStyle(Color(.error))
                                : isFocused
                                    ? AnyShapeStyle(.tint)
                                    : AnyShapeStyle(Color(.separator)),
                            lineWidth: 1
                        )
                }
                .animation(.easeOut(duration: 0.15), value: isFocused)
                .contentShape(.rect)
                .onTapGesture { isFocused = true }

            if let errorMessage {
                Text(errorMessage)
                    .font(.caption)
                    .foregroundStyle(Color(.error))
                    .accessibilityElement(children: .combine)
            }
        }
    }
}

#Preview {
    @Previewable @State var prompt = ""

    FormTextEditor(
        label: "PROMPT",
        placeholder: "Describe the brandkit you want to create.",
        text: $prompt
    )
    .padding(24)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .preferredColorScheme(.dark)
}
