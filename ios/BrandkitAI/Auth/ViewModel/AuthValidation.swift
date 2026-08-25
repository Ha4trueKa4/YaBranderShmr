import Foundation

enum AuthValidation {
    static func email(_ value: String) -> String? {
        let email = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !email.isEmpty else { return String(localized: "Email is required.") }
        let pattern = #"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$"#
        guard email.range(of: pattern, options: [.regularExpression, .caseInsensitive]) != nil else {
            return String(localized: "Enter a valid email address.")
        }
        return nil
    }

    static func password(_ value: String, requiredStrength: Bool) -> String? {
        guard !value.isEmpty else { return String(localized: "Password is required.") }
        guard requiredStrength else { return nil }
        guard (8...128).contains(value.count),
              value.rangeOfCharacter(from: .letters) != nil,
              value.rangeOfCharacter(from: .decimalDigits) != nil else {
            return String(localized: "Use 8-128 characters with at least one letter and one number.")
        }
        return nil
    }

    static func fullName(_ value: String) -> String? {
        let name = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else { return String(localized: "Name is required.") }
        guard (3...64).contains(name.count) else { return String(localized: "Name must be 3–64 characters.") }
        return nil
    }
}
