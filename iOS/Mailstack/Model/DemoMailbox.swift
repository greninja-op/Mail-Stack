import SwiftUI

enum MailBucket: String, CaseIterable, Identifiable {
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

enum MailFolder: String, CaseIterable, Identifiable {
    case inbox, starred, spam
    var id: String { rawValue }
    var label: String {
        switch self {
        case .inbox: "All Inbox"
        case .starred: "Starred"
        case .spam: "Spam"
        }
    }
}

struct DemoMessage: Identifiable, Equatable {
    let id: String
    var sender: String
    var address: String
    var subject: String
    var snippet: String
    var body: String
    var timeLabel: String
    var unread: Bool
    var starred: Bool
    var bucket: MailBucket
    var company: String?
    var accountLabel: String

    var initial: String {
        String(sender.first ?? "?").uppercased()
    }
}

/// Local demo mailbox copied from Android `SampleMailboxSeeder` subjects.
/// Not a live Gmail account and not a sync result.
@MainActor
final class DemoMailbox: ObservableObject {
    @Published var messages: [DemoMessage]

    init() {
        messages = DemoMailbox.seed
    }

    func toggleStar(_ id: String) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].starred.toggle()
    }

    func markRead(_ id: String) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        messages[index].unread = false
    }

    func message(_ id: String) -> DemoMessage? {
        messages.first { $0.id == id }
    }

    func visible(folder: MailFolder, bucket: MailBucket) -> [DemoMessage] {
        messages.filter { message in
            switch folder {
            case .starred:
                return message.starred
            case .spam:
                return message.bucket == .spam
            case .inbox:
                return message.bucket != .spam && (bucket == .primary ? message.bucket == .primary : message.bucket == bucket)
            }
        }
    }

    static let accountName = "Demo inbox"
    static let accountEmail = "demo@mailstack.local"

    private static let seed: [DemoMessage] = [
        DemoMessage(id: "m1", sender: "Amazon", address: "auto-confirm@amazon.com", subject: "Your Amazon order has been delivered", snippet: "Your package was left at the door.", body: "Your Amazon order has been delivered. This is local demo mail, not a live order.", timeLabel: "9:14 AM", unread: true, starred: false, bucket: .primary, company: "Amazon", accountLabel: accountEmail),
        DemoMessage(id: "m2", sender: "Ananya Sharma", address: "ananya@example.com", subject: "Ananya shared a design with you: Q3 Launch Deck", snippet: "Take a look when you have a minute.", body: "Ananya shared a design with you. Local demo only.", timeLabel: "Yesterday", unread: false, starred: true, bucket: .primary, company: nil, accountLabel: accountEmail),
        DemoMessage(id: "m3", sender: "Netflix", address: "info@netflix.com", subject: "New sign-in to your Netflix account", snippet: "A new device signed in.", body: "Security-style demo notice. No live account was checked.", timeLabel: "Mon", unread: true, starred: false, bucket: .primary, company: "Netflix", accountLabel: accountEmail),
        DemoMessage(id: "m4", sender: "GitHub", address: "noreply@github.com", subject: "A pull request was merged: Fix login redirect loop", snippet: "The pull request is now merged.", body: "Demo notification from the Android sample mailbox.", timeLabel: "Sun", unread: false, starred: false, bucket: .social, company: "GitHub", accountLabel: accountEmail),
        DemoMessage(id: "m5", sender: "Hiring Desk", address: "jobs@example.com", subject: "Task assigned: Review Q3 roadmap", snippet: "Please review the roadmap this week.", body: "A demo task mention. Nothing was created in a real task list.", timeLabel: "8:02 AM", unread: true, starred: false, bucket: .primary, company: nil, accountLabel: accountEmail),
        DemoMessage(id: "m6", sender: "Spotify", address: "no-reply@spotify.com", subject: "Your receipt from Spotify", snippet: "Thanks for your payment.", body: "Demo receipt. No payment was made.", timeLabel: "Tue", unread: false, starred: false, bucket: .promotional, company: "Spotify", accountLabel: accountEmail),
        DemoMessage(id: "m7", sender: "Adobe", address: "mail@adobe.com", subject: "Your Creative Cloud subscription renews in 7 days", snippet: "Your plan renews soon.", body: "Demo renewal notice. Not a live subscription.", timeLabel: "Tue", unread: true, starred: false, bucket: .promotional, company: "Adobe", accountLabel: accountEmail),
        DemoMessage(id: "m8", sender: "Fireship", address: "noreply@youtube.com", subject: "Fireship uploaded: Kotlin in 100 Seconds", snippet: "A new video is up.", body: "Demo social notification.", timeLabel: "Sat", unread: false, starred: false, bucket: .social, company: "YouTube", accountLabel: accountEmail),
        DemoMessage(id: "m9", sender: "Unknown", address: "prize@example.invalid", subject: "CONGRATULATIONS! You have won the lottery", snippet: "This looks like spam.", body: "Demo spam. Do not treat this as a real message.", timeLabel: "Fri", unread: true, starred: false, bucket: .spam, company: nil, accountLabel: accountEmail),
        DemoMessage(id: "m10", sender: "AWS", address: "no-reply@amazonaws.com", subject: "Your AWS bill for September 2026", snippet: "Your monthly bill is ready.", body: "Demo bill. No AWS account is connected.", timeLabel: "Oct 2", unread: false, starred: false, bucket: .primary, company: "Amazon Web Services", accountLabel: accountEmail)
    ]
}
