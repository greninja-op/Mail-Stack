import SwiftUI

enum PadMailCategory: String, CaseIterable, Identifiable {
    case primary, promotional, social, spam

    var id: String { rawValue }

    var label: String {
        switch self {
        case .primary: "Primary"
        case .promotional: "Promotional"
        case .social: "Social"
        case .spam: "Spam"
        }
    }
}

struct PadMessage: Identifiable, Equatable {
    let id: String
    var sender: String
    var address: String
    var subject: String
    var snippet: String
    var body: String
    var timeLabel: String
    var unread: Bool
    var starred: Bool
    var category: PadMailCategory

    var initial: String {
        String(sender.first ?? "?").uppercased()
    }
}

/// In-memory demo mailbox. These eight messages are not a Gmail account
/// and are never fetched from the network.
@MainActor
final class PadMailbox: ObservableObject {
    static let shared = PadMailbox()

    @Published private(set) var messages: [PadMessage]

    private init() {
        messages = Self.seed
    }

    func message(_ id: String) -> PadMessage? {
        messages.first { $0.id == id }
    }

    func toggleStar(_ id: String) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        var updated = messages
        updated[index].starred.toggle()
        messages = updated
    }

    func markRead(_ id: String) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        guard messages[index].unread else { return }
        var updated = messages
        updated[index].unread = false
        messages = updated
    }

    private static let seed: [PadMessage] = [
        PadMessage(
            id: "amazon-delivered",
            sender: "Amazon",
            address: "auto-confirm@amazon.com",
            subject: "Your Amazon order has been delivered",
            snippet: "Your package was left at the door.",
            body: "Your Amazon order has been delivered. This is local demo mail, not a live order.",
            timeLabel: "9:14 AM",
            unread: true,
            starred: false,
            category: .primary
        ),
        PadMessage(
            id: "ananya-design",
            sender: "Ananya Sharma",
            address: "ananya@example.com",
            subject: "Ananya shared a design with you: Q3 Launch Deck",
            snippet: "Take a look when you have a minute.",
            body: "Ananya shared a design with you. Local demo only.",
            timeLabel: "Yesterday",
            unread: false,
            starred: true,
            category: .primary
        ),
        PadMessage(
            id: "netflix-sign-in",
            sender: "Netflix",
            address: "info@netflix.com",
            subject: "New sign-in to your Netflix account",
            snippet: "A new device signed in.",
            body: "Security-style demo notice. No live account was checked.",
            timeLabel: "Mon",
            unread: true,
            starred: false,
            category: .primary
        ),
        PadMessage(
            id: "github-pr",
            sender: "GitHub",
            address: "noreply@github.com",
            subject: "A pull request was merged: Fix login redirect loop",
            snippet: "The pull request is now merged.",
            body: "Demo notification. No repository was contacted.",
            timeLabel: "Sun",
            unread: false,
            starred: false,
            category: .social
        ),
        PadMessage(
            id: "q3-roadmap",
            sender: "Hiring Desk",
            address: "jobs@example.com",
            subject: "Task assigned: Review Q3 roadmap",
            snippet: "Please review the roadmap this week.",
            body: "A demo task mention. Nothing was created in a real task list.",
            timeLabel: "8:02 AM",
            unread: true,
            starred: false,
            category: .primary
        ),
        PadMessage(
            id: "spotify-receipt",
            sender: "Spotify",
            address: "no-reply@spotify.com",
            subject: "Your receipt from Spotify",
            snippet: "Thanks for your payment.",
            body: "Demo receipt. No payment was made.",
            timeLabel: "Tue",
            unread: false,
            starred: false,
            category: .promotional
        ),
        PadMessage(
            id: "adobe-renew",
            sender: "Adobe",
            address: "mail@adobe.com",
            subject: "Your Creative Cloud subscription renews in 7 days",
            snippet: "Your plan renews soon.",
            body: "Demo renewal notice. Not a live subscription.",
            timeLabel: "Tue",
            unread: true,
            starred: false,
            category: .promotional
        ),
        PadMessage(
            id: "spam-lottery",
            sender: "Unknown",
            address: "prize@example.invalid",
            subject: "CONGRATULATIONS! You have won the lottery",
            snippet: "This looks like spam.",
            body: "Demo spam. Do not treat this as a real message.",
            timeLabel: "Fri",
            unread: true,
            starred: false,
            category: .spam
        )
    ]
}
