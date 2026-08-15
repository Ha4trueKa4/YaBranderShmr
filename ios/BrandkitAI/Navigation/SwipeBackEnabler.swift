import SwiftUI
import UIKit

private struct SwipeBackEnabler: UIViewControllerRepresentable {
    final class Controller: UIViewController, UIGestureRecognizerDelegate {
        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            attachToPopGesture()
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            attachToPopGesture()
        }

        private func attachToPopGesture() {
            navigationController?.interactivePopGestureRecognizer?.delegate = self
        }

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard let navigationController else { return false }
            return navigationController.viewControllers.count > 1
                && navigationController.transitionCoordinator == nil
        }
    }

    func makeUIViewController(context: Context) -> Controller {
        Controller()
    }

    func updateUIViewController(_ uiViewController: Controller, context: Context) {}
}

extension View {
    func swipeBackEnabled() -> some View {
        background(SwipeBackEnabler())
    }
}
