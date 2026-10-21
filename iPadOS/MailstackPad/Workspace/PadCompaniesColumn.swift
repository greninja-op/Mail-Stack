import SwiftUI

/// Companies named on the local demo. Wide columns place each unique name
/// beside that company's messages. Narrow columns stack the name above them.
public struct PadCompaniesColumn: View {
    @Environment(\.colorScheme) private var scheme

    public init() {}

    private var companies: [PadCompany] { PadCompany.samples }

    public var body: some View {
        let colors = PadColumnColors(scheme: scheme)
        ViewThatFits(in: .horizontal) {
            companyStack(sideBySide: true, colors: colors)
                .frame(minWidth: 720, idealWidth: 720, maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            companyStack(sideBySide: false, colors: colors)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(colors.background)
    }

    private func companyStack(sideBySide: Bool, colors: PadColumnColors) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("Companies named on the local demo mailbox.")
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .frame(maxWidth: .infinity, alignment: .leading)

                ForEach(companies) { company in
                    companyCard(company, sideBySide: sideBySide, colors: colors)
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .scrollContentBackground(.hidden)
    }

    private func companyCard(_ company: PadCompany, sideBySide: Bool, colors: PadColumnColors) -> some View {
        let shape = RoundedRectangle(cornerRadius: colors.radius, style: .continuous)
        let identity = companyIdentity(company, colors: colors)
        let messages = messageStack(company, colors: colors)

        return Group {
            if sideBySide {
                HStack(alignment: company.messages.count > 1 ? .top : .center, spacing: 8) {
                    identity
                        .frame(minWidth: 200, idealWidth: 240, maxWidth: 280, alignment: .leading)
                    messages
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    identity
                        .frame(maxWidth: .infinity, alignment: .leading)
                    messages
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.card, in: shape)
        .clipShape(shape)
        .accessibilityElement(children: .contain)
    }

    private func companyIdentity(_ company: PadCompany, colors: PadColumnColors) -> some View {
        HStack(alignment: .center, spacing: 12) {
            padLetter(company.name, colors: colors)
            VStack(alignment: .leading, spacing: 2) {
                Text(company.name)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(colors.onSurface)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
                Text(padMessageCount(company.messages.count))
                    .font(.caption)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(company.name), \(padMessageCount(company.messages.count))")
    }

    private func messageStack(_ company: PadCompany, colors: PadColumnColors) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(company.messages.enumerated()), id: \.element.id) { index, message in
                messageRow(message, colors: colors)
                if index < company.messages.count - 1 {
                    Rectangle()
                        .fill(colors.hairline)
                        .frame(height: 1)
                        .padding(.leading, 16 + 40)
                }
            }
        }
    }

    private func messageRow(_ message: PadCompanyMessage, colors: PadColumnColors) -> some View {
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
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity, minHeight: 88, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(message.unread ? "Unread" : "Read"), \(message.sender), \(message.subject), \(message.time)")
    }
}

private struct PadCompanyMessage: Identifiable {
    let id: String
    let sender: String
    let subject: String
    let snippet: String
    let time: String
    let unread: Bool
}

private struct PadCompany: Identifiable {
    let name: String
    let messages: [PadCompanyMessage]
    var id: String { name }

    /// One entry per company name. Messages for that name sit on the entry.
    static let samples: [PadCompany] = [
        PadCompany(
            name: "Amazon",
            messages: [
                PadCompanyMessage(id: "co-am-1", sender: "Amazon", subject: "Your Amazon order has been delivered", snippet: "Your package was left at the door.", time: "9:14 AM", unread: true),
                PadCompanyMessage(id: "co-am-2", sender: "Amazon", subject: "A delivery photo was added to your order", snippet: "Local demo only. No live order was checked.", time: "9:20 AM", unread: false)
            ]
        ),
        PadCompany(
            name: "Netflix",
            messages: [
                PadCompanyMessage(id: "co-nf-1", sender: "Netflix", subject: "New sign-in to your Netflix account", snippet: "A new device signed in.", time: "Mon", unread: true)
            ]
        ),
        PadCompany(
            name: "GitHub",
            messages: [
                PadCompanyMessage(id: "co-gh-1", sender: "GitHub", subject: "A pull request was merged: Fix login redirect loop", snippet: "The pull request is now merged.", time: "Sun", unread: false),
                PadCompanyMessage(id: "co-gh-2", sender: "GitHub", subject: "A pull request was opened: Adjust column padding", snippet: "Demo notice. Nothing was opened on a live repo.", time: "Sun", unread: true)
            ]
        ),
        PadCompany(
            name: "Spotify",
            messages: [
                PadCompanyMessage(id: "co-sp-1", sender: "Spotify", subject: "Your receipt from Spotify", snippet: "Thanks for your payment.", time: "Tue", unread: false)
            ]
        ),
        PadCompany(
            name: "Adobe",
            messages: [
                PadCompanyMessage(id: "co-ad-1", sender: "Adobe", subject: "Your Creative Cloud subscription renews in 7 days", snippet: "Your plan renews soon.", time: "Tue", unread: true)
            ]
        ),
        PadCompany(
            name: "YouTube",
            messages: [
                PadCompanyMessage(id: "co-yt-1", sender: "Fireship", subject: "Fireship uploaded: Kotlin in 100 Seconds", snippet: "A new video is up.", time: "Sat", unread: false)
            ]
        ),
        PadCompany(
            name: "Amazon Web Services",
            messages: [
                PadCompanyMessage(id: "co-aws-1", sender: "AWS", subject: "Your AWS bill for September 2026", snippet: "Your monthly bill is ready.", time: "Oct 2", unread: false)
            ]
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

#Preview("Companies wide") {
    PadCompaniesColumn()
        .frame(width: 960, height: 800)
}

#Preview("Companies narrow") {
    PadCompaniesColumn()
        .frame(width: 360, height: 800)
}
