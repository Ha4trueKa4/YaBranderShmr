//
//  onboardingView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 08.08.2026.
//

import SwiftUI
import Observation

struct OnboardingView: View {
    var onSignUp: () -> Void = {}
    var onSignIn: () -> Void = {}
    
    private let onboardingOpenAnimationDuration: Double = 1.5
    
    @State private var page = 0
    @State private var onboardingOpenAnimation: CGFloat = -1.0
    @State private var generateFeedback: Bool = false
    @State private var scrollState = OnboardingScrollState()

    private var pages: [AnyView] {
        [AnyView(logoPage), AnyView(howItWorksPage), AnyView(getStartedPage)]
    }

    private var lastPage: Int { pages.count - 1 }

    var body: some View {
        TabView(selection: $page) {
            ForEach(Array(pages.enumerated()), id: \.offset) { index, content in
                content.tag(index)
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button {
                if page < lastPage {
                    withAnimation(.smooth) {
                        page += 1
                    }
                } else {
                    onSignUp()
                }
            } label: {
                Text(page < lastPage ? "Next" : "Get Started")
                    .font(.title3.bold())
                    .padding(12)
                    .frame(maxWidth: .infinity)
            }
            .padding(.horizontal, 36)
            .buttonStyle(.borderedProminent)
            .animation(.smooth, value: page)
        }
        .tabViewStyle(.page)
        .background {
            OnboardingGradientBackground(state: scrollState)
        }
        .brightness(onboardingOpenAnimation)
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
    
    var logoPage: some View {
        var logo: some View {
            Image(systemName: "xmark.circle")
                .resizable()
                .frame(width: 50, height: 50)
                .padding(25)
        }
        
        return VStack(spacing: 32) {
            if #available(iOS 26.0, *) {
                logo
                    .glassEffect(.regular, in: RoundedRectangle(cornerSize: CGSize(width: 28, height: 28)))
            } else {
                logo
                    .alternateBackground(cornerSize: CGSize(width: 28, height: 28))
            }
            VStack(spacing: 12) {
                Text("Brandkit AI")
                    .font(.title.bold())
                Text("Turn words into brand identity")
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .overlay {
                GeometryReader { proxy in
                    let fraction = max(0, min(1, 1 + proxy.frame(in: .global).minX / proxy.size.width))
                    Color.clear
                        .onChange(of: fraction) { _, newValue in
                            scrollState.fraction = newValue
                        }
                }
            }
        }
        .colorScheme(.dark)
        .padding(.top, 16)
    }
    
    var howItWorksPage: some View {
        func makeLine(logo: Image, title: String, subtitle: String) -> some View {
            var sizedLogo: some View {
                logo
                    .font(.system(size: 20))
                    .padding(14)
                    .frame(width: 48, height: 48)
            }
            
            return HStack(spacing: 16) {
                if #available(iOS 26.0, *) {
                    sizedLogo
                        .glassEffect(.regular, in: RoundedRectangle(cornerSize: CGSize(width: 14, height: 14)))
                } else {
                    sizedLogo
                        .alternateBackground(cornerSize: CGSize(width: 14, height: 14))
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .bold()
                    Text(subtitle)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        
        return VStack {
            Text("How it works")
                .font(.title2.bold())
            VStack(spacing: 28) {
                makeLine(logo: Image(systemName: "pencil.line"), title: "Describe", subtitle: "Write what you need")
                makeLine(logo: Image(systemName: "pencil.line"), title: "Describe", subtitle: "Write what you need")
                makeLine(logo: Image(systemName: "pencil.line"), title: "Describe", subtitle: "Write what you need")
                makeLine(logo: Image(systemName: "pencil.line"), title: "Describe", subtitle: "Write what you need")
            }
            .padding(24)
        }
    }
    
    var getStartedPage: some View {
        var logo: some View {
            VStack(spacing: 16) {
                Image(systemName: "wand.and.sparkles")
                    .font(.system(size: 40))
                    .frame(width: 40, height: 40)
                    .padding(30)
                    .background {
                        RadialGradient(
                            colors: [
                                Color(.endGradient),
                                Color(.startGradient),
                            ],
                            center: .center,
                            startRadius: 0, endRadius: 50
                        )
                        .clipShape(Circle())
                    }
                VStack(spacing: 6) {
                    Text("NEXUS")
                        .font(.title3.bold())
                    Text("BRAND DESIGN")
                        .foregroundStyle(.secondary)
                        .font(.caption)
                }
            }
            .frame(width: 300, height: 300)
        }
        
        return VStack(spacing: 32) {
            if #available(iOS 26.0, *) {
                logo
                    .glassEffect(.regular, in: RoundedRectangle(cornerSize: CGSize(width: 24, height: 24)))
            } else {
                logo
                    .alternateBackground(cornerSize: CGSize(width: 24, height: 24))
            }
            VStack(spacing: 20) {
                Text("Start creating for free")
                    .font(.title.bold())
            }
            HStack {
                Text("Already have an account?")
                    .foregroundStyle(.secondary)
                Button {
                    onSignIn()
                } label: {
                    Text("Sign in")
                }
            }
        }
        .padding(24)
    }
}

@Observable
final class OnboardingScrollState {
    var fraction: CGFloat = 1.0
}

private struct OnboardingGradientBackground: View {
    let state: OnboardingScrollState

    var body: some View {
        let fraction = state.fraction
        let inverted = 1 - fraction
        let gradientStopLocation = min(0.999, 0.35 * (inverted * 2 + 1))
        ZStack {
            Color(.background)
            RadialGradient(
                stops: [
                    .init(color: .endGradient, location: 0),
                    .init(color: .startGradient, location: gradientStopLocation),
                    .init(color: .endGradient, location: 1),
                ],
                center: .center,
                startRadius: 0,
                endRadius: 500
            )
            .opacity(fraction)
            .blur(radius: inverted * 20)
            .grayscale(inverted)
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
