import Foundation

/// Sync pacing requirement for new platforms (~10 req/sec). Not Android phone behaviour.
struct TokenBucketPacer: Sendable {
    var capacity: Double
    var tokens: Double
    var refillPerSec: Double
    var lastMs: UInt64

    static func mailstackDefault() -> TokenBucketPacer {
        TokenBucketPacer(capacity: 10, tokens: 10, refillPerSec: 10, lastMs: 0)
    }

    mutating func tryTake(nowMs: UInt64) -> Bool {
        if nowMs > lastMs {
            let elapsed = Double(nowMs - lastMs) / 1000.0
            tokens = min(capacity, tokens + elapsed * refillPerSec)
            lastMs = nowMs
        }
        if tokens >= 1 {
            tokens -= 1
            return true
        }
        return false
    }
}

enum SyncFailureKind: Equatable {
    case perSecondThrottle
    case dailyQuota
    case offline
    case other

    var userMessage: String {
        switch self {
        case .perSecondThrottle: return "wait a minute, tap once"
        case .dailyQuota: return "resets at midnight Pacific"
        case .offline: return "You're offline — showing saved mail."
        case .other: return "Something went wrong. Try again."
        }
    }

    static func classify(status: Int, body: String) -> SyncFailureKind {
        let lower = body.lowercased()
        if lower.contains("daily") && (lower.contains("quota") || lower.contains("limit")) {
            return .dailyQuota
        }
        if status == 429 || lower.contains("ratelimit") {
            return .perSecondThrottle
        }
        if status == 0 { return .offline }
        return .other
    }
}
