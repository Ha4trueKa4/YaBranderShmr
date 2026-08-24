import SwiftUI

struct ProjectCreateView: View {
    var onBack: (() -> Void)?
    var onGenerate: (ProjectCreationResult) -> Void = { _ in }

    @State private var viewModel: ProjectCreateViewModel
    @State private var isModelPickerPresented = false
    @State private var didLeaveScreen = false
    @State private var submitTask: Task<Void, Never>?

    init(
        onBack: (() -> Void)? = nil,
        onGenerate: @escaping (ProjectCreationResult) -> Void = { _ in },
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
                    autocapitalization: .words,
                    errorMessage: viewModel.nameValidationMessage
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
                    title: viewModel.isLoading ? "Creating..." : "Generate",
                    isEnabled: viewModel.canSubmit,
                    fill: AnyShapeStyle(Color.accentColor)
                ) {
                    guard submitTask == nil else { return }
                    submitTask = Task { @MainActor in
                        defer { submitTask = nil }
                        if let result = await viewModel.submit() {
                            guard !didLeaveScreen else { return }
                            onGenerate(result)
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
        .customNavigationBar(
            title: viewModel.title,
            onBack: onBack,
            isBackEnabled: submitTask == nil
        )
        .navigationSwipeBackDisabled(submitTask != nil)
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
        .onDisappear {
            didLeaveScreen = true
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
