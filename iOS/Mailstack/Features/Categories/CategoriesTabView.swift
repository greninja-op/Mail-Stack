import SwiftUI

/// Categories for the local demo mailbox. Each group is one rounded card.
/// Members expand inside that same card. Names come only from demo messages.
struct CategoriesTabView: View {
    @ObservedObject private var model: AppModel
    @ObservedObject private var mailbox: DemoMailbox
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var expanded: Set<String> = []

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
        _mailbox = ObservedObject(wrappedValue: model.mailbox)
    }

    var body: some View {
        let groups = groups(in: mailbox.messages)
        Group {
            if groups.isEmpty {
                emptyState
            } else {
                BarHidingScroll(model: model) {
                    LazyVStack(spacing: 8) {
                        ForEach(groups) { group in
                            categoryCard(group)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(MailstackColors.background(scheme))
    }

    private var emptyState: some View {
        Text("No messages in these groups.")
            .font(.body)
            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
            .multilineTextAlignment(.center)
            .padding(24)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func categoryCard(_ group: MailGroup) -> some View {
        let isExpanded = expanded.contains(group.id)
        let shape = RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous)
        return VStack(spacing: 0) {
            Button {
                toggle(group.id)
            } label: {
                header(group, isExpanded: isExpanded)
            }
            .buttonStyle(RowButtonStyle())
            .accessibilityLabel("\(group.title), \(messageCountLabel(group.messages.count))")
            .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")
            .accessibilityHint(isExpanded ? "Collapses the group" : "Expands the group")

            if isExpanded {
                ForEach(group.messages) { message in
                    MailMessageRow(message: message, model: model)
                }
            }
        }
        .background(MailstackColors.surfaceContainer(scheme), in: shape)
        .clipShape(shape)
        .contentShape(shape)
    }

    private func header(_ group: MailGroup, isExpanded: Bool) -> some View {
        HStack(spacing: 16) {
            Image(systemName: group.symbol)
                .font(.body.weight(.semibold))
                .foregroundStyle(group.accent)
                .frame(width: MailstackColors.avatar, height: MailstackColors.avatar)
                .background(group.accent.opacity(0.14), in: Circle())
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(group.title)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                    .lineLimit(1)
                Text(messageCountLabel(group.messages.count))
                    .font(.caption)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            Image(systemName: "chevron.down")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                .rotationEffect(.degrees(isExpanded ? 180 : 0))
                .accessibilityHidden(true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: 56, alignment: .center)
        .contentShape(Rectangle())
    }

    private func toggle(_ id: String) {
        let change = {
            if expanded.contains(id) {
                expanded.remove(id)
            } else {
                expanded.insert(id)
            }
        }
        if reduceMotion || model.reduceMotion {
            change()
        } else {
            withAnimation(.easeInOut(duration: 0.2)) {
                change()
            }
        }
    }

    /// Each predicate is independent. A message can sit in more than one group.
    /// Empty groups are omitted. Nothing is added that is not on a demo message.
    private func groups(in messages: [DemoMessage]) -> [MailGroup] {
        let specs: [(String, String, String, Color, (DemoMessage) -> Bool)] = [
            (
                "needs-attention",
                "Needs attention",
                "exclamationmark.shield",
                MailstackColors.needsAttention,
                { message in
                    message.unread && (
                        contains(message.subject, "sign-in")
                            || contains(message.subject, "Security")
                            || contains(message.subject, "alert")
                    )
                }
            ),
            (
                "promotions",
                "Promotions",
                "tag",
                MailstackColors.promotions,
                { $0.bucket == .promotional }
            ),
            (
                "updates",
                "Updates",
                "arrow.triangle.2.circlepath",
                MailstackColors.updates,
                { message in
                    contains(message.subject, "bill")
                        || contains(message.subject, "receipt")
                        || contains(message.subject, "renew")
                }
            ),
            (
                "work",
                "Work",
                "briefcase",
                MailstackColors.work,
                { message in
                    contains(message.subject, "pull request")
                        || contains(message.subject, "Task")
                        || contains(message.subject, "roadmap")
                }
            ),
            (
                "everything-else",
                "Everything else",
                "ellipse",
                MailstackColors.everythingElse,
                { $0.bucket == .social }
            ),
        ]
        return specs.compactMap { id, title, symbol, accent, include in
            let members = messages.filter(include)
            guard !members.isEmpty else { return nil }
            return MailGroup(
                id: id,
                title: title,
                symbol: symbol,
                accent: accent,
                messages: members
            )
        }
    }
}

private struct MailGroup: Identifiable {
    let id: String
    let title: String
    let symbol: String
    let accent: Color
    let messages: [DemoMessage]
}

private struct RowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? Color.primary.opacity(0.08) : Color.clear)
    }
}

private func contains(_ subject: String, _ needle: String) -> Bool {
    subject.range(of: needle, options: .caseInsensitive) != nil
}

private func messageCountLabel(_ count: Int) -> String {
    count == 1 ? "1 message" : "\(count) messages"
}

#Preview {
    CategoriesTabView(model: AppModel())
}
