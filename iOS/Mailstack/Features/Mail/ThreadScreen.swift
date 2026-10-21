import SwiftUI

struct ThreadScreen: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var mailbox: DemoMailbox
    let messageId: String
    @Environment(\.colorScheme) private var scheme
    @State private var replyStatus: String?

    init(model: AppModel, messageId: String) {
        _model = ObservedObject(wrappedValue: model)
        _mailbox = ObservedObject(wrappedValue: model.mailbox)
        self.messageId = messageId
    }

    var body: some View {
        let insets = ChromeLayout.currentWindowInsets()
        VStack(alignment: .leading, spacing: 0) {
            backBar
                .padding(.top, insets.top)
            ScrollView {
                if let message = mailbox.message(messageId) {
                    messageBody(message, bottomInset: insets.bottom)
                } else {
                    missingMessage
                }
            }
            .scrollContentBackground(.hidden)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(MailstackColors.surface(scheme).ignoresSafeArea())
        .onChange(of: messageId) { _, _ in
            replyStatus = nil
        }
    }

    private var backBar: some View {
        HStack {
            Button {
                model.threadId = nil
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "chevron.backward")
                        .font(.system(size: 17, weight: .semibold))
                    Text("Back")
                        .font(.body)
                }
                .foregroundStyle(MailstackColors.brand(scheme))
                .frame(minHeight: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back")
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .background(MailstackColors.surface(scheme))
    }

    private func messageBody(_ message: DemoMessage, bottomInset: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(message.subject)
                .font(.title2.weight(.semibold))
                .foregroundStyle(MailstackColors.onSurface(scheme))
                .fixedSize(horizontal: false, vertical: true)

            Text("\(message.sender) · \(message.accountLabel) · \(message.timeLabel)")
                .font(.subheadline)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                .fixedSize(horizontal: false, vertical: true)

            Text(message.body)
                .font(.body)
                .foregroundStyle(MailstackColors.onSurface(scheme))
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 4) {
                Text("Remote images are not loaded.")
                Text("This is demo mail.")
            }
            .font(.subheadline)
            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
            .fixedSize(horizontal: false, vertical: true)

            Button {
                replyStatus = "Reply stays on this device"
            } label: {
                Text("Reply")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(MailstackColors.onSecondaryContainer(scheme))
                    .padding(.horizontal, 16)
                    .frame(minHeight: 40)
                    .background(MailstackColors.secondaryContainer(scheme), in: Capsule())
            }
            .buttonStyle(.plain)
            .accessibilityHint("Keeps the reply on this device")

            if let replyStatus {
                Text(replyStatus)
                    .font(.subheadline)
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, bottomInset + 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var missingMessage: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Nothing here")
                .font(.body.weight(.semibold))
                .foregroundStyle(MailstackColors.onSurface(scheme))
            Text("This demo message is not on this device.")
                .font(.subheadline)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
