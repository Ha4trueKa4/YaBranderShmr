//
//  ChatView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 09.08.2026.
//

import SwiftUI

struct ChatView: View {
    let projectID: UUID
    let title: String
    var onBack: () -> Void = {}
    var onOpenImage: (UUID, Date?) -> Void = { _, _ in }

    @State private var viewModel: ChatViewModel
    @State private var messageText = ""
    @State private var didFinishInitialLoad = false
    @State private var isPrivacyPickerPresented = false
    @State private var selectedPrivacy = ChatViewModel.ProjectPrivacy.privateProject
    @FocusState private var isInputFocused: Bool

    init(
        projectID: UUID = UUID(),
        title: String,
        onBack: @escaping () -> Void = {},
        onOpenImage: @escaping (UUID, Date?) -> Void = { _, _ in },
        service: any ProjectServiceProtocol = ProjectService(),
        viewModel: ChatViewModel? = nil
    ) {
        let resolvedViewModel = viewModel ?? ChatViewModel(projectID: projectID, service: service)
        self.projectID = resolvedViewModel.projectID
        self.title = title
        self.onBack = onBack
        self.onOpenImage = onOpenImage
        _viewModel = State(initialValue: resolvedViewModel)
    }

    private let bottomAnchor = "chat-bottom"

    var body: some View {
        ScrollViewReader { proxy in
            Group {
                if viewModel.loadFailed && viewModel.messages.isEmpty {
                    loadFailedState
                } else {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 20) {
                            if viewModel.isLoading && viewModel.messages.isEmpty {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                            }

                            if viewModel.isOffline {
                                offlinePill
                            }

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
                                    case .version(let versionID, let url):
                                        GeometryReader { _ in
                                            SVGDocumentView(
                                                source: .url(
                                                    url,
                                                    cacheKey: SVGCache.versionKey(projectID: projectID, versionID: versionID),
                                                    isPrivate: true
                                                ),
                                                refreshURL: {
                                                    try await viewModel.versionURL(for: versionID)
                                                },
                                                onTap: {
                                                    onOpenImage(versionID, message.date)
                                                }
                                            )
                                        }
                                        .aspectRatio(1, contentMode: .fit)
                                        .frame(maxWidth: .infinity)
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .contentShape(RoundedRectangle(cornerRadius: 10))
                                    case .error:
                                        EmptyView()
                                    case .cancelled:
                                        EmptyView()
                                    case .none:
                                        EmptyView()
                                    }
                                }
                            }
                        }
                    }

                    if viewModel.isGenerating {
                        HStack {
                            HStack(spacing: 10) {
                                ProgressView()
                                    .controlSize(.small)
                                Text(viewModel.isCancelling ? "Cancelling…" : "Generating…")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(14)
                            .frame(maxWidth: 270, alignment: .leading)
                            .background(
                                Color(.secondarySystemBackground),
                                in: UnevenRoundedRectangle(
                                    topLeadingRadius: 16,
                                    bottomLeadingRadius: 4,
                                    bottomTrailingRadius: 16,
                                    topTrailingRadius: 16
                                )
                            )

                            Spacer(minLength: 40)
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
                }
            }
            .task {
                await viewModel.load()
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
            .onChange(of: viewModel.isGenerating) { _, isGenerating in
                guard didFinishInitialLoad, isGenerating else { return }

                Task { @MainActor in
                    await Task.yield()
                    scrollToBottom(using: proxy, animated: true)
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if !(viewModel.loadFailed && viewModel.messages.isEmpty) {
                    inputBar(using: proxy)
                }
            }
        }
            .customNavigationBar(title: title, onBack: onBack)
            .toolbar {
                privacyToolbarItem
            }
            .safeAreaInset(edge: .top) {
                Rectangle()
                    .foregroundStyle(.border)
                    .frame(height: 1)
                    .background(Color.background)
            }
            .sheet(isPresented: $isPrivacyPickerPresented) {
                PrivacyPicker(selection: $selectedPrivacy) { newPrivacy in
                    Task { await viewModel.updatePrivacy(newPrivacy) }
                }
            }
    }

    @ToolbarContentBuilder
    private var privacyToolbarItem: some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarItem(placement: .topBarTrailing) {
                privacyButton
            }
            .sharedBackgroundVisibility(.hidden)
        } else {
            ToolbarItem(placement: .topBarTrailing) {
                privacyButton
            }
        }
    }

    private var privacyButton: some View {
        Button {
            selectedPrivacy = viewModel.privacy
            isPrivacyPickerPresented = true
        } label: {
            Text(viewModel.privacy.shortTitle)
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 12)
                .frame(height: 34)
                .background(Color(.secondarySystemBackground), in: Capsule())
                .overlay {
                    Capsule()
                        .stroke(Color(.border), lineWidth: 1)
                }
        }
        .buttonStyle(.plain)
        .foregroundStyle(.primary)
        .accessibilityLabel("Project visibility: \(viewModel.privacy.shortTitle)")
        .accessibilityHint("Opens project visibility settings")
    }

    private var offlinePill: some View {
        Button {
            Task { await viewModel.retry() }
        } label: {
            Label("Offline — showing saved messages", systemImage: "wifi.slash")
                .font(.caption2.weight(.medium))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(.secondarySystemBackground), in: Capsule())
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity)
        .accessibilityHint("Tap to retry")
    }

    private var loadFailedState: some View {
        VStack(spacing: 12) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 28, weight: .medium))
                .foregroundStyle(.secondary)

            Text("Unable to load the chat")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Button("Retry") {
                Task { await viewModel.retry() }
            }
            .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(24)
    }

    private func inputBar(using proxy: ScrollViewProxy) -> some View {
        VStack(spacing: 8) {
            if let inputErrorMessage {
                Label(inputErrorMessage, systemImage: "exclamationmark.circle")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
            }

            if viewModel.pendingCount > 0 {
                Label(
                    viewModel.pendingCount == 1
                        ? "1 message pending — will send when the network is back."
                        : "\(viewModel.pendingCount) messages pending — will send when the network is back.",
                    systemImage: "clock.arrow.circlepath"
                )
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
            }

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
                    if viewModel.isGenerating {
                        Task { await viewModel.cancelGeneration() }
                    } else {
                        sendMessage()
                    }
                } label: {
                    Group {
                        if viewModel.isCancelling {
                            ProgressView()
                                .tint(Color(.onAccent))
                        } else if viewModel.isGenerating {
                            Image(systemName: "stop.fill")
                                .font(.system(size: 17, weight: .semibold))
                        } else {
                            Image(systemName: "arrow.up")
                                .font(.system(size: 17, weight: .semibold))
                        }
                    }
                    .foregroundStyle(Color(.onAccent))
                    .frame(width: 40, height: 40)
                    .background(.tint, in: Circle())
                }
                .opacity(viewModel.isGenerating ? 1 : (canSendMessage ? 1 : 0.5))
                .disabled(viewModel.isCancelling || (!viewModel.isGenerating && !canSendMessage))
                .accessibilityLabel(viewModel.isGenerating ? "Cancel generation" : "Send message")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.background)
            .overlay(alignment: .top) {
                Rectangle()
                    .fill(.border)
                    .frame(height: 1)
            }
        }
        .background(Color.background)
        .onChange(of: messageText) {
            viewModel.clearSendError()
        }
    }

    private var canSendMessage: Bool {
        let text = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        return !viewModel.isGenerating
            && !text.isEmpty
            && text.count <= ChatViewModel.maxPromptLength
    }

    private var inputErrorMessage: String? {
        let text = messageText.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.count > ChatViewModel.maxPromptLength {
            return "Message is too long (maximum 5000 characters)."
        }
        return viewModel.sendErrorMessage
    }

    private func sendMessage() {
        guard canSendMessage else { return }

        let text = messageText
        messageText = ""
        Task {
            await viewModel.sendMessage(text)
        }
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
            ChatView(
                title: "Test chat",
                viewModel: ChatViewModel.makePreview()
            )
            .background(Color.background)
        }
    }
}
