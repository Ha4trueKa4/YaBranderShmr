//
//  onboardingView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 08.08.2026.
//

import SwiftUI
import Observation

struct OnboardingView: View {
    var onGetStarted: () -> Void = {}
    var onSignIn: () -> Void = {}
    
    private let onboardingOpenAnimationDuration: Double = 1.5
    
    @State private var page = 0
    @State private var onboardingOpenAnimation: CGFloat = -1.0
    @State private var generateFeedback: Bool = false
    @State private var scrollState = OnboardingScrollState()

    private var pages: [AnyView] {
        [AnyView(brandIdentity), AnyView(yourDesign), AnyView(shareWithCommunity)]
    }

    private var lastPage: Int { pages.count - 1 }

    var body: some View {
        TabView(selection: $page) {
            ForEach(Array(pages.enumerated()), id: \.offset) { index, content in
                content.tag(index)
            }
        }
        .onAppear() {
            setupAppearance()
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                if page < lastPage {
                    withAnimation(.smooth) {
                        page += 1
                    }
                } else {
                    onGetStarted()
                }
            } label: {
                Text(page < lastPage ? "Next" : "Get Started")
                    .font(.title3.bold())
                    .padding(12)
                    .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 36)
            .buttonStyle(.borderedProminent)
            .buttonBorderShape(.roundedRectangle(radius: 20))
            .animation(.smooth, value: page)
        }
        .tabViewStyle(.page)
        .background {
            OnboardingGradientBackground(state: scrollState)
        }
        .brightness(onboardingOpenAnimation)
        .opacity(onboardingOpenAnimation + 1)
        .sensoryFeedback(.impact(weight: .light), trigger: generateFeedback)
        .task {
            let duration = onboardingOpenAnimationDuration / 2
            do {
                try await Task.sleep(for: .seconds(0.5))
                guard !Task.isCancelled else { return }
                withAnimation(.easeIn(duration: duration)) {
                    onboardingOpenAnimation = 0.18
                }

                try await Task.sleep(for: .seconds(duration))
                guard !Task.isCancelled else { return }
                generateFeedback.toggle()
                withAnimation(.easeOut(duration: duration)) {
                    onboardingOpenAnimation = 0
                }
            } catch is CancellationError {
                return
            } catch {
                return
            }
        }
    }
    
    func setupAppearance() {
        UIPageControl.appearance().currentPageIndicatorTintColor = .accent
        UIPageControl.appearance().pageIndicatorTintColor = .secondaryLabel.withAlphaComponent(0.4)
    }
    
    var brandIdentity: some View {
        var logo: some View {
            Image(.appLogoM)
                .resizable()
                .frame(width: 88, height: 88)
        }
        
        return VStack(spacing: 0) {
            VStack(spacing: 20) {
                logo
                Text("Ya Brander")
                    .font(.title.bold())
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .overlay {
                GeometryReader { proxy in
                    let frame = proxy.frame(in: .global)
                    let fraction = max(0, min(1, 1 + frame.minX / proxy.size.width))
                    Color.clear
                        .onAppear {
                            scrollState.fraction = fraction
                            scrollState.gradientCenter = CGPoint(x: frame.midX, y: frame.midY)
                        }
                        .onChange(of: fraction) { _, newValue in
                            scrollState.fraction = newValue
                        }
                        .onChange(of: frame) { _, newValue in
                            scrollState.gradientCenter = CGPoint(x: newValue.midX, y: newValue.midY)
                        }
                }
            }
            makeTextBlock(firstLine: "Turn words into", secondLine: "brand identity", subtitle: "Describe your brand in a brief — AI builds\nlogo, palette and typography in vector")
        }
    }
    
    var yourDesign: some View {
        let shape = RoundedRectangle(cornerRadius: 24)
        
        return VStack(spacing: 24) {
            chatDemo
                .clipShape(shape)
                .overlay {
                    shape.stroke().foregroundStyle(.border)
                }
                .padding(.top, 1)
                .padding(.horizontal, 24)
                .frame(maxHeight: 460)
                .frame(maxHeight: .infinity)
            makeTextBlock(firstLine: "Refine the design", secondLine: "right in the chat", subtitle: "Ask for changes in plain words — the\nassistant updates the design and saves it")
        }
    }
    
    private var chatDemo: some View {
        NavigationStack {
            ChatView(
                title: "Ya Brander",
                viewModel: ChatViewModel.makeDemo()
            )
        }
        .allowsHitTesting(false)
    }
    
    var shareWithCommunity: some View {
        let shape = RoundedRectangle(cornerRadius: 24)

        return VStack(spacing: 24) {
            communityDemo
                .background(Color.background)
                .clipShape(shape)
                .overlay {
                    shape.stroke().foregroundStyle(.border)
                }
                .padding(.top, 1)
                .padding(.horizontal, 24)
                .frame(maxHeight: 460)
                .frame(maxHeight: .infinity)
            makeTextBlock(firstLine: "Share with", secondLine: "the community", subtitle: "Publish to the feed, follow creators and\nexport your kit as PNG or SVG")
        }
    }

    private var communityDemo: some View {
        VStack(spacing: 20) {
            HStack(spacing: 12) {
                FeedListCardView(
                    title: "Project Alpha",
                    coverColors: [Color(red: 1, green: 0.71, blue: 0.42), Color(red: 0.91, green: 0.36, blue: 0.02)],
                    coverHeight: 96,
                    accountName: "Alice",
                    onTap: {}
                ) {
                    projectCoverLogo
                }

                FeedListCardView(
                    title: "Project Beta",
                    coverColors: [Color(red: 0.64, green: 0.39, blue: 1), Color(red: 0.36, green: 0.18, blue: 0.62)],
                    coverHeight: 96,
                    accountName: "Bob",
                    onTap: {}
                ) {
                    projectCoverLogo
                }
            }

            ProfileConnectionRow(
                name: "Sarah Chen",
                subtitle: "Brand designer",
                onOpenProfile: {}
            ) {
                FollowButton(isSubscribed: false, isBusy: false, action: {})
            }

            HStack(spacing: 12) {
                ExportFormatButton(title: "PNG", systemImage: "photo")
                ExportFormatButton(title: "SVG", systemImage: "chevron.left.forwardslash.chevron.right")
            }
        }
        .padding(16)
        .allowsHitTesting(false)
    }

    private var projectCoverLogo: some View {
        Image(systemName: "sparkle")
            .resizable()
            .scaledToFit()
            .frame(width: 36, height: 36)
            .foregroundStyle(.white)
    }
    
    func makeTextBlock(firstLine: String, secondLine: String, subtitle: String) -> some View {
        return VStack(spacing: 13) {
            VStack(spacing: 0) {
                Text(firstLine)
                Text(secondLine)
                    .foregroundStyle(.accent)
            }
            .font(.largeTitle.bold())
            Text(subtitle)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.bottom, 90)
    }
}

@Observable
final class OnboardingScrollState {
    var fraction: CGFloat = 1.0
    var gradientCenter: CGPoint?
}

private struct OnboardingGradientBackground: View {
    @Environment(\.colorScheme) var colorScheme
    
    let state: OnboardingScrollState
    private let endRadius: Double = max(UIScreen.main.bounds.height, UIScreen.main.bounds.width) / 2
    private let middleRadius: Double = 160
    private let middleCoefficient: Double
    
    init(state: OnboardingScrollState) {
        self.state = state
        self.middleCoefficient = middleRadius / endRadius
    }
    
    var body: some View {
        GeometryReader { proxy in
            let fraction = state.fraction
            let inverted = 1 - fraction
            let center = state.gradientCenter.map {
                UnitPoint(x: $0.x / proxy.size.width, y: $0.y / proxy.size.height)
            } ?? .center
            ZStack {
                Color(.background)
                Group {
                    if colorScheme == .dark {
                        let gradientStopLocation = min(0.999, middleCoefficient * (inverted * 2 + 1))
                        
                        RadialGradient(
                            stops: [
                                .init(color: .clear, location: 0),
                                .init(color: .startGradient, location: gradientStopLocation),
                                .init(color: .clear, location: 1),
                            ],
                            center: center,
                            startRadius: 0,
                            endRadius: endRadius * (inverted + 1)
                        )
                    } else {
                        let gradientStopLocation = (inverted)
                        
                        RadialGradient(
                            stops: [
                                .init(color: .startGradient, location: gradientStopLocation),
                                .init(color: .clear, location: 1),
                            ],
                            center: center,
                            startRadius: 0,
                            endRadius: endRadius * (inverted + 1)
                        )
                    }
                }
                .opacity(fraction)
                .grayscale(inverted)
                LinearGradient(stops: [.init(color: .startGradient.opacity(0.8), location: 0), .init(color: .clear, location: 0.6)], startPoint: .top, endPoint: .bottom)
                .opacity(inverted)
            }
        }
        .ignoresSafeArea(.all)
    }
}

#Preview {
    Color.clear
        .fullScreenCover(isPresented: .constant(true)) {
            OnboardingView()
        }
}
