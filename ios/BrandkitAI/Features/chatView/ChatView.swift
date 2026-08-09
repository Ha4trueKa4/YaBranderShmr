//
//  ChatView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 09.08.2026.
//

import SwiftUI

struct ChatView: View {
    let title: String

    @State private var viewModel = ChatViewModel()
    @State private var messageText = ""

    private var backButton: some View {
        Button {
        }
        label: {
            Image(systemName: "arrow.left")
        }
    }
    
    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 20) {
                ForEach(viewModel.sections) { section in
                    VStack(alignment: .leading, spacing: 12) {
                        Text(viewModel.dateTitle(for: section.date))
                            .font(.caption.weight(.medium))
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity)

                        ForEach(section.messages) { message in
                            ChatMessageView(
                                message: message,
                                timeTitle: viewModel.timeTitle(for: message.date)
                            ) {
                                switch message.attachment {
                                case .mockImage:
                                    ChatImageMockView()
                                case .none:
                                    EmptyView()
                                }
                            }
                        }
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .task {
            viewModel.load()
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            inputBar
        }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden()
            .safeAreaInset(edge: .top) {
                Rectangle()
                    .foregroundStyle(.border)
                    .frame(height: 1)
                    .background(Color.background)
            }
            .toolbar {
                if #available(iOS 26.0, *) {
                    ToolbarItem(placement: .topBarLeading) {
                        backButton
                    }
                    .sharedBackgroundVisibility(.hidden)
                } else {
                    ToolbarItem(placement: .topBarLeading) {
                        backButton
                    }
                }
            }
    }

    private var inputBar: some View {
        HStack(spacing: 12) {
            TextField("Ask brand assistant...", text: $messageText, axis: .vertical)
                .font(.subheadline)
                .lineLimit(1...4)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemBackground), in: Capsule())

            Button {
                messageText = ""
            } label: {
                Image(systemName: "arrow.up")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 40, height: 40)
                    .background(.tint, in: Circle())
            }
            .accessibilityLabel("Send message")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.background)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(.separator)
                .frame(height: 1)
        }
    }

}

#Preview {
    NavigationStack {
        VStack {}
        .navigationDestination(isPresented: .constant(true)) {
            ChatView(title: "Test chat")
                .background(Color.background)
                .preferredColorScheme(.dark)
        }
    }
}
