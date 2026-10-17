import Foundation
import Combine

@MainActor
final class PadAppContainer: ObservableObject {
    let store: PadStore
    let privacy: PadPrivacy

    init(store: PadStore, privacy: PadPrivacy = PadPrivacy()) {
        self.store = store
        self.privacy = privacy
    }

    static func bootstrap() -> PadAppContainer {
        PadAppContainer(store: PadStore.demo())
    }
}

@MainActor
final class PadStore: ObservableObject {
    @Published var threads: [PadThread]
    @Published var destination: PadDestination = .allInbox
    @Published var selected: PadThread?
    @Published var signedIn: Bool = true
    @Published var accounts: [(id: PadAccountId, email: String)] 
    @Published var syncFraction: Double = 1.0

    init(threads: [PadThread], accounts: [(PadAccountId, String)]) {
        self.threads = threads
        self.accounts = accounts
    }

    static func demo() -> PadStore {
        let id = PadAccountId("local-ipad-demo")
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        func t(_ tid: String, _ sub: String, _ snip: String, _ name: String, _ addr: String, _ domain: String?, unread: Bool, starred: Bool, h: Int) -> PadThread {
            PadThread(threadId: tid, accountId: id, subject: sub, snippet: snip, senderName: name, senderAddress: addr, senderDomain: domain, timestampEpochMs: now - Int64(h)*3_600_000, unread: unread, starred: starred)
        }
        return PadStore(
            threads: [
                t("1", "Welcome to Mailstack on iPad", "Multi-column workspace ready.", "Mailstack", "hello@mailstack.app", "mailstack.app", unread: true, starred: false, h: 1),
                t("2", "Invoice ready", "Monthly invoice attached.", "Billing", "billing@acme.example", "acme.example", unread: true, starred: true, h: 4),
                t("3", "Design review", "Please review the tablet layouts.", "Sam", "sam@work.example", "work.example", unread: false, starred: false, h: 20),
                t("4", "Weekend sale", "20% off selected items.", "Shop", "deals@shop.example", "shop.example", unread: false, starred: false, h: 40),
                t("5", "Security alert", "New sign-in detected.", "Security", "noreply@secure.example", "secure.example", unread: true, starred: false, h: 2),
            ],
            accounts: [(id, "demo@mailstack.local")]
        )
    }

    var filtered: [PadThread] {
        switch destination {
        case .starred: return threads.filter(\.starred)
        case .promotional: return threads.filter { $0.senderDomain?.contains("shop") == true || $0.senderDomain?.contains("digest") == true }
        case .social: return threads.filter { $0.senderDomain == nil }
        case .spam: return []
        default: return threads
        }
    }

    func classify(_ t: PadThread) -> PadCategory {
        let s = (t.subject + t.snippet).lowercased()
        if s.contains("security") || s.contains("invoice") { return .needsAttention }
        if s.contains("sale") { return .promotions }
        if s.contains("review") || s.contains("design") { return .workAndLearning }
        if t.senderDomain != nil { return .updates }
        return .everythingElse
    }
}

struct PadPrivacy {
    func sanitizeHTML(_ html: String) -> String {
        html.replacingOccurrences(of: #"<script[\s\S]*?</script>"#, with: "", options: .regularExpression)
            .replacingOccurrences(of: #"<img[^>]*>"#, with: "", options: .regularExpression)
    }
    var fetchesRemoteImages: Bool { false }
}
