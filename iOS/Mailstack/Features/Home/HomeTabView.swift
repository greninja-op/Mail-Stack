import SwiftUI

struct HomeTabView: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var mailbox: DemoMailbox
    @Environment(\.colorScheme) private var scheme

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
        _mailbox = ObservedObject(wrappedValue: model.mailbox)
    }

    var body: some View {
        let attention = mailbox.messages.filter { $0.unread && $0.bucket == .primary }
        let starred = mailbox.messages.filter(\.starred)
        let recent = mailbox.messages

        BarHidingScroll(model: model) {
            VStack(alignment: .leading, spacing: 20) {
                header
                if !attention.isEmpty {
                    section(title: "Needs attention", rows: attention)
                }
                if !starred.isEmpty {
                    section(title: "Starred", rows: starred)
                }
                if !recent.isEmpty {
                    section(title: "Recent", rows: recent)
                }
                if mailbox.messages.isEmpty {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Nothing here")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(MailstackColors.onSurface(scheme))
                        Text("This mailbox has no demo messages.")
                            .font(.subheadline)
                            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    }
                    .padding(.horizontal, 16)
                }
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(MailstackColors.background(scheme))
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(DemoMailbox.accountName)
                .font(.title2.weight(.semibold))
                .foregroundStyle(MailstackColors.onSurface(scheme))
            Text(DemoMailbox.accountEmail)
                .font(.subheadline)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
            Text("This mailbox is a local demo, not a connected Gmail account.")
                .font(.subheadline)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 4)
        }
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func section(title: String, rows: [DemoMessage]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                Spacer(minLength: 8)
                Text("\(rows.count)")
                    .font(.subheadline)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .accessibilityLabel("\(rows.count) messages")
            }
            .padding(.horizontal, 16)
            .accessibilityElement(children: .combine)

            ForEach(rows) { message in
                MailMessageRow(message: message, model: model)
                    .id("\(title)-\(message.id)")
            }
        }
    }
}
