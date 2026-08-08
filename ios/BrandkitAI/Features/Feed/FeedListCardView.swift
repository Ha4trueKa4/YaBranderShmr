//
//  FeedListCardView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 08.08.2026.
//

import SwiftUI

struct FeedListCardView: View {
    let title: String
    let accountImage: Image
    let accountAvatar: Image
    let accountName: String
    let onAccountTap: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            accountImage
                .resizable()
                .scaledToFit()

            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)

                HStack(spacing: 8) {
                    accountAvatar
                        .resizable()
                        .scaledToFill()
                        .frame(width: 24, height: 24)
                        .clipShape(Circle())

                    Text(accountName)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                .onTapGesture { onAccountTap() }
            }
            .padding(12)
        }
        .alternateBackground(cornerSize: CGSize(width: 16, height: 16))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
