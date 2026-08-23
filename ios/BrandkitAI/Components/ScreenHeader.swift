import SwiftUI

struct ScreenHeader: View {
    let title: String

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            // TODO: add logo if need
            
            Text(title)
                .font(.title.bold())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#Preview {
    ScreenHeader(title: "Sign in")
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .tint(Color.accentColor)
    .preferredColorScheme(.dark)
}
