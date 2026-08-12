//
//  onboardingView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 08.08.2026.
//

import SwiftUI

struct OnboardingView: View {
    var onFinish: () -> Void = {}
    
    private let onboardingOpenAnimationDuration: Double = 1.5
    
    @State private var page: Page = .logo
    @State private var logoScrollFraction: CGFloat = 1.0
    private var logoScrollFractionIverted: CGFloat { 1.0 - logoScrollFraction }
    @State private var onboardingOpenAnimation: CGFloat = -1.0
    @State private var generateFeedback: Bool = false
    
    private enum Page: Hashable {
        case logo
        case howItWorks
        case getStarted
    }
    
    var body: some View {
        TabView(selection: $page) {
            logoPage
                .tag(Page.logo)
                .frame(maxWidth: .infinity)
                .overlay {
                    GeometryReader { proxy in
                        let fraction = max(0, min(1, 1 + proxy.frame(in: .global).minX / proxy.size.width))
                        Color.clear
                            .onChange(of: fraction) { _, newValue in
                                logoScrollFraction = newValue
                            }
                    }
                }
            howItWorksPage.tag(Page.howItWorks)
            getStartedPage.tag(Page.getStarted)
        }
        .tabViewStyle(.page)
        .background {
            ZStack {
                Color(.background)
                RadialGradient(stops: [.init(color: .clear, location: 0), .init(color: .startGradient, location: 0.35 * (logoScrollFractionIverted * 2 + 1)), .init(color: .clear, location: 1)], center: .center, startRadius: 0, endRadius: 500)
                    .opacity(logoScrollFraction)
                    .blur(radius: logoScrollFractionIverted * 20)
                    .grayscale(logoScrollFractionIverted)
            }
            .ignoresSafeArea(.all)
        }
        .brightness(onboardingOpenAnimation)
        .sensoryFeedback(.impact(weight: .light), trigger: generateFeedback)
        .onAppear {
            let duration = onboardingOpenAnimationDuration / 2
            Task {
                try await Task.sleep(for: .seconds(0.4))
                await MainActor.run {
                    withAnimation(.easeIn(duration: duration)) {
                        onboardingOpenAnimation = 0.18
                    }
                }
                try await Task.sleep(for: .seconds(duration))
                await MainActor.run {
                    generateFeedback.toggle()
                    withAnimation(.easeOut(duration: duration)) {
                        onboardingOpenAnimation = 0
                    }
                }
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
        }
        .padding(.bottom, 50)
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
                Button {
                    onFinish()
                } label: {
                    Text("Get Started")
                        .bold()
                        .padding(15)
                        .frame(maxWidth: .infinity)
                }
                .prominentStyle()
            }
            HStack {
                Text("Already have an account?")
                    .foregroundStyle(.secondary)
                Button {
                    
                } label: {
                    Text("Sign in")
                }
            }
        }
        .padding(24)
    }
}

#Preview {
    Color.clear
        .fullScreenCover(isPresented: .constant(true)) {
            OnboardingView()
        }
}
