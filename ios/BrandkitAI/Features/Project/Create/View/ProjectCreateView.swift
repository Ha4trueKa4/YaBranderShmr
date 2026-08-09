import SwiftUI

struct ProjectCreateView: View {
    var onBack: (() -> Void)?

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
                    viewModel.generate()
                }
                .padding(.top, 8)
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 40)
        }
        .scrollDismissesKeyboard(.interactively)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .navigationTitle(viewModel.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let onBack {
                backItem(action: onBack)
            }
        }
        .preferredColorScheme(.dark)
        .navigationDestination(isPresented: $viewModel.isGenerating) {
            ProjectGeneratingView()
        }
    }

    @ToolbarContentBuilder
    private func backItem(action: @escaping () -> Void) -> some ToolbarContent {
        if #available(iOS 26.0, *) {
            ToolbarItem(placement: .topBarLeading) {
                backButton(action: action)
            }
            .sharedBackgroundVisibility(.hidden)
        } else {
            ToolbarItem(placement: .topBarLeading) {
                backButton(action: action)
            }
        }
    }

    private func backButton(action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: "arrow.left")
                .font(.body.weight(.semibold))
                .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Back")
    }
}

#Preview {
    NavigationStack {
        ProjectCreateView(onBack: {})
    }
}
