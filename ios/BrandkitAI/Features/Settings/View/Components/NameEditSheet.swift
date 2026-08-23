import SwiftUI

struct NameEditSheet: View {
    let viewModel: SettingsViewModel

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @FocusState private var isFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Change name")
                .font(.system(size: 20, weight: .bold))
                .frame(maxWidth: .infinity, alignment: .leading)

            FormTextField(
                label: String(localized: "Full name"),
                placeholder: String(localized: "Enter your name"),
                text: $name,
                autocapitalization: .words
            )
            .focused($isFocused)

            if let error = viewModel.nameError {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(Color(.error))
            }

            PrimaryButton(
                title: String(localized: "Save"),
                isEnabled: !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                isLoading: viewModel.isSavingName
            ) {
                Task {
                    if await viewModel.saveName(name) {
                        dismiss()
                    }
                }
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .brandBackground()
        .presentationDetents([.height(280)])
        .presentationDragIndicator(.visible)
        .onAppear {
            name = viewModel.name
            viewModel.clearNameError()
            isFocused = true
        }
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        NameEditSheet(viewModel: SettingsViewModel(
            currentUserService: MockCurrentUserService(),
            userProfileService: MockUserProfileService()
        ))
    }
    .preferredColorScheme(.dark)
}
