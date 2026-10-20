import Foundation

/// Phase 2 — file-backed local store. Uses Application Support JSON as a portable
/// stand-in that survives process restart; on Mac/device this can be swapped for
/// GRDB/SQLite without changing the PersistencePort surface.
@MainActor
final class SQLiteStore: ObservableObject {
    @Published private(set) var threads: [ThreadSummary] = []
    @Published private(set) var accounts: [AccountSnapshot] = []

    private let root: URL
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(root: URL? = nil) {
        let base = root ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
            .appendingPathComponent("Mailstack", isDirectory: true)
        self.root = base
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
    }

    private var threadsURL: URL { root.appendingPathComponent("threads.json") }
    private var accountsURL: URL { root.appendingPathComponent("accounts.json") }

    func load() throws {
        if FileManager.default.fileExists(atPath: threadsURL.path) {
            let data = try Data(contentsOf: threadsURL)
            threads = try decoder.decode([ThreadSummary].self, from: data)
        }
        if FileManager.default.fileExists(atPath: accountsURL.path) {
            let data = try Data(contentsOf: accountsURL)
            accounts = try decoder.decode([AccountSnapshot].self, from: data)
        }
    }

    func saveThreads(_ value: [ThreadSummary]) throws {
        threads = value
        try encoder.encode(value).write(to: threadsURL, options: .atomic)
    }

    func saveAccounts(_ value: [AccountSnapshot]) throws {
        accounts = value
        try encoder.encode(value).write(to: accountsURL, options: .atomic)
    }

    func wipeAll() throws {
        threads = []
        accounts = []
        try? FileManager.default.removeItem(at: threadsURL)
        try? FileManager.default.removeItem(at: accountsURL)
    }

    /// Seed demo if empty — demo account never syncs.
    func ensureDemoSeed() throws {
        if accounts.isEmpty {
            let demo = AccountSnapshot(
                accountId: AccountId("local-demo"),
                email: "demo@mailstack.local",
                displayName: "Demo",
                isActive: true
            )
            try saveAccounts([demo])
            try saveThreads(DemoSeed.threads)
        }
    }
}
