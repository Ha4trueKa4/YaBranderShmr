import SwiftUI

struct ProjectCreateView: View {
    var onBack: (() -> Void)?
    var onGenerate: (Project) -> Void = { _ in }

    @State private var viewModel: ProjectCreateViewModel
    @State private var isModelPickerPresented = false

    init(
        onBack: (() -> Void)? = nil,
        onGenerate: @escaping (Project) -> Void = { _ in },
        service: any ProjectServiceProtocol = ProjectService()
    ) {
        self.onBack = onBack
        self.onGenerate = onGenerate
        _viewModel = State(initialValue: ProjectCreateViewModel(service: service))
    }

    var body: some View {
        @Bindable var viewModel = viewModel

        ScrollView {
            VStack(spacing: 24) {
                FormTextField(
                    label: "PROJECT NAME",
                    placeholder: "My Startup Brand",
                    text: $viewModel.name,
                    autocapitalization: .words
                )

                FormTextEditor(
                    label: "PROMPT",
                    placeholder: "Describe the brandkit you want to create — its style, colors, audience, mood, and required elements.",
                    text: $viewModel.prompt,
                    errorMessage: viewModel.promptValidationMessage
                )

                modelPickerButton

                FormHint(text: viewModel.hint)

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                PrimaryButton(
                    title: viewModel.isLoading ? "Creating..." : "Generate Brandkit",
                    isEnabled: viewModel.canSubmit && !viewModel.isLoading,
                    fill: AnyShapeStyle(.brand)
                ) {
                    Task {
                        if let project = await viewModel.submit() {
                            onGenerate(project)
                        }
                    }
                }
                .padding(.top, 8)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 40)
        }
        .scrollDismissesKeyboard(.interactively)
        .brandBackground()
        .customNavigationBar(title: viewModel.title, onBack: onBack)
        .sheet(isPresented: $isModelPickerPresented) {
            ModelPickerSheet(
                availableModels: viewModel.availableModels,
                selectedModelID: viewModel.selectedModel.id,
                isLoading: viewModel.isLoadingModels,
                loadFailed: viewModel.modelsLoadFailed,
                onSelect: viewModel.selectModel,
                onRetry: { await viewModel.loadModels(force: true) }
            )
        }
        .task {
            await viewModel.loadModels()
        }
    }

    private var modelPickerButton: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("MODEL")
                .font(.footnote)
                .foregroundStyle(.secondary)

            Button {
                isModelPickerPresented = true
            } label: {
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(viewModel.selectedModel.title)
                            .font(.body.weight(.semibold))
                            .foregroundStyle(.primary)

                        Text(viewModel.selectedModel.providerTitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Spacer(minLength: 8)

                    if viewModel.isLoadingModels && viewModel.availableModels.isEmpty {
                        ProgressView()
                            .controlSize(.small)
                    } else {
                        Image(systemName: "chevron.right")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal, 16)
                .frame(minHeight: 60)
                .background(Color(.secondarySystemBackground), in: .rect(cornerRadius: 12))
                .overlay {
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color(.separator), lineWidth: 1)
                }
                .contentShape(.rect(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Generation model")
            .accessibilityValue(viewModel.selectedModel.title)
            .accessibilityHint("Opens the list of available models")
        }
    }
}

#Preview {
    NavigationStack {
        ProjectCreateView(onBack: {})
    }
}
