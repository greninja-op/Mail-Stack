import SwiftUI

/// Flat Gmail-style row — no card, no border, no shadow.
struct MailRowView: View {
    let thread: ThreadSummary
    var onStar: (() -> Void)?

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            AvatarView(name: thread.senderName, domain: thread.senderDomain)
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(thread.senderName)
                        .font(.system(size: MSTypography.bodyLarge, weight: thread.unread ? .bold : .regular))
                        .lineLimit(1)
                    Spacer()
                    Text(Self.formatTime(thread.timestampEpochMs))
                        .font(.system(size: MSTypography.labelSmall, weight: thread.unread ? .semibold : .regular))
                        .foregroundStyle(thread.unread ? Color.accentColor : .secondary)
                }
                Text(thread.subject)
                    .font(.system(size: MSTypography.bodyMedium, weight: thread.unread ? .semibold : .regular))
                    .lineLimit(1)
                Text(thread.snippet)
                    .font(.system(size: MSTypography.bodyMedium))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            if thread.starred {
                Button {
                    onStar?()
                } label: {
                    Image(systemName: "star.fill")
                        .foregroundStyle(.yellow)
                        .frame(width: MSShape.minHitTarget, height: MSShape.minHitTarget)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Starred")
            }
            if let badge = thread.accountBadgeLabel {
                Text(badge)
                    .font(.system(size: 10, weight: .bold))
                    .frame(width: 18, height: 18)
                    .background(Circle().fill(Color.secondary.opacity(0.25)))
                    .accessibilityLabel("Account \(badge)")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(minHeight: MSShape.mailRowMinHeight)
        .contentShape(Rectangle())
    }

    private static func formatTime(_ ms: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(ms) / 1000)
        let f = DateFormatter()
        f.dateStyle = .short
        f.timeStyle = .none
        return f.string(from: date)
    }
}

struct AvatarView: View {
    let name: String
    let domain: String?

    var body: some View {
        ZStack {
            Circle()
                .fill(Color.accentColor.opacity(0.18))
            Text(letter)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(Color.accentColor)
        }
        .frame(width: MSShape.avatarSize, height: MSShape.avatarSize)
        .accessibilityHidden(true)
    }

    private var letter: String {
        String(name.trimmingCharacters(in: .whitespaces).prefix(1)).uppercased()
    }
}
