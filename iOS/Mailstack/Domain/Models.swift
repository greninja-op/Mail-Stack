import Foundation

struct AccountId: Hashable, Codable, Sendable {
    let value: String
    init(_ value: String) {
        precondition(!value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        self.value = value
    }
}

enum MailDestination: String, CaseIterable, Identifiable, Sendable {
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
    var systemImage: String {
        switch self {
        case .allInbox: return "tray.full"
        case .primary: return "tray"
        case .promotional: return "tag"
        case .social: return "person.2"
        case .spam: return "exclamationmark.octagon"
        case .starred: return "star"
        }
    }
}

enum CategoryGroup: String, CaseIterable, Identifiable, Sendable {
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
    var accent: UInt32 {
        switch self {
        case .needsAttention: return 0xE6A817
        case .promotions: return 0xE07020
        case .updates: return 0x2BA3D4
        case .workAndLearning: return 0x3B5BDB
        case .everythingElse: return 0x5238E8
        }
    }
    var systemImage: String {
        switch self {
        case .needsAttention: return "exclamationmark.circle"
        case .promotions: return "megaphone"
        case .updates: return "bell"
        case .workAndLearning: return "briefcase"
        case .everythingElse: return "tray"
        }
    }
}

struct ThreadSummary: Identifiable, Hashable, Sendable {
    var id: String { threadId }
    let threadId: String
    let accountId: AccountId
    let subject: String
    let snippet: String
    let senderName: String
    let senderAddress: String
    let senderDomain: String?
    let timestampEpochMs: Int64
    let unread: Bool
    var starred: Bool
    let accountBadgeLabel: String?
}

struct AccountSnapshot: Identifiable, Hashable, Sendable {
    var id: String { accountId.value }
    let accountId: AccountId
    let email: String
    let displayName: String?
    var isActive: Bool
}

enum SyncStage: String, Sendable {
    case idle, connecting, fetching, saving, processing, finalizing, succeeded, failed
}

enum SyncProgress: Sendable {
    case idle
    case running(stage: SyncStage, messagesProcessed: Int, totalEstimate: Int?)
    case succeeded(messagesProcessed: Int)
    case failed(message: String)
}

struct StructuredQuery: Sendable {
    var freeText: String? = nil
    var from: String? = nil
    var to: String? = nil
    var subject: String? = nil
    var hasAttachment: Bool? = nil
    var isUnread: Bool? = nil
    var isStarred: Bool? = nil
}

struct ProposedAction: Identifiable, Sendable {
    let id: String
    let title: String
    let rationale: String
    let reversible: Bool
}
