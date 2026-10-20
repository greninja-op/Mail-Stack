import Foundation

/// Phase 3 — OAuth contract constants. No client secrets in the repo.
enum OAuthConstants {
    static let redirectURI = "com.greninjaop.mailorganizer:/oauth2redirect"
    static let pkceMethod = "S256"
    /// Base scopes — gmail.modify is NEVER included (incremental only).
    static let baseScopes = [
        "openid", "email", "profile",
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/calendar.events",
        "https://www.googleapis.com/auth/tasks",
    ]
    static var baseExcludesModify: Bool {
        !baseScopes.contains(where: { $0.contains("gmail.modify") })
    }
}
