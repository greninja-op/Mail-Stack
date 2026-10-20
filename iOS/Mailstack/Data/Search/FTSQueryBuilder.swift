import Foundation

/// Structured query → FTS/MATCH fragments. UI never builds raw SQL from free text.
enum FTSQueryBuilder {
    static func escapeToken(_ raw: String) -> String {
        let cleaned = raw.replacingOccurrences(of: "\"", with: "\"\"")
        return "\"\(cleaned)\""
    }

    static func matchExpression(for query: StructuredQuery) -> String? {
        var parts: [String] = []
        if let t = query.freeText, !t.isEmpty {
            parts.append(escapeToken(t))
        }
        if let from = query.from, !from.isEmpty {
            parts.append("from:\(escapeToken(from))")
        }
        if let subject = query.subject, !subject.isEmpty {
            parts.append("subject:\(escapeToken(subject))")
        }
        return parts.isEmpty ? nil : parts.joined(separator: " ")
    }
}
