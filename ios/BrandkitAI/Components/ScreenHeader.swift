//
//  ScreenHeader.swift
//  BrandkitAI
//

import SwiftUI


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
    .tint(Color.accentColor)
    .preferredColorScheme(.dark)
}
