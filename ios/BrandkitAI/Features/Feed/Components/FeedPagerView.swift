import SwiftUI

struct FeedPagerView<Content: View>: View {
    @Binding var selection: FeedTab
    @ViewBuilder var content: (FeedTab) -> Content

    var body: some View {
        TabView(selection: $selection) {
            ForEach(FeedTab.allCases, id: \.self) { tab in
                content(tab)
                    .tag(tab)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
    }
}

#Preview {
    FeedPagerView(selection: .constant(.all)) { tab in
        Text(tab.rawValue)
    }
}
