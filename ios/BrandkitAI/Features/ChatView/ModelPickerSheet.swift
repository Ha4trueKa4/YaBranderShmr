import SwiftUI

struct ModelPickerSheet: View {
    let viewModel: ChatViewModel

    @Environment(\.dismiss) private var dismiss
    @State private var pendingSelectionID: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Choose a model")
                .font(.title2.bold())

            Text("Select the model that will generate your brandkit assets.")
                .font(.body)
                .foregroundStyle(.secondary)
                .padding(.top, 4)

            Group {
                if viewModel.isLoadingModels && viewModel.availableModels.isEmpty {
                    ProgressView("Loading models…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.modelsLoadFailed && viewModel.availableModels.isEmpty {
                    unavailableState
                } else {
                    modelList
                }
            }
            .padding(.top, 28)
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .presentationDetents([.height(460)])
        .presentationDragIndicator(.visible)
        .presentationBackground(Color(.background))
    }

    private var modelList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(viewModel.availableModels) { model in
                    option(model)
                }
            }
        }
        .scrollIndicators(.hidden)
    }

    private func option(_ model: GenerationModel) -> some View {
        let selectedModelID = pendingSelectionID ?? viewModel.selectedModel.id
        let isSelected = model.id == selectedModelID
        let cardShape = RoundedRectangle(cornerRadius: 18, style: .circular)

        return Button {
            guard pendingSelectionID == nil else { return }

            withAnimation(.easeInOut(duration: 0.08)) {
                pendingSelectionID = model.id
                viewModel.selectModel(model)
            }

            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(100))
                dismiss()
            }
        } label: {
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(model.title)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(.primary)

                    Text(model.providerTitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Spacer(minLength: 8)

                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color.accentColor)
                        .frame(width: 24, height: 24)
                } else {
                    Circle()
                        .stroke(Color.secondary.opacity(0.25), lineWidth: 1)
                        .frame(width: 24, height: 24)
                }
            }
            .padding(.horizontal, 18)
            .frame(minHeight: 76)
            .background(Color(.secondarySystemBackground), in: cardShape)
            .overlay {
                cardShape.strokeBorder(
                    isSelected ? Color.accentColor : Color.secondary.opacity(0.16),
                    lineWidth: isSelected ? 1.5 : 1
                )
            }
            .contentShape(cardShape)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private var unavailableState: some View {
        ContentUnavailableView {
            Label("Models unavailable", systemImage: "exclamationmark.triangle")
        } description: {
            Text("Check your connection and try again.")
        } actions: {
            Button("Retry") {
                Task { await viewModel.loadModels(force: true) }
            }
            .buttonStyle(.borderedProminent)
        }
    }
}
