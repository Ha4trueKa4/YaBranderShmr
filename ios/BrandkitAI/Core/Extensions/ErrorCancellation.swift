import Foundation

extension Error {
    /// `true`, если ошибка — следствие отмены задачи (например, `.task(id:)`
    /// перезапустил загрузку при смене сессии), а не реальный сбой.
    /// Такую ошибку пользователю не показываем.
    var isTaskCancellation: Bool {
        if self is CancellationError { return true }
        return (self as? URLError)?.code == .cancelled
    }
}
