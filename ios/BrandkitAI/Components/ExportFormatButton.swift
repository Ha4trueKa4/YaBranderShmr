import SwiftUI

struct ExportFormatButton: View {
    let title: LocalizedStringKey
    let systemImage: String
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: systemImage)
                    .font(.system(size: 22, weight: .semibold))

                Text(title)
                    .font(.system(size: 15, weight: .semibold))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 92)
            .foregroundStyle(.primary)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    HStack(spacing: 12) {
        ExportFormatButton(title: "PNG", systemImage: "arrow.down.to.line")
        ExportFormatButton(title: "SVG", systemImage: "arrow.down.to.line")
    }
    .padding(16)
    .preferredColorScheme(.dark)
}
