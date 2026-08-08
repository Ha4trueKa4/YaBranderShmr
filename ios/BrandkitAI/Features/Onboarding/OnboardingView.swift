import SwiftUI

struct OnboardingView: View {
    var onFinish: () -> Void = {}

    var body: some View {
        VStack(spacing: 16) {
            Text("Onboarding")

            Button("Get started") {
                onFinish()
            }
        }
        .navigationTitle("Onboarding")
    }
}

#Preview {
    OnboardingView()
}
