import SwiftUI

struct ProjectCreateView: View {
    var onBack: (() -> Void)?
    /// Отдаёт введённое имя проекта — оно становится заголовком чата.
    var onGenerate: (String) -> Void = { _ in }

    @State private var viewModel = ProjectCreateViewModel()

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

                PrimaryButton(
                    title: "Generate Brandkit",
                    isEnabled: viewModel.canSubmit,
                    fill: AnyShapeStyle(.brand)
                ) {
                    guard viewModel.canSubmit else { return }
                    onGenerate(viewModel.name)
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
