import SwiftUI

struct ChatImagePreviewView: View {
    let viewModel: ChatImagePreviewViewModel
    var onBack: () -> Void = {}
    var onDownloadPNG: () -> Void = {}
    var onDownloadSVG: () -> Void = {}

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                imagePlaceholder

                VStack(alignment: .leading, spacing: 0) {
                    Text(viewModel.title)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(.primary)
                        .padding(.top, 24)

                    Text(viewModel.publishedText)
                        .font(.system(size: 14))
                        .foregroundStyle(.secondary)
                        .padding(.top, 6)

                    downloadButton(
                        title: "Download PNG",
                        systemImage: "square.and.arrow.down",
                        isPrimary: true,
                        action: onDownloadPNG
                    )
                    .padding(.top, 20)

                    downloadButton(
                        title: "Download SVG",
                        systemImage: "chevron.left.forwardslash.chevron.right",
                        isPrimary: false,
                        action: onDownloadSVG
                    )
                    .padding(.top, 12)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .scrollIndicators(.hidden)
        .customNavigationBar(onBack: onBack)
        .toolbarBackground(Color.background, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }

    private var imagePlaceholder: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.47, green: 0.18, blue: 0.96),
                    Color(red: 0.02, green: 0.73, blue: 0.82),
                    Color(red: 0.91, green: 0.27, blue: 0.60)
                ],
                startPoint: .leading,
                endPoint: .trailing
            )

            Image(systemName: "photo")
                .font(.system(size: 44, weight: .light))
                .foregroundStyle(.white.opacity(0.72))
        }
        .aspectRatio(1, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Generated project image placeholder")
    }

    private func downloadButton(
        title: String,
        systemImage: String,
        isPrimary: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(.system(size: 15, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .foregroundStyle(isPrimary ? .white : .secondary)
                .background {
                    RoundedRectangle(cornerRadius: 9)
                        .fill(isPrimary ? Color.accentColor : Color(.secondarySystemBackground))
                }
                .overlay {
                    if !isPrimary {
                        RoundedRectangle(cornerRadius: 9)
                            .stroke(Color(.separator), lineWidth: 1)
                    }
                }
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        ChatImagePreviewView(
            viewModel: ChatImagePreviewViewModel(
                projectID: UUID(),
                title: "Nova Studio Brand"
            )
        )
        .brandBackground()
        .preferredColorScheme(.dark)
    }
}
