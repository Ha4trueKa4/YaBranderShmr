import Foundation
import Network

extension Notification.Name {
    static let networkDidBecomeAvailable = Notification.Name("networkDidBecomeAvailable")
}

final class NetworkMonitor {
    static let shared = NetworkMonitor()

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.brandkitai.networkmonitor")

    private(set) var isConnected = true

    private init() {
        monitor.pathUpdateHandler = { [weak self] path in
            let connected = path.status == .satisfied
            let wasConnected = self?.isConnected ?? true
            self?.isConnected = connected

            if connected && !wasConnected {
                NotificationCenter.default.post(
                    name: .networkDidBecomeAvailable,
                    object: nil
                )
            }
        }
        monitor.start(queue: queue)
    }
}