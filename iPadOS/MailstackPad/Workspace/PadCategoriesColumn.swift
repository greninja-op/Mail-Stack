import SwiftUI

/// Category groups for the iPad sidebar destination.
/// One 28pt card per group that has demo items. Members expand inside that card.
public struct PadCategoriesColumn: View {
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var expanded: Set<String> = ["needs-attention"]

    public init() {}

    private var groups: [PadCategoryGroup] { PadCategoryGroup.groups }

    public var body: some View {
        let colors = PadColumnColors(scheme: scheme)
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Groups from the local demo mailbox.")
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ForEach(groups) { group in
                    categoryCard(group, colors: colors)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollContentBackground(.hidden)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(colors.background)
    }

    private func categoryCard(_ group: PadCategoryGroup, colors: PadColumnColors) -> some View {
        let isExpanded = expanded.contains(group.id)
        let shape = RoundedRectangle(cornerRadius: colors.radius, style: .continuous)
        return VStack(spacing: 0) {
            Button {
                toggle(group.id)
            } label: {
                header(group, isExpanded: isExpanded, colors: colors)
            }
            .buttonStyle(PadHeaderButtonStyle())
            .accessibilityLabel("\(group.title), \(padMessageCount(group.messages.count))")
            .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")
            .accessibilityHint(isExpanded ? "Collapses the group" : "Expands the group")

            if isExpanded {
                ForEach(group.messages) { message in
                    memberRow(message, colors: colors)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: shape)
        .clipShape(shape)
    }

    private func header(_ group: PadCategoryGroup, isExpanded: Bool, colors: PadColumnColors) -> some View {
        let accent = group.accent(scheme)
        return HStack(alignment: .center, spacing: 16) {
            Image(systemName: group.symbol)
                .font(.body.weight(.semibold))
                .foregroundStyle(accent)
                .frame(width: 40, height: 40)
                .background(accent.opacity(0.14), in: Circle())
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(group.title)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(colors.onSurface)
                    .lineLimit(1)
                Text(padMessageCount(group.messages.count))
                    .font(.caption)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            Image(systemName: "chevron.down")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(colors.onSurfaceVariant)
                .rotationEffect(.degrees(isExpanded ? 180 : 0))
                .frame(width: 24, height: 24)
                .accessibilityHidden(true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, minHeight: 64, alignment: .center)
        .contentShape(Rectangle())
    }

    private func memberRow(_ message: PadCategoryMessage, colors: PadColumnColors) -> some View {
        HStack(alignment: .center, spacing: 16) {
            padLetter(message.sender, colors: colors)
            VStack(alignment: .leading, spacing: 2) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(message.sender)
                        .font(.body.weight(message.unread ? .semibold : .regular))
                        .foregroundStyle(colors.onSurface)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    Text(message.time)
                        .font(.caption)
                        .foregroundStyle(message.unread ? colors.brand : colors.onSurfaceVariant)
                        .lineLimit(1)
                }
                Text(message.subject)
                    .font(.subheadline.weight(message.unread ? .semibold : .regular))
                    .foregroundStyle(colors.onSurface)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(message.snippet)
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .frame(maxWidth: .infinity, minHeight: 88, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(message.unread ? "Unread" : "Read"), \(message.sender), \(message.subject), \(message.time)")
    }

    private func toggle(_ id: String) {
        let change = {
            if expanded.contains(id) {
                expanded.remove(id)
            } else {
                expanded.insert(id)
            }
        }
        if reduceMotion {
            change()
        } else {
            withAnimation(.easeInOut(duration: 0.2)) {
                change()
            }
        }
    }
}

private struct PadHeaderButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? Color.primary.opacity(0.08) : Color.clear)
    }
}

private struct PadCategoryMessage: Identifiable {
    let id: String
    let sender: String
    let subject: String
    let snippet: String
    let time: String
    let unread: Bool
    let kind: PadDemoKind
}

private enum PadDemoKind {
    case primary
    case promotional
    case social
}

private struct PadCategoryGroup: Identifiable {
    let id: String
    let title: String
    let symbol: String
    let messages: [PadCategoryMessage]

    func accent(_ scheme: ColorScheme) -> Color {
        let dark = scheme == .dark
        switch id {
        case "needs-attention":
            return dark ? Color(red: 245 / 255, green: 193 / 255, blue: 108 / 255) : Color(red: 180 / 255, green: 83 / 255, blue: 9 / 255)
        case "promotions":
            return dark ? Color(red: 255 / 255, green: 176 / 255, blue: 116 / 255) : Color(red: 230 / 255, green: 126 / 255, blue: 34 / 255)
        case "updates":
            return dark ? Color(red: 142 / 255, green: 202 / 255, blue: 255 / 255) : Color(red: 59 / 255, green: 130 / 255, blue: 196 / 255)
        case "work":
            return dark ? Color(red: 168 / 255, green: 196 / 255, blue: 255 / 255) : Color(red: 29 / 255, green: 78 / 255, blue: 216 / 255)
        default:
            return dark ? Color(red: 185 / 255, green: 168 / 255, blue: 255 / 255) : Color(red: 82 / 255, green: 56 / 255, blue: 232 / 255)
        }
    }

    /// Empty groups are omitted. A message may qualify for more than one group.
    static let groups: [PadCategoryGroup] = {
        let messages: [PadCategoryMessage] = [
            PadCategoryMessage(id: "c1", sender: "Netflix", subject: "New sign-in to your Netflix account", snippet: "A new device signed in.", time: "Mon", unread: true, kind: .primary),
            PadCategoryMessage(id: "c2", sender: "Spotify", subject: "Your receipt from Spotify", snippet: "Thanks for your payment.", time: "Tue", unread: false, kind: .promotional),
            PadCategoryMessage(id: "c3", sender: "Adobe", subject: "Your Creative Cloud subscription renews in 7 days", snippet: "Your plan renews soon.", time: "Tue", unread: true, kind: .promotional),
            PadCategoryMessage(id: "c4", sender: "AWS", subject: "Your AWS bill for September 2026", snippet: "Your monthly bill is ready.", time: "Oct 2", unread: false, kind: .primary),
            PadCategoryMessage(id: "c5", sender: "GitHub", subject: "A pull request was merged: Fix login redirect loop", snippet: "The pull request is now merged.", time: "Sun", unread: false, kind: .social),
            PadCategoryMessage(id: "c6", sender: "Hiring Desk", subject: "Task assigned: Review Q3 roadmap", snippet: "Please review the roadmap this week.", time: "8:02 AM", unread: true, kind: .primary),
            PadCategoryMessage(id: "c7", sender: "Fireship", subject: "Fireship uploaded: Kotlin in 100 Seconds", snippet: "A new video is up.", time: "Sat", unread: false, kind: .social)
        ]
        let specs: [(String, String, String, (PadCategoryMessage) -> Bool)] = [
            (
                "needs-attention",
                "Needs attention",
                "exclamationmark.shield",
                { message in
                    message.unread && (
                        padContains(message.subject, "sign-in")
                            || padContains(message.subject, "Security")
                            || padContains(message.subject, "alert")
                    )
                }
            ),
            (
                "promotions",
                "Promotions",
                "tag",
                { $0.kind == .promotional }
            ),
            (
                "updates",
                "Updates",
                "arrow.triangle.2.circlepath",
                { message in
                    padContains(message.subject, "bill")
                        || padContains(message.subject, "receipt")
                        || padContains(message.subject, "renew")
                }
            ),
            (
                "work",
                "Work",
                "briefcase",
                { message in
                    padContains(message.subject, "pull request")
                        || padContains(message.subject, "Task")
                        || padContains(message.subject, "roadmap")
                }
            ),
            (
                "everything-else",
                "Everything else",
                "ellipse",
                { $0.kind == .social }
            )
        ]
        return specs.compactMap { id, title, symbol, include in
            let members = messages.filter(include)
            guard !members.isEmpty else { return nil }
            return PadCategoryGroup(id: id, title: title, symbol: symbol, messages: members)
        }
    }()
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

private func padContains(_ subject: String, _ needle: String) -> Bool {
    subject.range(of: needle, options: .caseInsensitive) != nil
}

#Preview("Categories") {
    PadCategoriesColumn()
}
