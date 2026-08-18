//
//  FormTextField.swift
//  BrandkitAI
//

import SwiftUI

struct FormTextField: View {
    let label: String
    let placeholder: String
    @Binding var text: String

    var isSecure: Bool = false
    var textContentType: UITextContentType?
    var keyboardType: UIKeyboardType = .default
    var autocapitalization: TextInputAutocapitalization = .never
    var submitLabel: SubmitLabel = .next
    var errorMessage: String?
    var onSubmit: (() -> Void)?

    @State private var isRevealed = false
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.footnote)
                .foregroundStyle(errorMessage == nil ? AnyShapeStyle(.secondary) : AnyShapeStyle(.red))

            HStack(spacing: 12) {
                inputField
                if isSecure {
                    revealButton
                }
            }
            .padding(.horizontal, 16)
            .frame(height: 52)
            .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 12))
            .overlay {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        errorMessage != nil
                            ? AnyShapeStyle(.red)
                            : isFocused ? AnyShapeStyle(.tint) : AnyShapeStyle(Color(.separator)),
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

    @ViewBuilder
    private var inputField: some View {
        Group {
            if isSecure && !isRevealed {
                SecureField(placeholder, text: $text)
            } else {
                TextField(placeholder, text: $text)
            }
        }
        .focused($isFocused)
        .textContentType(textContentType)
        .keyboardType(keyboardType)
        .submitLabel(submitLabel)
        .textInputAutocapitalization(autocapitalization)
        .autocorrectionDisabled()
        .onSubmit { onSubmit?() }
    }

    private var revealButton: some View {
        Button {
            isRevealed.toggle()
            if isFocused {
                DispatchQueue.main.async { isFocused = true }
            }
        } label: {
            Image(systemName: isRevealed ? "eye.slash" : "eye")
                .foregroundStyle(.secondary)
                .frame(width: 24, height: 24)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isRevealed ? "Hide password" : "Show password")
    }
}

#Preview {
    @Previewable @State var email = ""
    @Previewable @State var password = "supersecret"

    VStack(spacing: 16) {
        FormTextField(label: "Email", placeholder: "hello@brandkit.ai", text: $email)
        FormTextField(
            label: "Password",
            placeholder: "Enter your password",
            text: $password,
            isSecure: true
        )
    }
    .padding(24)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .tint(Color.accentColor)
    .preferredColorScheme(.dark)
}
