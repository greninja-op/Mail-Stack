import SwiftUI

/// Conversation column for one local demo message.
/// `messageId == nil` shows the empty selection, centered in this column.
struct PadThreadColumn: View {
    var messageId: String?

    @ObservedObject private var mailbox: PadMailbox
    @Environment(\.colorScheme) private var scheme

    init(messageId: String? = nil, mailbox: PadMailbox = .shared) {
        self.messageId = messageId
        _mailbox = ObservedObject(wrappedValue: mailbox)
    }

    var body: some View {
        Group {
            if let messageId, let message = mailbox.message(messageId) {
                conversation(message)
            } else if messageId != nil {
                centeredNote("This demo message is not on this device.")
            } else {
                centeredNote("No conversation selected")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(PadColors.surface(scheme))
        .onAppear(perform: reveal)
        .onChange(of: messageId) { _, _ in
            reveal()
        }
    }

    /// The note is given the column's width, then centered. A free-floating
    /// ideal width would run past the column's trailing edge and clip.
    private func centeredNote(_ title: String) -> some View {
        Text(title)
            .font(.title3.weight(.semibold))
            .foregroundStyle(PadColors.onSurface(scheme))
            .multilineTextAlignment(.center)
            .lineLimit(4)
            .minimumScaleFactor(0.75)
            .frame(minWidth: 0, maxWidth: .infinity, alignment: .center)
            .padding(.horizontal, 24)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
    }

    private func conversation(_ message: PadMessage) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(message.subject)
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(PadColors.onSurface(scheme))
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)

                Text("\(message.sender) · \(message.address) · \(message.timeLabel)")
                    .font(.subheadline)
                    .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)

                Text(message.body)
                    .font(.body)
                    .foregroundStyle(PadColors.onSurface(scheme))
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)

                Text("Remote images are not loaded.")
                    .font(.subheadline)
                    .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)

                Text("This is demo mail on this iPad. It was not loaded from Gmail.")
                    .font(.subheadline)
                    .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollContentBackground(.hidden)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func reveal() {
        if let messageId {
            mailbox.markRead(messageId)
        }
    }
}
