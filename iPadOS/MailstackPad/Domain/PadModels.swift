import Foundation
import Combine

struct PadAccountId: Hashable, Codable {
    let value: String
    init(_ value: String) {
        precondition(!value.trimmingCharacters(in: .whitespaces).isEmpty)
        self.value = value
    }
}

enum PadDestination: String, CaseIterable, Identifiable {
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

struct PadThread: Identifiable, Hashable {
    var id: String { threadId }
    let threadId: String
    let accountId: PadAccountId
    let subject: String
    let snippet: String
    let senderName: String
    let senderAddress: String
    let senderDomain: String?
    let timestampEpochMs: Int64
    let unread: Bool
    var starred: Bool
}

enum PadCategory: String, CaseIterable, Identifiable {
    case needsAttention, promotions, updates, workAndLearning, everythingElse
    var id: String { rawValue }
    var title: String {
        switch self {
        case .needsAttention: return "Needs attention"
        case .promotions: return "Promotions"
        case .updates: return "Updates"
        case .workAndLearning: return "Work & learning"
        case .everythingElse: return "Everything else"
        }
    }
}
