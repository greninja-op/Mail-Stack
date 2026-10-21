import SwiftUI

/// iPad workspace. Regular widths at least 900pt keep a sidebar.
/// At 1100pt a mailbox shows sidebar, list, and detail together.
/// Narrower windows, and any compact size class, stay one column.
struct PadShell: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorScheme) private var scheme

    @State private var destination: PadDestination = .allInbox
    @State private var selectedMessageId: String?
    @State private var drawerOpen = false

    private static let sidebarWidth: CGFloat = 900
    private static let threeColumnWidth: CGFloat = 1100
    private static let slideDistance: CGFloat = 16

    var body: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            // Container safe area only. Zero means the column sits on the
            // screen edge — nothing is reserved for a phone tab bar.
            let liveBottomInset = max(0, proxy.safeAreaInsets.bottom)
            let mode = makeLayout(width: width, sizeClass: horizontalSizeClass)
            ZStack(alignment: .top) {
                PadShellPalette.background(scheme)
                workspace(mode: mode, width: width)
                    .ignoresSafeArea(.container, edges: .bottom)
                    .padding(.bottom, liveBottomInset)
            }
            .frame(width: proxy.size.width, height: proxy.size.height, alignment: .top)
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: mode)
            .onChange(of: mode) { _, new in
                if new != .single {
                    drawerOpen = false
                }
            }
        }
        .ignoresSafeArea(.container, edges: .bottom)
        .background(PadShellPalette.background(scheme).ignoresSafeArea())
        .tint(PadShellPalette.brand(scheme))
    }

    private var destinationBinding: Binding<PadDestination> {
        Binding {
            destination
        } set: { newValue in
            if destination != newValue {
                destination = newValue
                selectedMessageId = nil
            }
            drawerOpen = false
        }
    }

    /// Compact size class is always one column. Otherwise width decides:
    /// under 900 one column, under 1100 sidebar plus content, and a
    /// mailbox at 1100 or wider is sidebar, list, and detail.
    /// Home, Categories, Companies, and Actions keep sidebar plus content
    /// so a blank detail column is not left open.
    private func makeLayout(width: CGFloat, sizeClass: UserInterfaceSizeClass?) -> PadLayout {
        if sizeClass == .compact {
            return .single
        }
        guard width > 0 else { return .two }
        if width < Self.sidebarWidth { return .single }
        if width >= Self.threeColumnWidth, destination.isMailbox { return .three }
        return .two
    }

    @ViewBuilder
    private func workspace(mode: PadLayout, width: CGFloat) -> some View {
        switch mode {
        case .single:
            compactColumn(width: width)
        case .two:
            NavigationSplitView {
                sidebarColumn
            } detail: {
                slidingContent(mode: .two)
                    .toolbar(.hidden, for: .navigationBar)
            }
            .navigationSplitViewStyle(.balanced)
        case .three:
            NavigationSplitView {
                sidebarColumn
            } content: {
                slidingContent(mode: .three)
                    .navigationSplitViewColumnWidth(min: 300, ideal: 380, max: 520)
                    .toolbar(.hidden, for: .navigationBar)
            } detail: {
                PadThreadColumn(messageId: selectedMessageId)
                    .toolbar(.hidden, for: .navigationBar)
            }
            .navigationSplitViewStyle(.balanced)
            .navigationSplitViewColumnVisibility(.all)
        }
    }

    private var sidebarColumn: some View {
        PadSidebar(destination: destinationBinding)
            .navigationSplitViewColumnWidth(min: 220, ideal: 268, max: 320)
            .toolbar(.hidden, for: .navigationBar)
            .toolbarBackground(PadShellPalette.background(scheme), for: .windowToolbar)
            .toolbarBackground(.visible, for: .windowToolbar)
            .containerBackground(PadShellPalette.background(scheme), for: .navigation)
    }

    private func drawerWidth(for width: CGFloat) -> CGFloat {
        let proposed = min(320, max(240, width - 56))
        return min(proposed, max(0, width - 24))
    }

    private func compactColumn(width: CGFloat) -> some View {
        ZStack(alignment: .leading) {
            VStack(spacing: 0) {
                compactBar
                slidingContent(mode: .single)
            }
            .background(PadShellPalette.background(scheme))
            .accessibilityHidden(drawerOpen)

            if drawerOpen {
                Color.black.opacity(0.32)
                    .ignoresSafeArea(.container)
                    .contentShape(Rectangle())
                    .onTapGesture { drawerOpen = false }
                    .accessibilityLabel("Close destinations")
                    .accessibilityAddTraits(.isButton)
                    .accessibilityAction { drawerOpen = false }
                    .transition(reduceMotion ? .identity : .opacity)

                PadSidebar(destination: destinationBinding)
                    .frame(width: drawerWidth(for: width))
                    .frame(maxHeight: .infinity, alignment: .top)
                    .background(PadShellPalette.background(scheme))
                    .accessibilityAddTraits(.isModal)
                    .transition(reduceMotion ? .identity : .move(edge: .leading))
            }
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: drawerOpen)
    }

    private var compactBar: some View {
        VStack(spacing: 0) {
            HStack(spacing: 4) {
                Button {
                    drawerOpen = true
                } label: {
                    Image(systemName: "line.3.horizontal")
                        .font(.body.weight(.medium))
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Open destinations")
                .foregroundStyle(PadColors.onSurface(scheme))

                if destination.isMailbox, selectedMessageId != nil {
                    Button {
                        selectedMessageId = nil
                    } label: {
                        Image(systemName: "chevron.backward")
                            .font(.body.weight(.semibold))
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Back to \(destination.title)")
                    .foregroundStyle(PadShellPalette.brand(scheme))
                }

                Text(destination.title)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(PadColors.onSurface(scheme))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 8)

            Rectangle()
                .fill(PadColors.onSurfaceVariant(scheme).opacity(0.2))
                .frame(height: 1)
        }
        .background(PadShellPalette.background(scheme))
    }

    private func slidingContent(mode: PadLayout) -> some View {
        ZStack {
            ForEach([destination]) { item in
                destinationPage(item, mode: mode)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .transition(contentTransition(for: item))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .background(PadShellPalette.background(scheme))
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: destination)
    }

    @ViewBuilder
    private func destinationPage(_ item: PadDestination, mode: PadLayout) -> some View {
        switch item {
        case .allInbox, .primary, .promotional, .social, .starred, .spam:
            mailboxPage(mode)
        case .home:
            PadHomeColumn()
        case .categories:
            PadCategoriesColumn()
        case .companies:
            PadCompaniesColumn()
        case .actions:
            PadActionsColumn()
        }
    }

    @ViewBuilder
    private func mailboxPage(_ mode: PadLayout) -> some View {
        if mode == .three || selectedMessageId == nil {
            PadMailColumn(filter: listFilter) { selectedMessageId = $0 }
        } else if mode == .single {
            PadThreadColumn(messageId: selectedMessageId)
        } else {
            VStack(spacing: 0) {
                backToList
                PadThreadColumn(messageId: selectedMessageId)
            }
            .background(PadShellPalette.background(scheme))
        }
    }

    private var backToList: some View {
        HStack(spacing: 4) {
            Button {
                selectedMessageId = nil
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "chevron.backward")
                        .font(.body.weight(.semibold))
                    Text(destination.title)
                        .font(.body.weight(.semibold))
                }
                .foregroundStyle(PadShellPalette.brand(scheme))
                .frame(minHeight: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Back to \(destination.title)")
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .background(PadShellPalette.background(scheme))
    }

    /// Sidebar folder for the current mailbox destination.
    private var listFilter: PadListFilter {
        switch destination {
        case .primary: .primary
        case .promotional: .promotional
        case .social: .social
        case .starred: .starred
        case .spam: .spam
        case .allInbox, .home, .categories, .companies, .actions: .allInbox
        }
    }

    /// Upper destinations enter from above, lower ones from below.
    /// The offset stays small. Reduce Motion swaps with no slide.
    private func contentTransition(for item: PadDestination) -> AnyTransition {
        guard !reduceMotion else { return .identity }
        let index = PadDestination.allCases.firstIndex(of: item) ?? 0
        let edge = padContentEdge(forDestinationIndex: index, count: PadDestination.allCases.count)
        let y = edge == .bottom ? Self.slideDistance : -Self.slideDistance
        return .asymmetric(
            insertion: .offset(y: y).combined(with: .opacity),
            removal: .opacity
        )
    }
}

private enum PadLayout: Equatable {
    case single
    case two
    case three
}

#Preview {
    PadShell()
}
