import SwiftUI

private struct PickerBarHeightKey: PreferenceKey {
    static let defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct FeedView: View {
    @State private var selectedTab: FeedTab = .all
    @State private var scrolledTabs: [FeedTab: Bool] = [:]
    @State private var pickerBarHeight: CGFloat = 50

    private var animatedSelection: Binding<FeedTab> {
        Binding(
            get: { selectedTab },
            set: { newValue in
                withAnimation(.easeOut) { selectedTab = newValue }
            }
        )
    }

    private var picker: some View {
        Picker(selection: animatedSelection) {
            ForEach(FeedTab.allCases, id: \.self) { tab in
                Text(tab.rawValue).tag(tab)
            }
        } label: {
            EmptyView()
        }
        .pickerStyle(.segmented)
    }

    private func scrolledBinding(for tab: FeedTab) -> Binding<Bool> {
        Binding(
            get: { scrolledTabs[tab, default: false] },
            set: { scrolledTabs[tab] = $0 }
        )
    }

    var body: some View {
        GeometryReader() { geo in
        TabView(selection: $selectedTab) {
            FeedListView(topInset: pickerBarHeight, isScrolled: scrolledBinding(for: .all))
                .tag(FeedTab.all)
            FeedListView(topInset: pickerBarHeight, isScrolled: scrolledBinding(for: .subscriptions))
                .tag(FeedTab.subscriptions)
        }
        .overlay(alignment: .top) {
            if #available(iOS 26.0, *) {
                measuredPickerBar(
                    picker
                        .glassEffect(.clear)
                        .padding(.top, geo.safeAreaInsets.top)
                )
            } else {
                measuredPickerBar(
                    VStack(spacing: 0) {
                        picker
                            .padding(.top, geo.safeAreaInsets.top + 8)
                            .padding(.horizontal, 16)
                            .padding(.bottom, 8)
                    }
                    .frame(maxWidth: .infinity)
                    .background(.bar.opacity(scrolledTabs[selectedTab, default: false] ? 1 : 0))
                    .animation(.easeOut(duration: 0.2), value: scrolledTabs[selectedTab, default: false])
                    .ignoresSafeArea(edges: .top)
                )
            }
        }
        .onPreferenceChange(PickerBarHeightKey.self) { height in
            guard height > 0 else { return }
            pickerBarHeight = height
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .ignoresSafeArea(.all)
    }
    }

    private func measuredPickerBar<Content: View>(_ content: Content) -> some View {
        content.background {
            GeometryReader { proxy in
                Color.clear
                    .preference(key: PickerBarHeightKey.self, value: proxy.size.height)
            }
        }
    }
}

#Preview {
    FeedView()
}
