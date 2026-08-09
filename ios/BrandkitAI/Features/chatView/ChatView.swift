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

}

#Preview {
    NavigationStack {
        VStack {}
        .navigationDestination(isPresented: .constant(true)) {
            ChatView(title: "Test chat")
                .background(Color.background)
        }
    }
}
