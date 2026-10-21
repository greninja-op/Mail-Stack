import SwiftUI

/// Demo messages whose subjects mention a task or a pull request.
/// Opening a card shows the message. Nothing is marked complete and nothing is sent.
struct ActionsTabView: View {
    @ObservedObject private var model: AppModel
    @ObservedObject private var mailbox: DemoMailbox
    @Environment(\.colorScheme) private var scheme

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
        _mailbox = ObservedObject(wrappedValue: model.mailbox)
    }

    var body: some View {
        let messages = actionMessages
        Group {
            if messages.isEmpty {
                Text("No task or pull request messages in the demo mailbox.")
                    .font(.body)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .multilineTextAlignment(.center)
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                BarHidingScroll(model: model) {
                    LazyVStack(spacing: 8) {
                        ForEach(messages) { message in
                            actionCard(message)
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

    private func actionCard(_ message: DemoMessage) -> some View {
        let shape = RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous)
        return Button {
            model.openThread(message.id)
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                Text(message.subject)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text("From the local demo mailbox. Nothing is sent.")
                    .font(.subheadline)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(16)
            .background(MailstackColors.surfaceContainer(scheme), in: shape)
            .clipShape(shape)
            .contentShape(shape)
        }
        .buttonStyle(CardButtonStyle())
        .accessibilityLabel("\(message.subject). From the local demo mailbox. Nothing is sent.")
        .accessibilityHint("Opens the message. Nothing is sent.")
    }

    private var actionMessages: [DemoMessage] {
        mailbox.messages.filter { message in
            contains(message.subject, "Task") || contains(message.subject, "pull request")
        }
    }
}

private struct CardButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .overlay {
                if configuration.isPressed {
                    RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous)
                        .fill(Color.primary.opacity(0.08))
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous))
    }
}

private func contains(_ subject: String, _ needle: String) -> Bool {
    subject.range(of: needle, options: .caseInsensitive) != nil
}

#Preview {
    ActionsTabView(model: AppModel())
}
