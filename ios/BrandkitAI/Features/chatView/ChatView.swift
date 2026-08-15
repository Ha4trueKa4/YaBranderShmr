//
//  ChatView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 09.08.2026.
//

import SwiftUI

struct ChatView: View {
    let title: String
    var onBack: () -> Void = {}

    @State private var viewModel = ChatViewModel()
    @State private var messageText = ""
    @State private var didFinishInitialLoad = false
    @FocusState private var isInputFocused: Bool

    private let bottomAnchor = "chat-bottom"

    var body: some View {
        ScrollViewReader { proxy in
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

                    Color.clear
                        .frame(height: 1)
                        .id(bottomAnchor)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .top)
            }
            .scrollDismissesKeyboard(.interactively)
            .defaultScrollAnchor(.bottom)
            .task {
                viewModel.load()
                await Task.yield()
                scrollToBottom(using: proxy, animated: false)
                didFinishInitialLoad = true
            }
            .onChange(of: viewModel.lastMessageID) { _, newMessageID in
                guard didFinishInitialLoad, newMessageID != nil else { return }

                Task { @MainActor in
                    await Task.yield()
                    scrollToBottom(using: proxy, animated: true)
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                inputBar(using: proxy)
            }
        }
            .customNavigationBar(title: title, onBack: onBack)
            .safeAreaInset(edge: .top) {
                Rectangle()
                    .foregroundStyle(.border)
                    .frame(height: 1)
                    .background(Color.background)
            }
    }

    private func inputBar(using proxy: ScrollViewProxy) -> some View {
        HStack(spacing: 12) {
            TextField("Ask brand assistant...", text: $messageText, axis: .vertical)
                .font(.subheadline)
                .lineLimit(1...4)
                .focused($isInputFocused)
                .submitLabel(.send)
                .onSubmit {
                    sendMessage()
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemBackground), in: Capsule())

            Button {
                sendMessage()
            } label: {
                Image(systemName: "arrow.up")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 40, height: 40)
                    .background(.tint, in: Circle())
            }
            .opacity(canSendMessage ? 1 : 0.5)
            .disabled(!canSendMessage)
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

    private var canSendMessage: Bool {
        !messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func sendMessage() {
        guard canSendMessage else { return }

        viewModel.sendMessage(messageText)
        messageText = ""
    }

    private func scrollToBottom(using proxy: ScrollViewProxy, animated: Bool) {
        if animated {
            withAnimation(.easeOut(duration: 0.25)) {
                proxy.scrollTo(bottomAnchor, anchor: .bottom)
            }
        } else {
            proxy.scrollTo(bottomAnchor, anchor: .bottom)
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
