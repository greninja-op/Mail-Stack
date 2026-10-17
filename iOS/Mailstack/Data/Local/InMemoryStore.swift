import Foundation
import Combine

/// Local demo store (Phase 2). Demo account `local-demo` never syncs to Gmail.
@MainActor
final class InMemoryStore: ObservableObject {
    @Published var accounts: [AccountSnapshot] = [
        AccountSnapshot(
            accountId: AccountId("local-demo"),
            email: "demo@mailstack.local",
            displayName: "Demo",
            isActive: true
        )
    ]
    @Published var threads: [ThreadSummary] = DemoSeed.threads
    @Published var syncProgress: SyncProgress = .idle
    @Published var isOnline: Bool = true
    @Published var isSignedIn: Bool = true

    var activeAccountId: AccountId? {
        accounts.first(where: { $0.isActive })?.accountId
    }
}

enum DemoSeed {
    static let account = AccountId("local-demo")
    static var threads: [ThreadSummary] {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        func t(
            _ id: String, _ subject: String, _ snippet: String,
            _ name: String, _ addr: String, _ domain: String?,
            unread: Bool, starred: Bool, hours: Int
        ) -> ThreadSummary {
            ThreadSummary(
                threadId: id,
                accountId: account,
                subject: subject,
                snippet: snippet,
                senderName: name,
                senderAddress: addr,
                senderDomain: domain,
                timestampEpochMs: now - Int64(hours) * 3_600_000,
                unread: unread,
                starred: starred,
                accountBadgeLabel: "D"
            )
        }
        return [
            t("t1", "Welcome to Mailstack", "Your inbox stays organized on-device.", "Mailstack", "hello@mailstack.app", "mailstack.app", unread: true, starred: false, hours: 1),
            t("t2", "Invoice ready", "Your monthly invoice is ready.", "Billing", "billing@acme.example", "acme.example", unread: true, starred: true, hours: 5),
            t("t3", "Team standup notes", "Notes from today's standup.", "Alex Rivera", "alex@example.com", nil, unread: false, starred: false, hours: 26),
            t("t4", "Flash sale this weekend", "Save 20% — ends Sunday.", "Shop", "deals@shop.example", "shop.example", unread: false, starred: false, hours: 48),
            t("t5", "Security alert", "A new sign-in was detected.", "Security", "noreply@secure.example", "secure.example", unread: true, starred: false, hours: 3),
            t("t6", "Newsletter: October", "Top stories this month.", "Digest", "news@digest.example", "digest.example", unread: false, starred: false, hours: 72),
            t("t7", "Project kickoff", "Agenda and links for kickoff.", "Sam Chen", "sam@work.example", "work.example", unread: true, starred: false, hours: 8),
            t("t8", "Receipt #48291", "Thanks for your purchase.", "Store", "orders@store.example", "store.example", unread: false, starred: false, hours: 96),
        ]
    }
}
