import SwiftUI

struct SettingsView: View {
    var onBack: (() -> Void)?

    @Environment(SessionStore.self) private var session

    @State private var viewModel: SettingsViewModel
    @State private var isEditingName = false
    @State private var isShowingEmail = false
    @State private var isConfirmingExit = false
    @AppStorage(AppTheme.storageKey) private var themeRawValue = AppTheme.system.rawValue

    init(session: SessionStore, onBack: (() -> Void)? = nil) {
        self.onBack = onBack
        _viewModel = State(initialValue: SettingsViewModel(
            currentUserService: session.currentUserService,
            userProfileService: session.userProfileService
        ))
    }

    private var theme: Binding<AppTheme> {
        Binding(
            get: { AppTheme(rawValue: themeRawValue) ?? .system },
            set: { themeRawValue = $0.rawValue }
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                accountSection
                appearanceSection
                aboutSection
                exitButton
            }
            .padding(16)
        }
        .brandBackground()
        .customNavigationBar(title: String(localized: "Settings"), onBack: onBack)
        .sheet(isPresented: $isEditingName) {
            NameEditSheet(viewModel: viewModel)
        }
        .sheet(isPresented: $isShowingEmail) {
            EmailSheet(email: viewModel.email)
        }
        .alert("Sign out of your account?", isPresented: $isConfirmingExit) {
            Button("Exit", role: .destructive) {
                session.signOut()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You can sign back in at any time.")
        }
    }

    // MARK: - Секции

    private var accountSection: some View {
        SettingsSection(title: "Account") {
            SettingsRow(icon: "person", title: "Full name", value: viewModel.name) {
                isEditingName = true
            }

            SettingsDivider()

            SettingsRow(icon: "envelope", title: "Email", value: viewModel.email ?? "—") {
                isShowingEmail = true
            }
        }
    }

    private var appearanceSection: some View {
        SettingsSection(title: "Appearance") {
            SettingsRow(icon: "paintpalette", title: "Theme")

            SettingsDivider()

            ThemePicker(selection: theme)
                .padding(12)
        }
    }

    private var aboutSection: some View {
        SettingsSection(title: "About") {
            // Действий у политик пока нет — кнопки-заглушки под будущие ссылки.
            SettingsRow(icon: "doc.text", title: "Privacy Policy", showsChevron: true) {}

            SettingsDivider()

            SettingsRow(icon: "shield", title: "Terms of Use", showsChevron: true) {}
        }
    }

    private var exitButton: some View {
        Button(role: .destructive) {
            isConfirmingExit = true
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                Text("Exit")
            }
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(Color(.error))
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(Color(.error).opacity(0.08), in: .rect(cornerRadius: 14))
            .overlay {
                RoundedRectangle(cornerRadius: 14)
                    .stroke(Color(.error), lineWidth: 1)
            }
        }
        .buttonStyle(.plain)
        .padding(.top, 8)
    }
}

#Preview {
    NavigationStack {
        SettingsView(
            session: SessionStore(
                currentUserService: MockCurrentUserService(),
                userProfileService: MockUserProfileService()
            ),
            onBack: {}
        )
    }
    .environment(SessionStore(
        currentUserService: MockCurrentUserService(),
        userProfileService: MockUserProfileService()
    ))
}
