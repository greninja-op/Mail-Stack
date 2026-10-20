import Foundation

/// Phase 13 — AI never receives more than needed; deterministic stays authoritative.
enum DataMinimizer {
    static func minimizeForSummary(subject: String, snippet: String, maxChars: Int = 500) -> String {
        let raw = subject + "\n" + snippet
        if raw.count <= maxChars { return raw }
        return String(raw.prefix(maxChars))
    }

    static func looksLikeInjection(_ text: String) -> Bool {
        let lower = text.lowercased()
        return lower.contains("ignore previous") || lower.contains("system prompt")
    }
}
