import Foundation

/// Production-shaped OAuth seam. Live ASWebAuthenticationSession requires Mac + client IDs.
/// Unit tests use `MockGoogleOAuthService`.
protocol GoogleOAuthService: AnyObject {
    func beginAuthorization() async throws -> PendingOAuth
    func exchange(code: String, pending: PendingOAuth) async throws -> OAuthTokens
    func refresh(refreshToken: String) async throws -> OAuthTokens
}

struct PendingOAuth: Sendable {
    let state: String
    let codeVerifier: String
    let redirectURI: String
}

struct OAuthTokens: Sendable {
    let accessToken: String
    let refreshToken: String?
    let scope: String
}

@MainActor
final class MockGoogleOAuthService: GoogleOAuthService {
    func beginAuthorization() async throws -> PendingOAuth {
        let verifier = PKCEGenerator.makeVerifier()
        return PendingOAuth(
            state: UUID().uuidString,
            codeVerifier: verifier,
            redirectURI: OAuthConstants.redirectURI
        )
    }

    func exchange(code: String, pending: PendingOAuth) async throws -> OAuthTokens {
        _ = pending
        guard !code.isEmpty else { throw OAuthError.emptyCode }
        // Demo tokens — never real; Keychain store still exercised by AuthPort.
        return OAuthTokens(accessToken: "demo-access", refreshToken: "demo-refresh", scope: OAuthConstants.baseScopes.joined(separator: " "))
    }

    func refresh(refreshToken: String) async throws -> OAuthTokens {
        guard !refreshToken.isEmpty else { throw OAuthError.emptyCode }
        return OAuthTokens(accessToken: "demo-access-refreshed", refreshToken: refreshToken, scope: OAuthConstants.baseScopes.joined(separator: " "))
    }

    enum OAuthError: Error { case emptyCode }
}
