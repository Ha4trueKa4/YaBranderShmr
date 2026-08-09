import SwiftUI

/// Многострочная версия `FormTextField`: та же подложка и подпись, но текст
/// вводится в `TextEditor`. Плейсхолдера у него нет, поэтому текст лежит
/// подложкой и скрывается при вводе.
struct FormTextEditor: View {
    let label: String
    let placeholder: String
    @Binding var text: String

    var minHeight: CGFloat = 180

    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.footnote)
                .foregroundStyle(.secondary)

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
                            isFocused ? AnyShapeStyle(.tint) : AnyShapeStyle(Color(.separator)),
                            lineWidth: 1
                        )
                }
                .animation(.easeOut(duration: 0.15), value: isFocused)
                .contentShape(.rect)
                .onTapGesture { isFocused = true }
        }
    }
}

#Preview {
    @Previewable @State var brief = ""

    FormTextEditor(
        label: "BRAND DESCRIPTION & BRIEF",
        placeholder: "Describe your brand values, target audience, preferred color scheme, visual mood.",
        text: $brief
    )
    .padding(24)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .preferredColorScheme(.dark)
}
