//
//  onboardingView.swift
//  BrandkitAI
//
//  Created by Андрей Валянский on 08.08.2026.
//

import SwiftUI

struct onboardingView: View {
    var body: some View {
        TabView() {
            logoPage
            howItWorksPage
            getStartedPage
        }
        .tabViewStyle(.page)
        .ignoresSafeArea(.all)
        .background {
            Color(.colorBackground)
                .ignoresSafeArea(.all)
        }
    }
    
    var logoPage: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(.colorStartGradient),
                    Color(.colorEndGradient)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing,
            )
            .ignoresSafeArea(.all)
            VStack(spacing: 32) {
                Image(systemName: "xmark.circle")
                    .resizable()
                    .frame(width: 50, height: 50)
                    .padding(25)
                    .glassEffect(.regular, in: RoundedRectangle(cornerSize: CGSize(width: 28, height: 28)))
                VStack(spacing: 12) {
                    Text("Brandkit AI")
                        .font(.title.bold())
                    Text("Turn words into brand identity")
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.bottom, 50)
        }
    }
    
    var howItWorksPage: some View {
        func makeLine(logo: Image, title: String, subtitle: String) -> some View {
            HStack(spacing: 16) {
                logo
                    .font(.system(size: 20))
                    .padding(14)
                    .glassEffect(.regular, in: RoundedRectangle(cornerSize: CGSize(width: 14, height: 14)))
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
        VStack(spacing: 32) {
            VStack(spacing: 16) {
                Image(systemName: "wand.and.sparkles")
                    .font(.system(size: 40))
                    .frame(width: 40, height: 40)
                    .padding(30)
                    .background {
                        RadialGradient(
                            colors: [
                                Color(.colorEndGradient),
                                Color(.colorStartGradient),
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
            .glassEffect(.regular, in: RoundedRectangle(cornerSize: CGSize(width: 24, height: 24)))
            VStack(spacing: 20) {
                Text("Start creating for free")
                    .font(.title.bold())
                Button {
                    
                } label: {
                    Text("Get Started")
                        .bold()
                        .padding(15)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.glassProminent)
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
    onboardingView()
}
