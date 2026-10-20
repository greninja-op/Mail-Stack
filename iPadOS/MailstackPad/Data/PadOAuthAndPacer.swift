import Foundation

enum PadOAuthConstants {
    static let redirectURI = "com.greninjaop.mailorganizer.ipad:/oauth2redirect"
    static let pkceMethod = "S256"
    static let baseScopes = [
        "openid", "email", "profile",
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/calendar.events",
        "https://www.googleapis.com/auth/tasks",
    ]
    static var baseExcludesModify: Bool { !baseScopes.contains(where: { $0.contains("gmail.modify") }) }
}

struct PadTokenBucket {
    var capacity: Double = 10
    var tokens: Double = 10
    var refillPerSec: Double = 10
    var lastMs: UInt64 = 0
    mutating func tryTake(nowMs: UInt64) -> Bool {
        if nowMs > lastMs {
            tokens = min(capacity, tokens + Double(nowMs - lastMs) / 1000.0 * refillPerSec)
            lastMs = nowMs
        }
        if tokens >= 1 { tokens -= 1; return true }
        return false
    }
}
