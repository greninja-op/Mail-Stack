import Foundation

enum LogRedactor {
    private static let banned = ["token", "refresh", "password", "authorization", "cookie", "client_secret"]

    static func allow(event: String, fields: [String: String]) -> Bool {
        for (k, v) in fields {
            let key = k.lowercased()
            if banned.contains(where: { key.contains($0) }) { return false }
            if v.count > 200 { return false }
            if v.lowercased().contains("bearer ") { return false }
        }
        _ = event
        return true
    }

    static func redact(_ text: String) -> String {
        var out = text
        for word in banned {
            out = out.replacingOccurrences(of: word, with: "[redacted]", options: .caseInsensitive)
        }
        return out
    }
}
