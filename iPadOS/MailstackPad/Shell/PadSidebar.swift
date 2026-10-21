import SwiftUI

/// Light-mode brand and canvas, written as the shell spec.
/// Dark mode uses the shared iPad tokens so columns stay one surface.
enum PadShellPalette {
    static let brand = Color(red: 0x52/255, green: 0x38/255, blue: 0xE8/255)
    static let background = Color(red: 0xF6/255, green: 0xF3/255, blue: 1)

    static func background(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? PadColors.background(scheme) : background
    }

    static func brand(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? PadColors.brand(scheme) : brand
    }
}

/// Sidebar order is the slide order. The top half enters from the top
/// and the bottom half from the bottom (`padContentEdge`).
enum PadDestination: String, CaseIterable, Hashable, Identifiable {
    case allInbox
    case primary
    case promotional
    case social
    case starred
    case spam
    case home
    case categories
    case companies
    case actions

    var id: String { rawValue }

    var title: String {
        switch self {
        case .allInbox: "All Inbox"
        case .primary: "Primary"
        case .promotional: "Promotional"
        case .social: "Social"
        case .starred: "Starred"
        case .spam: "Spam"
        case .home: "Home"
        case .categories: "Categories"
        case .companies: "Companies"
        case .actions: "Actions"
        }
    }

    var isMailbox: Bool {
        switch self {
        case .home, .categories, .companies, .actions: false
        default: true
        }
    }

    static var mailbox: [PadDestination] {
        allCases.filter(\.isMailbox)
    }

    static var workspace: [PadDestination] {
        allCases.filter { !$0.isMailbox }
    }

    var symbol: String {
        switch self {
        case .allInbox: "tray.full"
        case .primary: "envelope"
        case .promotional: "megaphone"
        case .social: "person.2"
        case .starred: "star"
        case .spam: "exclamationmark.triangle"
        case .home: "house"
        case .categories: "list.bullet"
        case .companies: "person.crop.rectangle"
        case .actions: "checkmark.circle"
        }
    }

    var selectedSymbol: String {
        switch self {
        case .allInbox: "tray.full"
        case .primary: "envelope.fill"
        case .promotional: "megaphone.fill"
        case .social: "person.2.fill"
        case .starred: "star.fill"
        case .spam: "exclamationmark.triangle.fill"
        case .home: "house.fill"
        case .categories: "list.bullet"
        case .companies: "person.crop.rectangle.fill"
        case .actions: "checkmark.circle.fill"
        }
    }
}

/// Destinations shared by the persistent sidebar and the narrow drawer.
/// Solid fills only — no material, no vibrancy.
struct PadSidebar: View {
    @Binding var destination: PadDestination

    @Environment(\.colorScheme) private var scheme
    @State private var hovered: PadDestination?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Mailstack")
                .font(.title2.weight(.semibold))
                .foregroundStyle(PadShellPalette.brand(scheme))
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 8)
                .accessibilityAddTraits(.isHeader)

            List {
                Section {
                    ForEach(PadDestination.mailbox) { item in
                        row(item)
                    }
                }
                Section {
                    ForEach(PadDestination.workspace) { item in
                        row(item)
                    }
                } header: {
                    Text("Workspace")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(PadColors.onSurfaceVariant(scheme))
                        .textCase(nil)
                        .accessibilityAddTraits(.isHeader)
                }
            }
            .listStyle(.plain)
            .listSectionSpacing(12)
            .scrollContentBackground(.hidden)
            .environment(\.defaultMinListRowHeight, 48)
            .accessibilityLabel("Destinations")
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(PadShellPalette.background(scheme))
    }

    private func row(_ item: PadDestination) -> some View {
        let selected = destination == item
        let hovering = hovered == item
        return Button {
            destination = item
        } label: {
            HStack(spacing: 12) {
                Image(systemName: selected ? item.selectedSymbol : item.symbol)
                    .font(.body)
                    .frame(width: 24, height: 24)
                    .accessibilityHidden(true)
                Text(item.title)
                    .font(.body.weight(selected ? .semibold : .regular))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .foregroundStyle(
                selected
                    ? PadShellPalette.brand(scheme)
                    : PadColors.onSurface(scheme)
            )
            .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
            .padding(.horizontal, 12)
            .background(rowFill(selected: selected, hovering: hovering), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(item.title)
        .accessibilityAddTraits(selected ? .isSelected : [])
        .onHover { inside in
            if inside {
                hovered = item
            } else if hovered == item {
                hovered = nil
            }
        }
        .listRowInsets(EdgeInsets(top: 2, leading: 12, bottom: 2, trailing: 12))
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
    }

    private func rowFill(selected: Bool, hovering: Bool) -> Color {
        if selected { return PadShellPalette.brand(scheme).opacity(0.14) }
        if hovering { return PadShellPalette.brand(scheme).opacity(0.06) }
        return Color.clear
    }
}
