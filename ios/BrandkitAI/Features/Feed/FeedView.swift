import SwiftUI

private struct ScrollDistances: Equatable {
    let top: CGFloat
    let bottom: CGFloat
}

struct FeedView: View {
    @State private var selectedTab: FeedTab = .all
    @State private var scrollPosition: FeedTab? = .all
    @State private var scrollDistances: [FeedTab: ScrollDistances] = [:]
    
    private let toolbarFadeDistance: CGFloat = 24
    
    private var currentScrollDistances: ScrollDistances {
        scrollDistances[selectedTab] ?? ScrollDistances(top: 0, bottom: 0)
    }
    
    private var toolbarOpacity: CGFloat {
        min(max(currentScrollDistances.top / toolbarFadeDistance, 0), 1)
    }
    
    private var tabBarOpacity: CGFloat {
        1 - min(max(currentScrollDistances.bottom / toolbarFadeDistance, 0), 1)
    }
    
    private var picker: some View {
        Picker(selection: $selectedTab) {
            ForEach(FeedTab.allCases, id: \.self) { tab in
                Text(tab.rawValue).tag(tab)
            }
        } label: {
            EmptyView()
        }
        .pickerStyle(.segmented)
        .padding(16)
    }
    
    @ViewBuilder
    private func applyingLegacyToolbarAppearance<Content: View>(to content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
        } else {
            content
                .toolbarBackgroundVisibility(.hidden, for: .navigationBar)
                .toolbarBackground(.windowBackground.opacity(tabBarOpacity), for: .tabBar)
                .toolbarBackgroundVisibility(tabBarOpacity > 0.6 ? .hidden : .visible, for: .tabBar)
        }
    }
    
    var body: some View {
        applyingLegacyToolbarAppearance(to:
        //        ScrollView {
        ScrollView(.horizontal) {
            LazyHStack(spacing: 0) {
                ForEach(FeedTab.allCases, id: \.self) { tab in
                    ScrollView {
                        FeedListView(selectedTab: tab)
                            .containerRelativeFrame(.horizontal)
                            .id(tab)
                    }
                    .onScrollGeometryChange(for: ScrollDistances.self) { geometry in
                        let topDistance = max(
                            0,
                            geometry.contentOffset.y + geometry.contentInsets.top
                        )
                        let bottomDistance = max(
                            0,
                            geometry.contentSize.height
                                + geometry.contentInsets.bottom
                                - geometry.visibleRect.maxY
                        )
                        return ScrollDistances(top: topDistance, bottom: bottomDistance)
                    } action: { _, distances in
                        scrollDistances[tab] = distances
                    }
                }
            }
            .scrollTargetLayout()
        }
        .scrollIndicators(.hidden)
        .scrollTargetBehavior(.paging)
        .scrollPosition(id: $scrollPosition)
        .onChange(of: scrollPosition) { _, tab in
            if let tab {
                selectedTab = tab
            }
        }
        .onChange(of: selectedTab) { _, tab in
            withAnimation(.spring) {
                scrollPosition = tab
            }
        }
        .safeAreaInset(edge: .top) {
            if #available(iOS 26.0, *) {
                picker
            } else {
                picker
                    .background {
                        EmptyView()
                            .background(.bar)
                            .opacity(toolbarOpacity)
                    }
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    
                } label: {
                    Image(systemName: "magnifyingglass")
                }
            }
        }
        )
    }
}

#Preview {
    FeedView()
}
