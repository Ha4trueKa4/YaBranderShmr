import Foundation
import Network
import os

nonisolated extension Notification.Name {
    static let networkDidBecomeAvailable = Notification.Name("networkDidBecomeAvailable")
}

/// Потокобезопасный монитор сети. Колбэк `NWPathMonitor` приходит на фоновой
/// очереди, а `isConnected` читается синхронно из MainActor
/// (например, `ChatViewModel`), поэтому состояние хранится под блокировкой.
/// Единственный писатель — очередь монитора; детекция перехода offline→online
/// выполняется в колбэке атомарно с записью.
nonisolated final class NetworkMonitor: Sendable {
    static let shared = NetworkMonitor()

    private let state = OSAllocatedUnfairLock(initialState: true)
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.brandkitai.networkmonitor")

    var isConnected: Bool {
        state.withLock { $0 }
    }

    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            guard let self else { return }
            let connected = path.status == .satisfied
            let restored = self.state.withLock { state -> Bool in
                let restored = connected && !state
                state = connected
                return restored
            }
            if restored {
                NotificationCenter.default.post(
                    name: .networkDidBecomeAvailable,
                    object: nil
                )
            }
        }
        monitor.start(queue: queue)
    }
}
