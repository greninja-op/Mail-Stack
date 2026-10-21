import SwiftUI

/// Which local rows the sidebar is showing. All Inbox hides spam.
enum PadListFilter: Equatable {
    case allInbox
    case primary
    case promotional
    case social
    case starred
    case spam

    func includes(_ message: PadMessage) -> Bool {
        switch self {
        case .allInbox: message.category != .spam
        case .primary: message.category == .primary
        case .promotional: message.category == .promotional
        case .social: message.category == .social
        case .starred: message.starred
        case .spam: message.category == .spam
        }
    }
}

/// Flat demo rows on the column background. No card, border, or shadow.
struct PadMailColumn: View {
    var onSelect: (String) -> Void
    var filter: PadListFilter

    /// Column width at which the sender line can also show the category
    /// without dropping the time off the trailing edge.
    private static let wideColumnWidth: CGFloat = 420

    @ObservedObject private var mailbox: PadMailbox
    @Environment(\.colorScheme) private var scheme
    @State private var selectedId: String?
    @State private var columnWidth: CGFloat = 0

    init(
        filter: PadListFilter = .allInbox,
        onSelect: @escaping (String) -> Void,
        mailbox: PadMailbox = .shared
    ) {
        self.filter = filter
        self.onSelect = onSelect
        _mailbox = ObservedObject(wrappedValue: mailbox)
    }

    private var visible: [PadMessage] {
        mailbox.messages.filter { filter.includes($0) }
    }

    var body: some View {
        let showsCategory = columnWidth >= Self.wideColumnWidth
        ScrollView {
            LazyVStack(spacing: 0) {
                if visible.isEmpty {
                    Text("Nothing here")
                        .font(.body)
                        .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                        .frame(maxWidth: .infinity, minHeight: PadColors.rowMinHeight, alignment: .leading)
                        .padding(.horizontal, 16)
                }
                ForEach(visible) { message in
                    PadMailRow(
                        message: message,
                        selected: selectedId == message.id,
                        showsCategory: showsCategory,
                        onSelect: {
                            selectedId = message.id
                            onSelect(message.id)
                        },
                        onStar: {
                            mailbox.toggleStar(message.id)
                        }
                    )
                }
            }
        }
        .scrollContentBackground(.hidden)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background {
            ZStack {
                PadColors.background(scheme)
                GeometryReader { proxy in
                    Color.clear.preference(key: PadMailColumnWidthKey.self, value: proxy.size.width)
                }
            }
        }
        .onPreferenceChange(PadMailColumnWidthKey.self) { columnWidth = $0 }
        .onChange(of: filter) { _, _ in
            selectedId = nil
        }
    }
}

/// Three lines: sender and time, subject, snippet. On a wide column the
/// category name sits on the sender line as a small secondary label.
/// The letter circle is the sender initial — no generated logo.
private struct PadMailRow: View {
    let message: PadMessage
    let selected: Bool
    let showsCategory: Bool
    let onSelect: () -> Void
    let onStar: () -> Void

    @Environment(\.colorScheme) private var scheme
    @State private var hovered = false

    var body: some View {
        HStack(alignment: .center, spacing: 4) {
            Button(action: onSelect) {
                HStack(alignment: .center, spacing: 16) {
                    Text(message.initial)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(PadColors.onPrimaryContainer)
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                        .frame(width: PadColors.avatar, height: PadColors.avatar)
                        .background(PadColors.primaryContainer, in: Circle())
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: 2) {
                        senderLine
                        Text(message.subject)
                            .font(.subheadline.weight(message.unread ? .semibold : .regular))
                            .foregroundStyle(PadColors.onSurface(scheme))
                            .lineLimit(1)
                            .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
                        Text(message.snippet)
                            .font(.subheadline)
                            .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                            .lineLimit(1)
                            .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
                    }
                    .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
                }
                .frame(maxWidth: .infinity, minHeight: PadColors.rowMinHeight, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(openLabel)
            .accessibilityAddTraits(selected ? .isSelected : [])

            Button(action: onStar) {
                Image(systemName: message.starred ? "star.fill" : "star")
                    .font(.system(size: 18))
                    .foregroundStyle(
                        message.starred
                            ? PadColors.brand(scheme)
                            : PadColors.onSurfaceVariant(scheme)
                    )
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(message.starred ? "Starred" : "Star")
            .accessibilityHint(message.starred ? "Removes the star" : "Stars this message")
            .accessibilityAddTraits(message.starred ? .isSelected : [])
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
        .background(rowFill)
        .onHover { hovered = $0 }
    }

    private var senderLine: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(message.sender)
                .font(.body.weight(message.unread ? .semibold : .regular))
                .foregroundStyle(PadColors.onSurface(scheme))
                .lineLimit(1)
                .layoutPriority(0)

            if showsCategory {
                Text(message.category.label)
                    .font(.caption)
                    .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
                    .layoutPriority(1)
            }

            Spacer(minLength: 8)

            Text(message.timeLabel)
                .font(.caption)
                .foregroundStyle(
                    message.unread
                        ? PadColors.brand(scheme)
                        : PadColors.onSurfaceVariant(scheme)
                )
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
                .layoutPriority(2)
        }
        .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
    }

    private var rowFill: Color {
        if selected { return PadColors.surfaceContainerHigh(scheme) }
        if hovered { return PadColors.surfaceContainer(scheme) }
        return Color.clear
    }

    private var openLabel: String {
        let state = message.unread ? "Unread" : "Read"
        return "\(state), \(message.sender), \(message.category.label), \(message.subject), \(message.timeLabel)"
    }
}

private struct PadMailColumnWidthKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}
