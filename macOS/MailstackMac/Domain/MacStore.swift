import Foundation
import Combine

enum MacDestination: String, CaseIterable, Identifiable {
    case allInbox, primary, promotional, social, spam, starred
    var id: String { rawValue }
    var title: String {
        switch self {
        case .allInbox: return "All Inbox"
        case .primary: return "Primary"
        case .promotional: return "Promotional"
        case .social: return "Social"
        case .spam: return "Spam"
        case .starred: return "Starred"
        }
    }
}

struct MacThread: Identifiable, Hashable {
    var id: String { threadId }
    let threadId: String
    let subject: String
    let snippet: String
    let senderName: String
    let senderAddress: String
    let unread: Bool
    var starred: Bool
}

@MainActor
final class MacStore: ObservableObject {
    @Published var threads: [MacThread]
    @Published var destination: MacDestination = .allInbox
    @Published var selected: MacThread?
    let accountLabel = "local-desktop-demo"

    init(threads: [MacThread]) { self.threads = threads }

    static func demo() -> MacStore {
        MacStore(threads: [
            MacThread(threadId: "t1", subject: "Welcome to Mailstack Desktop", snippet: "Rust core + SwiftUI shell.", senderName: "Mailstack", senderAddress: "hello@mailstack.app", unread: true, starred: false),
            MacThread(threadId: "t2", subject: "Invoice ready", snippet: "Monthly invoice.", senderName: "Billing", senderAddress: "billing@acme.example", unread: true, starred: true),
            MacThread(threadId: "t3", subject: "Security alert", snippet: "New sign-in.", senderName: "Security", senderAddress: "noreply@secure.example", unread: true, starred: false),
            MacThread(threadId: "t4", subject: "Weekend sale", snippet: "Save 20%.", senderName: "Shop", senderAddress: "deals@shop.example", unread: false, starred: false),
        ])
    }

    var filtered: [MacThread] {
        switch destination {
        case .starred: return threads.filter(\.starred)
        case .promotional: return threads.filter { $0.senderAddress.contains("shop") }
        case .spam: return []
        default: return threads
        }
    }
}
