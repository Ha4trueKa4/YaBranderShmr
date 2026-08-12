//
//  PrimaryButton.swift
//  BrandkitAI
//

import SwiftUI

struct PrimaryButton: View {
    let title: String
    var isEnabled: Bool = true
    var fill: AnyShapeStyle = AnyShapeStyle(.tint)
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
        }
        .buttonStyle(PrimaryButtonStyle(fill: fill))
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.5)
    }
}

private struct PrimaryButtonStyle: ButtonStyle {
    let fill: AnyShapeStyle

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(fill, in: .rect(cornerRadius: 12))
            .opacity(configuration.isPressed ? 0.85 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

struct LinkButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.tint)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack(spacing: 16) {
        PrimaryButton(title: "Sign in") {}
        LinkButton(title: "Forgot password?") {}
    }
    .padding(24)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .tint(.purple)
    .preferredColorScheme(.dark)
}
