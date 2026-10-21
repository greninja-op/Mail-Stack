import SwiftUI

/// Short Home dashboard for the iPad sidebar destination.
/// The sample lives in this file. It is a local demo, not a Gmail account.
public struct PadHomeColumn: View {
    @Environment(\.colorScheme) private var scheme

    public init() {}

    private var items: [PadHomeItem] { PadHomeItem.samples }

    public var body: some View {
        let colors = PadColumnColors(scheme: scheme)
        let unread = items.filter(\.unread).count
        let starred = items.filter(\.starred).count

        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header(colors)
                metrics(
                    colors: colors,
                    tiles: [
                        PadHomeMetric(id: "unread", value: unread, title: "Unread"),
                        PadHomeMetric(id: "starred", value: starred, title: "Starred"),
                        PadHomeMetric(id: "messages", value: items.count, title: "Messages")
                    ]
                )
                recent(colors)
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollContentBackground(.hidden)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(colors.background)
    }

    private func header(_ colors: PadColumnColors) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Demo inbox")
                .font(.title.weight(.semibold))
                .foregroundStyle(colors.onSurface)
            Text("demo@mailstack.local")
                .font(.subheadline)
                .foregroundStyle(colors.onSurfaceVariant)
            Text("This mailbox is a local demo, not a connected Gmail account.")
                .font(.body)
                .foregroundStyle(colors.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func metrics(colors: PadColumnColors, tiles: [PadHomeMetric]) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 12) {
                ForEach(tiles) { tile in
                    metricTile(tile, colors: colors)
                }
            }
            .frame(minWidth: 560, idealWidth: 560, maxWidth: .infinity, alignment: .leading)

            VStack(spacing: 12) {
                ForEach(tiles) { tile in
                    metricTile(tile, colors: colors)
                }
            }
        }
    }

    private func metricTile(_ tile: PadHomeMetric, colors: PadColumnColors) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("\(tile.value)")
                .font(.title.weight(.semibold))
                .foregroundStyle(colors.brand)
            Text(tile.title)
                .font(.subheadline)
                .foregroundStyle(colors.onSurfaceVariant)
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 88, alignment: .leading)
        .background(colors.card, in: RoundedRectangle(cornerRadius: colors.radius, style: .continuous))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(tile.spoken)
    }

    private func recent(_ colors: PadColumnColors) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                Text("Recent")
                    .font(.headline)
                    .foregroundStyle(colors.onSurface)
                Spacer(minLength: 8)
                Text("\(items.count)")
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Recent, \(padMessageCount(items.count))")

            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: 24) {
                    recentColumn(Array(items.prefix(columnSplit)), colors: colors)
                    recentColumn(Array(items.suffix(items.count - columnSplit)), colors: colors)
                }
                .frame(minWidth: 760, idealWidth: 760, maxWidth: .infinity, alignment: .topLeading)

                recentColumn(items, colors: colors)
            }
        }
    }

    private var columnSplit: Int {
        (items.count + 1) / 2
    }

    private func recentColumn(_ rows: [PadHomeItem], colors: PadColumnColors) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(rows.enumerated()), id: \.element.id) { index, item in
                homeRow(item, colors: colors)
                if index < rows.count - 1 {
                    Rectangle()
                        .fill(colors.hairline)
                        .frame(height: 1)
                        .padding(.leading, 16 + 40 + 16)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func homeRow(_ item: PadHomeItem, colors: PadColumnColors) -> some View {
        HStack(alignment: .center, spacing: 16) {
            padLetter(item.sender, colors: colors)
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(item.sender)
                        .font(.body.weight(item.unread ? .semibold : .regular))
                        .foregroundStyle(colors.onSurface)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    if item.starred {
                        Image(systemName: "star.fill")
                            .font(.caption)
                            .foregroundStyle(colors.brand)
                            .accessibilityHidden(true)
                    }
                    Text(item.time)
                        .font(.caption)
                        .foregroundStyle(item.unread ? colors.brand : colors.onSurfaceVariant)
                        .lineLimit(1)
                }
                Text(item.subject)
                    .font(.subheadline.weight(item.unread ? .semibold : .regular))
                    .foregroundStyle(colors.onSurface)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(item.snippet)
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: 88, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(homeLabel(item))
    }

    private func homeLabel(_ item: PadHomeItem) -> String {
        let state = item.unread ? "Unread" : "Read"
        let star = item.starred ? "Starred" : "Not starred"
        return "\(state), \(star), \(item.sender), \(item.subject), \(item.time)"
    }
}

private struct PadHomeItem: Identifiable {
    let id: String
    let sender: String
    let subject: String
    let snippet: String
    let time: String
    let unread: Bool
    let starred: Bool

    static let samples: [PadHomeItem] = [
        PadHomeItem(id: "h1", sender: "Netflix", subject: "New sign-in to your Netflix account", snippet: "A new device signed in.", time: "Mon", unread: true, starred: false),
        PadHomeItem(id: "h2", sender: "Ananya Sharma", subject: "Ananya shared a design with you: Q3 Launch Deck", snippet: "Take a look when you have a minute.", time: "Yesterday", unread: false, starred: true),
        PadHomeItem(id: "h3", sender: "Hiring Desk", subject: "Task assigned: Review Q3 roadmap", snippet: "Please review the roadmap this week.", time: "8:02 AM", unread: true, starred: false),
        PadHomeItem(id: "h4", sender: "Amazon", subject: "Your Amazon order has been delivered", snippet: "Your package was left at the door.", time: "9:14 AM", unread: true, starred: false),
        PadHomeItem(id: "h5", sender: "Spotify", subject: "Your receipt from Spotify", snippet: "Thanks for your payment.", time: "Tue", unread: false, starred: false),
        PadHomeItem(id: "h6", sender: "GitHub", subject: "A pull request was merged: Fix login redirect loop", snippet: "The pull request is now merged.", time: "Sun", unread: false, starred: false)
    ]
}

private struct PadHomeMetric: Identifiable {
    let id: String
    let value: Int
    let title: String

    var spoken: String {
        let word: String
        switch id {
        case "unread":
            word = value == 1 ? "unread message" : "unread messages"
        case "starred":
            word = value == 1 ? "starred message" : "starred messages"
        default:
            word = value == 1 ? "message" : "messages"
        }
        return "\(value) \(word)"
    }
}

private struct PadColumnColors {
    let scheme: ColorScheme

    var brand: Color { scheme == .dark ? rgb(0xB9A8FF) : rgb(0x5238E8) }
    var background: Color { scheme == .dark ? rgb(0x131120) : rgb(0xF6F3FF) }
    var card: Color { scheme == .dark ? rgb(0x262340) : rgb(0xEFEAFB) }
    var onSurface: Color { scheme == .dark ? rgb(0xF0EDFB) : rgb(0x1C1A26) }
    var onSurfaceVariant: Color { scheme == .dark ? rgb(0xCBC6DE) : rgb(0x48455A) }
    var letterFill: Color { scheme == .dark ? rgb(0x4634A8) : rgb(0xE3DCFF) }
    var letterInk: Color { scheme == .dark ? rgb(0xE3DCFF) : rgb(0x1D0F6E) }
    var hairline: Color { onSurface.opacity(0.08) }
    var radius: CGFloat { 28 }

    private func rgb(_ hex: UInt32) -> Color {
        Color(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

private func padLetter(_ name: String, colors: PadColumnColors) -> some View {
    Text(String(name.first ?? "?").uppercased())
        .font(.body.weight(.semibold))
        .foregroundStyle(colors.letterInk)
        .frame(width: 40, height: 40)
        .background(colors.letterFill, in: Circle())
        .accessibilityHidden(true)
}

private func padMessageCount(_ count: Int) -> String {
    count == 1 ? "1 message" : "\(count) messages"
}

#Preview("Home") {
    PadHomeColumn()
}
