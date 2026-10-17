import Foundation
import Combine

/// Phase 4 — sync contracts. Demo accounts never call Gmail.
/// Progress uses fixed denominator; UI caps at 99% while running.
@MainActor
final class DemoSyncCoordinator: SyncCoordinatorPort {
    private let store: InMemoryStore
    private var task: Task<Void, Never>?

    init(store: InMemoryStore) { self.store = store }

    func observeProgress(accountId: AccountId) -> AnyPublisher<SyncProgress, Never> {
        store.$syncProgress.eraseToAnyPublisher()
    }

    func startSync(accountId: AccountId) async {
        if accountId.value.hasPrefix("local-") || accountId.value.hasPrefix("fixture-") {
            // Demo policy: never sync.
            store.syncProgress = .succeeded(messagesProcessed: store.threads.count)
            return
        }
        task?.cancel()
        let total = 100 // fixed once known — never revised mid-run
        task = Task { @MainActor in
            for i in 0...total {
                if Task.isCancelled { return }
                store.syncProgress = .running(stage: .fetching, messagesProcessed: i, totalEstimate: total)
                try? await Task.sleep(nanoseconds: 20_000_000)
            }
            store.syncProgress = .succeeded(messagesProcessed: total)
        }
    }

    func cancelSync(accountId: AccountId) async {
        task?.cancel()
        store.syncProgress = .idle
    }
}
