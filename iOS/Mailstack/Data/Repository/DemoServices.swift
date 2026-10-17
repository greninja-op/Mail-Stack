import Foundation
import Combine

@MainActor
final class DemoAccountSession: AccountSessionPort {
    private let store: InMemoryStore
    init(store: InMemoryStore) { self.store = store }

    var activeAccountPublisher: AnyPublisher<AccountId?, Never> {
        store.$accounts
            .map { $0.first(where: { $0.isActive })?.accountId }
            .eraseToAnyPublisher()
    }

    func listAccounts() async -> [AccountSnapshot] { store.accounts }

    func switchAccount(_ id: AccountId) async {
        store.accounts = store.accounts.map {
            AccountSnapshot(
                accountId: $0.accountId,
                email: $0.email,
                displayName: $0.displayName,
                isActive: $0.accountId == id
            )
        }
    }

    func addDemoAccount(email: String) async {
        let id = AccountId("local-\(UUID().uuidString.prefix(8))")
        store.accounts.append(
            AccountSnapshot(accountId: id, email: email, displayName: email, isActive: false)
        )
    }
}

@MainActor
final class DemoMailboxReader: MailboxReaderPort {
    private let store: InMemoryStore
    init(store: InMemoryStore) { self.store = store }

    func observeThreads(accountId: AccountId, destination: MailDestination) -> AnyPublisher<[ThreadSummary], Never> {
        store.$threads
            .map { threads in
                threads.filter { $0.accountId == accountId }.filter { t in
                    switch destination {
                    case .allInbox, .primary: return true
                    case .starred: return t.starred
                    case .promotional: return t.senderDomain?.contains("shop") == true || t.senderDomain?.contains("store") == true || t.senderDomain?.contains("digest") == true
                    case .social: return t.senderDomain == nil
                    case .spam: return false
                    }
                }
            }
            .eraseToAnyPublisher()
    }

    func message(accountId: AccountId, threadId: String) async -> ThreadSummary? {
        store.threads.first { $0.threadId == threadId && $0.accountId == accountId }
    }

    func setStarred(threadId: String, starred: Bool) async {
        if let i = store.threads.firstIndex(where: { $0.threadId == threadId }) {
            var t = store.threads[i]
            t.starred = starred
            store.threads[i] = t
        }
    }
}

@MainActor
final class DemoSearch: SearchPort {
    private let store: InMemoryStore
    init(store: InMemoryStore) { self.store = store }

    func search(accountId: AccountId, query: StructuredQuery) async -> [ThreadSummary] {
        store.threads.filter { t in
            guard t.accountId == accountId else { return false }
            if let q = query.freeText?.lowercased(), !q.isEmpty {
                let hay = (t.subject + " " + t.snippet + " " + t.senderName).lowercased()
                if !hay.contains(q) { return false }
            }
            if let from = query.from?.lowercased(), !t.senderAddress.lowercased().contains(from) { return false }
            if let subject = query.subject?.lowercased(), !t.subject.lowercased().contains(subject) { return false }
            if let unread = query.isUnread, t.unread != unread { return false }
            if let starred = query.isStarred, t.starred != starred { return false }
            return true
        }
    }
}

@MainActor
final class DemoClassification: ClassificationPort {
    private let store: InMemoryStore
    init(store: InMemoryStore) { self.store = store }

    func category(for threadId: String) async -> CategoryGroup {
        guard let t = store.threads.first(where: { $0.threadId == threadId }) else {
            return .everythingElse
        }
        return Self.classify(t)
    }

    func grouped(accountId: AccountId) async -> [(CategoryGroup, [ThreadSummary])] {
        let scoped = store.threads.filter { $0.accountId == accountId }
        var map: [CategoryGroup: [ThreadSummary]] = [:]
        for t in scoped {
            map[Self.classify(t), default: []].append(t)
        }
        return CategoryGroup.allCases.compactMap { g in
            guard let rows = map[g], !rows.isEmpty else { return nil }
            return (g, rows)
        }
    }

    static func classify(_ t: ThreadSummary) -> CategoryGroup {
        let s = (t.subject + " " + t.snippet).lowercased()
        if s.contains("security") || s.contains("invoice") || s.contains("alert") {
            return .needsAttention
        }
        if s.contains("sale") || s.contains("newsletter") || s.contains("receipt") {
            return .promotions
        }
        if s.contains("standup") || s.contains("project") || s.contains("kickoff") {
            return .workAndLearning
        }
        if t.senderDomain != nil {
            return .updates
        }
        return .everythingElse
    }
}

@MainActor
final class DemoActionEngine: ActionEnginePort {
    func propose(accountId: AccountId) async -> [ProposedAction] {
        [
            ProposedAction(id: "a1", title: "Star security alerts", rationale: "Security messages should stay visible.", reversible: true),
            ProposedAction(id: "a2", title: "Archive old promotions", rationale: "Promotional mail older than 30 days.", reversible: true),
        ]
    }
    func execute(actionId: String) async -> Bool { true }
}

@MainActor
final class DefaultPrivacy: PrivacyPort {
    func sanitizeHTML(_ html: String) -> String {
        // Strip script/style; block remote images by removing img src.
        var out = html
        let patterns = [
            #"<script[\s\S]*?</script>"#,
            #"<style[\s\S]*?</style>"#,
            #"<img[^>]*>"#,
            #"on\w+\s*="#,
        ]
        for p in patterns {
            out = out.replacingOccurrences(of: p, with: "", options: .regularExpression)
        }
        return out
    }

    func decodeEntities(_ text: String) -> String {
        var s = text
        let map = ["&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": "\"", "&#39;": "'", "&nbsp;": " ", "&bull;": "•"]
        for (k, v) in map { s = s.replacingOccurrences(of: k, with: v) }
        return s
    }

    func shouldFetchRemoteImages() -> Bool { false }
}

@MainActor
final class DefaultOffline: OfflinePort {
    private let subject = CurrentValueSubject<Bool, Never>(true)
    var isOnlinePublisher: AnyPublisher<Bool, Never> { subject.eraseToAnyPublisher() }
}

@MainActor
final class StubAI: AIPort {
    var isEnabled: Bool { false }
    func summarize(threadId: String) async -> String? { nil }
}

@MainActor
final class StubAutomation: AutomationPort {
    func listRules() async -> [String] { [] }
}

@MainActor
final class SafeDiagnostics: DiagnosticsPort {
    func log(_ event: String, fields: [String: String]) {
        // Never log tokens, refresh tokens, or full email bodies.
        let banned = ["token", "refresh", "password", "authorization", "cookie"]
        for (k, v) in fields {
            let key = k.lowercased()
            if banned.contains(where: { key.contains($0) }) { return }
            if v.count > 200 { return }
        }
        #if DEBUG
        print("[Mailstack]", event, fields)
        #endif
    }
}
