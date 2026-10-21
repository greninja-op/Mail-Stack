import SwiftUI

/// Action cards for the iPad sidebar destination.
/// Each card's subtitle states that the mailbox is local and nothing is sent.
public struct PadActionsColumn: View {
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var expanded: Set<String> = []

    public init() {}

    private var actions: [PadActionItem] { PadActionItem.samples }

    public var body: some View {
        let colors = PadColumnColors(scheme: scheme)
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Task and pull request notes in the local demo.")
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .top, spacing: 12) {
                        ForEach(actions) { action in
                            actionCard(action, colors: colors)
                        }
                    }
                    .frame(minWidth: 640, idealWidth: 640, maxWidth: .infinity, alignment: .topLeading)

                    VStack(spacing: 12) {
                        ForEach(actions) { action in
                            actionCard(action, colors: colors)
                        }
                    }
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollContentBackground(.hidden)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(colors.background)
    }

    private func actionCard(_ action: PadActionItem, colors: PadColumnColors) -> some View {
        let isExpanded = expanded.contains(action.id)
        let shape = RoundedRectangle(cornerRadius: colors.radius, style: .continuous)
        return Button {
            toggle(action.id)
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .center, spacing: 12) {
                    padLetter(action.sender, colors: colors)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(action.sender)
                            .font(.body.weight(.medium))
                            .foregroundStyle(colors.onSurface)
                            .lineLimit(1)
                        Text(action.time)
                            .font(.caption)
                            .foregroundStyle(colors.onSurfaceVariant)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                }

                Text(action.subject)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(colors.onSurface)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text("From the local demo mailbox. Nothing is sent.")
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)

                if isExpanded {
                    Text(action.detail)
                        .font(.body)
                        .foregroundStyle(colors.onSurface)
                        .multilineTextAlignment(.leading)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 4)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(colors.card, in: shape)
        }
        .buttonStyle(PadActionButtonStyle())
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .accessibilityLabel("\(action.subject). From the local demo mailbox. Nothing is sent.")
        .accessibilityValue(isExpanded ? "Expanded" : "Collapsed")
        .accessibilityHint("Shows the demo text. Nothing is sent.")
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

private struct PadActionButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .overlay {
                if configuration.isPressed {
                    Color.primary.opacity(0.08)
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

private struct PadActionItem: Identifiable {
    let id: String
    let sender: String
    let subject: String
    let detail: String
    let time: String

    static let samples: [PadActionItem] = [
        PadActionItem(
            id: "a1",
            sender: "Hiring Desk",
            subject: "Task assigned: Review Q3 roadmap",
            detail: "Please review the roadmap this week. This is a local demo mention. Nothing was added to a task list, and nothing is sent.",
            time: "8:02 AM"
        ),
        PadActionItem(
            id: "a2",
            sender: "GitHub",
            subject: "A pull request was merged: Fix login redirect loop",
            detail: "The pull request is now merged in this demo mailbox. No repository was changed, and nothing is sent.",
            time: "Sun"
        )
    ]
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

#Preview("Actions") {
    PadActionsColumn()
}
