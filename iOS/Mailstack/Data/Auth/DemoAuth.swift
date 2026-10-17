import Foundation
import Combine

/// Phase 3 — OAuth seam. Live ASWebAuthenticationSession requires Mac +
/// Google client config. Demo path signs into `local-demo` only.
@MainActor
final class DemoAuth: AuthPort {
    private let store: InMemoryStore
    private let keychain: KeychainTokenStore

    init(store: InMemoryStore, keychain: KeychainTokenStore = KeychainTokenStore()) {
        self.store = store
        self.keychain = keychain
    }

    var isSignedInPublisher: AnyPublisher<Bool, Never> {
        store.$isSignedIn.eraseToAnyPublisher()
    }

    func beginSignIn() async -> Result<Void, Error> {
        // Never store real tokens in demo. Keychain API is exercised with a niche marker.
        _ = try? keychain.save(account: "local-demo", data: Data("demo-session".utf8))
        store.isSignedIn = true
        if store.accounts.isEmpty {
            store.accounts = [
                AccountSnapshot(
                    accountId: AccountId("local-demo"),
                    email: "demo@mailstack.local",
                    displayName: "Demo",
                    isActive: true
                )
            ]
            store.threads = DemoSeed.threads
        }
        return .success(())
    }

    func signOut() async {
        try? keychain.delete(account: "local-demo")
        store.isSignedIn = false
    }

    func hasModifyScope() async -> Bool { false }
}
