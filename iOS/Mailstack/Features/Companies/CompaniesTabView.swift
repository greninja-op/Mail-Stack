import SwiftUI

/// Companies named on demo messages. The pin is view state only and is not saved.
struct CompaniesTabView: View {
    @ObservedObject private var model: AppModel
    @ObservedObject private var mailbox: DemoMailbox
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var pinned: Set<String> = []
    @State private var selectedCompany: String?

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
        _mailbox = ObservedObject(wrappedValue: model.mailbox)
    }

    var body: some View {
        Group {
            if let selectedCompany {
                companyMessages(selectedCompany)
            } else {
                companyList
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(MailstackColors.background(scheme))
    }

    private var companyList: some View {
        let rows = companyRows
        return Group {
            if rows.isEmpty {
                Text("No companies are named on these messages.")
                    .font(.body)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .multilineTextAlignment(.center)
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                BarHidingScroll(model: model) {
                    LazyVStack(spacing: 0) {
                        ForEach(Array(rows.enumerated()), id: \.element.id) { index, company in
                            companyRow(company)
                            if index < rows.count - 1 {
                                Rectangle()
                                    .fill(MailstackColors.onSurface(scheme).opacity(0.08))
                                    .frame(height: 1)
                                    .padding(.leading, 16 + MailstackColors.avatar + 12)
                            }
                        }
                    }
                    .padding(.vertical, 8)
                    .animation(motion, value: pinned)
                }
            }
        }
    }

    private func companyMessages(_ name: String) -> some View {
        let messages = mailbox.messages.filter { $0.company == name }
        return VStack(spacing: 0) {
            Button {
                select(nil)
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "chevron.left")
                        .font(.body.weight(.semibold))
                        .frame(width: 44, height: 44)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(name)
                            .font(.headline)
                            .foregroundStyle(MailstackColors.onSurface(scheme))
                            .lineLimit(1)
                        Text(messageCountLabel(messages.count))
                            .font(.caption)
                            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    }
                    Spacer(minLength: 0)
                }
                .padding(.leading, 4)
                .padding(.trailing, 16)
                .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(RowButtonStyle())
            .accessibilityLabel("Back to companies")
            .accessibilityHint("\(name), \(messageCountLabel(messages.count))")

            if messages.isEmpty {
                Text("No messages for this company.")
                    .font(.body)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .multilineTextAlignment(.center)
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                BarHidingScroll(model: model) {
                    LazyVStack(spacing: 0) {
                        ForEach(messages) { message in
                            MailMessageRow(message: message, model: model)
                        }
                    }
                }
            }
        }
    }

    private func companyRow(_ company: CompanySummary) -> some View {
        let isPinned = pinned.contains(company.name)
        return HStack(spacing: 0) {
            Button {
                select(company.name)
            } label: {
                HStack(spacing: 12) {
                    Text(letter(company.name))
                        .font(.body.weight(.semibold))
                        .foregroundStyle(MailstackColors.onSecondaryContainer(scheme))
                        .frame(width: MailstackColors.avatar, height: MailstackColors.avatar)
                        .background(MailstackColors.secondaryContainer(scheme), in: Circle())
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(company.name)
                            .font(.body.weight(.medium))
                            .foregroundStyle(MailstackColors.onSurface(scheme))
                            .lineLimit(1)
                        Text(messageCountLabel(company.count))
                            .font(.caption)
                            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.leading, 16)
                .frame(maxWidth: .infinity, minHeight: 64, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(RowButtonStyle())
            .accessibilityLabel("\(company.name), \(messageCountLabel(company.count))")
            .accessibilityHint("Shows this company's messages")

            Button {
                togglePin(company.name)
            } label: {
                Image(systemName: isPinned ? "pin.fill" : "pin")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(
                        isPinned
                            ? MailstackColors.brand(scheme)
                            : MailstackColors.onSurfaceVariant(scheme)
                    )
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .padding(.trailing, 4)
            .accessibilityLabel(isPinned ? "Unpin \(company.name)" : "Pin \(company.name)")
            .accessibilityHint("Keeps this company at the top of the list on this screen")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Unique non-nil company names on the demo messages. Pinned names stay
    /// at the top for this view only.
    private var companyRows: [CompanySummary] {
        var counts: [String: Int] = [:]
        for message in mailbox.messages {
            guard let name = message.company, !name.isEmpty else { continue }
            counts[name, default: 0] += 1
        }
        return counts
            .map { CompanySummary(name: $0.key, count: $0.value) }
            .sorted { lhs, rhs in
                let lhsPinned = pinned.contains(lhs.name)
                let rhsPinned = pinned.contains(rhs.name)
                if lhsPinned != rhsPinned { return lhsPinned }
                return lhs.name.localizedStandardCompare(rhs.name) == .orderedAscending
            }
    }

    private var motion: Animation? {
        reduceMotion || model.reduceMotion ? nil : .easeInOut(duration: 0.2)
    }

    private func select(_ name: String?) {
        if reduceMotion || model.reduceMotion {
            selectedCompany = name
        } else {
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedCompany = name
            }
        }
    }

    private func togglePin(_ name: String) {
        if reduceMotion || model.reduceMotion {
            flipPin(name)
        } else {
            withAnimation(.easeInOut(duration: 0.2)) {
                flipPin(name)
            }
        }
    }

    private func flipPin(_ name: String) {
        if pinned.contains(name) {
            pinned.remove(name)
        } else {
            pinned.insert(name)
        }
    }

    private func letter(_ name: String) -> String {
        String(name.first ?? "?").uppercased()
    }
}

private struct CompanySummary: Identifiable {
    let name: String
    let count: Int
    var id: String { name }
}

private struct RowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? Color.primary.opacity(0.08) : Color.clear)
    }
}

private func messageCountLabel(_ count: Int) -> String {
    count == 1 ? "1 message" : "\(count) messages"
}

#Preview {
    CompaniesTabView(model: AppModel())
}
