import SwiftUI

struct ProjectCreateView: View {
    var onBack: (() -> Void)?
    var onGenerate: (Project) -> Void = { _ in }

    @State private var viewModel: ProjectCreateViewModel

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
                    label: "BRAND DESCRIPTION & BRIEF",
                    placeholder: "Describe your brand values, target audience, preferred color scheme, visual mood (e.g., 'A modern cyberpunk aesthetic with neon highlights, catering to software developers who love dark mode tools...')",
                    text: $viewModel.brief
                )

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
    }
}

#Preview {
    NavigationStack {
        ProjectCreateView(onBack: {})
    }
}
