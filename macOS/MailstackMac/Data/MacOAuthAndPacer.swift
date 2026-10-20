import Foundation

enum MacOAuthConstants {
    static let redirectURI = "com.greninjaop.mailorganizer.macos:/oauth2redirect"
    static let pkceMethod = "S256"
    static var baseExcludesModify: Bool { true } // gmail.modify incremental only
}

struct MacTokenBucket {
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
