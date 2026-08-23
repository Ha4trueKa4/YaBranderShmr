//
//  FormScreenLayout.swift
//  BrandkitAI
//

import SwiftUI

struct FormScreenLayout<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                    .frame(height: 72)

                content

                Spacer(minLength: 40)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)
        }
        .scrollBounceBehavior(.basedOnSize)
        .scrollDismissesKeyboard(.interactively)
        .background {
            LinearGradient(
                stops: [
                    .init(color: .startGradient.opacity(0.8), location: 0),
                    .init(color: .clear, location: 0.6),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        }
        .brandBackground()
        .onTapGesture {
            UIApplication.shared.sendAction(
                #selector(UIResponder.resignFirstResponder),
                to: nil,
                from: nil,
                for: nil
            )
        }
    }
}
