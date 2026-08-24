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

    func navigationSwipeBackDisabled(_ isDisabled: Bool) -> some View {
        background(SwipeBackDisabler(isDisabled: isDisabled))
    }
}

private struct SwipeBackDisabler: UIViewControllerRepresentable {
    let isDisabled: Bool

    final class Controller: UIViewController {
        var isDisabled = false

        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            applyState()
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            applyState()
        }

        func applyState() {
            guard let navigationController else { return }
            let isEnabled = !isDisabled
            navigationController.interactivePopGestureRecognizer?.isEnabled = isEnabled
            if #available(iOS 26.0, *) {
                navigationController.interactiveContentPopGestureRecognizer?.isEnabled = isEnabled
            }
        }
    }

    func makeUIViewController(context: Context) -> Controller {
        let controller = Controller()
        controller.isDisabled = isDisabled
        return controller
    }

    func updateUIViewController(_ uiViewController: Controller, context: Context) {
        uiViewController.isDisabled = isDisabled
        uiViewController.applyState()
    }

    static func dismantleUIViewController(_ uiViewController: Controller, coordinator: ()) {
        uiViewController.isDisabled = false
        uiViewController.applyState()
    }
}
