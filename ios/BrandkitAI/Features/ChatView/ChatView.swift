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
    @State private var isModelPickerPresented = false
    @State private var isModelControlVisible = false
    @State private var isComposerExpanded = false
    @State private var selectedPrivacy = ChatViewModel.ProjectPrivacy.privateProject
    @State private var isRestoringText = false
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

                    ForEach(viewModel.sections) { section in
                        VStack(alignment: .leading, spacing: 12) {
                            Text(viewModel.dateTitle(for: section.date))
                                .font(.caption.weight(.medium))
                                .foregroundStyle(.secondary)
                                .frame(maxWidth: .infinity)

                            ForEach(section.messages) { message in
                                ChatMessageView(
                                    message: message,
                                    timeTitle: viewModel.timeTitle(for: message.date),
                                    onRetry: {
                                        Task {
                                            await viewModel.retryFailedMessage(id: message.id)
                                        }
                                    }
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
                }
            }
            .task {
                await viewModel.load()
                await Task.yield()
                scrollToBottom(using: proxy, animated: false)
                didFinishInitialLoad = true
            }
            .task {
                await viewModel.waitForViewRemoval()
            }
            .onAppear {
                // Если restorePrompt установили, пока вьюха была закрыта
                // другим экраном, onChange не сработал — применяем теперь.
                applyPendingRestorePrompt()
            }
            .onChange(of: viewModel.restorePrompt) { _, prompt in
                guard let prompt else { return }
                isRestoringText = true
                messageText = prompt
                viewModel.consumeRestorePrompt()
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
            .toolbarBackground(Color.background, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                privacyToolbarItem
            }
            .safeAreaInset(edge: .top) {
                VStack(spacing: 0) {
                    Rectangle()
                        .foregroundStyle(.border)
                        .frame(height: 1)

                    if viewModel.isOffline {
                        offlinePill
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                    }
                }
            }
            .sheet(isPresented: $isPrivacyPickerPresented) {
                PrivacyPicker(selection: $selectedPrivacy) { newPrivacy in
                    Task { await viewModel.updatePrivacy(newPrivacy) }
                }
            }
            .sheet(isPresented: $isModelPickerPresented) {
                ModelPickerSheet(viewModel: viewModel)
            }
            .task {
                await viewModel.loadModels()
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

            composer
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
        .animation(.easeInOut(duration: 0.2), value: isComposerExpanded)
        .onChange(of: messageText) {
            if isRestoringText {
                isRestoringText = false
            } else {
                viewModel.clearSendError()
            }
        }
    }

    private var composer: some View {
        VStack(alignment: .leading, spacing: isComposerExpanded ? 6 : 0) {
            TextField("Ask brand assistant...", text: $messageText, axis: .vertical)
                .font(.subheadline)
                .lineLimit(1...4)
                .focused($isInputFocused)
                .submitLabel(.send)
                .onSubmit {
                    sendMessage()
                }
                .padding(.leading, 8)
                .padding(.trailing, isComposerExpanded ? 8 : 50)
                .padding(.vertical, 10)

            if isComposerExpanded {
                HStack(spacing: 10) {
                    if isModelControlVisible {
                        modelButton
                            .transition(.opacity)
                    }

                    Spacer(minLength: 12)

                    Color.clear
                        .frame(width: 40, height: 40)
                }
                .transition(.move(edge: .bottom))
            }
        }
        .padding(.leading, 8)
        .padding(.trailing, 6)
        .padding(.vertical, 6)
        .background(
            Color(.secondarySystemBackground),
            in: RoundedRectangle(cornerRadius: isComposerExpanded ? 26 : 24, style: .continuous)
        )
        .overlay(alignment: .bottomTrailing) {
            sendButton
                .padding(6)
        }
        .overlay {
            RoundedRectangle(cornerRadius: isComposerExpanded ? 26 : 24, style: .continuous)
                .stroke(Color(.border), lineWidth: 1)
                .allowsHitTesting(false)
        }
        .animation(.easeInOut(duration: 0.2), value: isComposerExpanded)
        .task(id: isInputFocused) {
            if isInputFocused {
                isModelControlVisible = false
                isComposerExpanded = true

                try? await Task.sleep(for: .milliseconds(220))
                guard !Task.isCancelled, isInputFocused else { return }

                withAnimation(.easeOut(duration: 0.12)) {
                    isModelControlVisible = true
                }
                return
            }

            withAnimation(.easeOut(duration: 0.1)) {
                isModelControlVisible = false
            }

            try? await Task.sleep(for: .milliseconds(120))
            guard !Task.isCancelled, !isInputFocused else { return }
            isComposerExpanded = false
        }
    }

    private var modelButton: some View {
        Button {
            isModelPickerPresented = true
        } label: {
            HStack(spacing: 5) {
                Text(viewModel.selectedModel.title)
                    .lineLimit(1)

                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .bold))
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.primary)
            .padding(.horizontal, 10)
            .frame(height: 36)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Model, \(viewModel.selectedModel.title)")
        .accessibilityHint("Opens model picker")
    }

    private enum SendButtonAction {
        case cancelGeneration
        case sendMessage
        case restoreUnsent
    }

    /// Единый приоритет для кнопки и Enter: набранный текст всегда
    /// отправляется, восстановление незавершённого сообщения — только
    /// при пустом поле ввода.
    private var sendButtonAction: SendButtonAction {
        if viewModel.isGenerating { return .cancelGeneration }
        if canSendMessage { return .sendMessage }
        if viewModel.hasUnsentMessage { return .restoreUnsent }
        return .sendMessage
    }

    private var sendButton: some View {
        Button {
            switch sendButtonAction {
            case .cancelGeneration:
                Task { await viewModel.cancelGeneration() }
            case .sendMessage:
                sendMessage()
            case .restoreUnsent:
                isInputFocused = false
                Task { await viewModel.restoreUnsentMessage() }
            }
        } label: {
            Group {
                if viewModel.isCancelling {
                    ProgressView()
                        .tint(Color(.onAccent))
                } else if sendButtonAction == .cancelGeneration
                    || sendButtonAction == .restoreUnsent {
                    Image(systemName: "stop.fill")
                        .font(.system(size: 17, weight: .semibold))
                } else {
                    Image(systemName: "arrow.up")
                        .font(.system(size: 17, weight: .semibold))
                }
            }
            .foregroundStyle(
                hasStopAction || canSendMessage
                    ? Color(.onAccent)
                    : Color(.secondaryLabel)
            )
            .frame(width: 40, height: 40)
            .background(
                hasStopAction || canSendMessage
                    ? Color.accentColor
                    : Color(.tertiarySystemFill),
                in: Circle()
            )
        }
        .buttonStyle(.plain)
        .disabled(viewModel.isCancelling || (!hasStopAction && !canSendMessage))
        .accessibilityLabel(sendButtonAccessibilityLabel)
    }

    private var hasStopAction: Bool {
        viewModel.isGenerating || viewModel.hasUnsentMessage
    }

    private var sendButtonAccessibilityLabel: String {
        switch sendButtonAction {
        case .cancelGeneration: return "Cancel generation"
        case .sendMessage: return "Send message"
        case .restoreUnsent: return "Edit unsent message"
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

    private func applyPendingRestorePrompt() {
        guard let prompt = viewModel.restorePrompt else { return }
        isRestoringText = true
        messageText = prompt
        viewModel.consumeRestorePrompt()
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
