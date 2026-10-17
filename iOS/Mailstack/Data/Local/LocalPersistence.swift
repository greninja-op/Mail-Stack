import Foundation

/// Phase 2 — local persistence seam. Demo uses in-memory; Mac build can
/// swap to SQLite / GRDB without rewriting views.
@MainActor
final class LocalPersistence: PersistencePort {
    private let store: InMemoryStore
    init(store: InMemoryStore) { self.store = store }

    func loadBootstrap() async {
        // Demo already seeded. Real build loads account-scoped DB files.
    }

    func wipeAllLocalData() async {
        store.threads = []
        store.accounts = []
        store.isSignedIn = false
        store.syncProgress = .idle
    }
}
