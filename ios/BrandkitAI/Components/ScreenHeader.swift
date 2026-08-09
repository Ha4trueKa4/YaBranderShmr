//
//  ScreenHeader.swift
//  BrandkitAI
//

import SwiftUI


/// TODO: заменить содержимое `overlay` на реальный логотип, когда появится ассет:
///       `Image("Logo").resizable().scaledToFit().padding(8)`
struct LogoBadge: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(.tint.opacity(0.2))
            .frame(width: 60, height: 60)
            .overlay {
                Image(systemName: "swift").resizable().scaledToFit().padding(8)
            }
            .accessibilityHidden(true)
    }
}

struct ScreenHeader: View {
    let title: String

    var body: some View {
        VStack(spacing: 20) {
            LogoBadge()

            Text(title)
                .font(.title.bold())
        }
    }
}

#Preview {
    ScreenHeader(title: "Sign in")
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .tint(.purple)
    .preferredColorScheme(.dark)
}
