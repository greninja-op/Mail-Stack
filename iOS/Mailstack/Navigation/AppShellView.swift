import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case mail, categories, actions, search, more
    var id: String { rawValue }
    var title: String {
        switch self {
        case .mail: return "Mail"
        case .categories: return "Categories"
        case .actions: return "Actions"
        case .search: return "Search"
        case .more: return "More"
        }
    }
    var systemImage: String {
        switch self {
        case .mail: return "envelope"
        case .categories: return "square.grid.2x2"
        case .actions: return "bolt"
        case .search: return "magnifyingglass"
        case .more: return "ellipsis.circle"
        }
    }
}

struct AppShellView: View {
    @EnvironmentObject private var container: AppContainer
    @Environment(\.colorScheme) private var scheme
    @State private var tab: AppTab = .mail
    @State private var destination: MailDestination = .allInbox
    @State private var showDrawer = false
    @State private var showAccountSwitcher = false
    @State private var showSettings = false
    @State private var barsHidden = false
    @State private var selectedThread: ThreadSummary?

    var body: some View {
        let theme = MSTheme(scheme: scheme)
        ZStack {
            theme.background.ignoresSafeArea()

            tabContent
                .offset(x: showSettings ? -UIScreen.main.bounds.width * MSMotion.settingsUnderlyingShift : 0)
                .animation(.easeInOut(duration: MSMotion.settingsPanel), value: showSettings)

            if showDrawer {
                DrawerOverlay(
                    destination: $destination,
                    isPresented: $showDrawer
                )
                .transition(.move(edge: .leading))
                .zIndex(2)
            }

            if showSettings {
                SettingsPanel(isPresented: $showSettings)
                    .transition(.move(edge: .trailing))
                    .zIndex(3)
            }
        }
        .environment(\.msTheme, theme)
        .safeAreaInset(edge: .top) {
            if !barsHidden {
                TopBar(
                    title: destination.title,
                    onMenu: { withAnimation { showDrawer = true } },
                    onProfile: { showAccountSwitcher = true }
                )
                .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .safeAreaInset(edge: .bottom) {
            if !barsHidden {
                TabBar(selection: $tab)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: MSMotion.scrollHideBars), value: barsHidden)
        .sheet(isPresented: $showAccountSwitcher) {
            AccountSwitcherSheet(isPresented: $showAccountSwitcher, onOpenSettings: {
                showAccountSwitcher = false
                withAnimation(.easeInOut(duration: MSMotion.settingsPanel)) { showSettings = true }
            })
            .environmentObject(container)
        }
        .sheet(item: $selectedThread) { thread in
            MessageDetailView(thread: thread)
                .environmentObject(container)
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch tab {
        case .mail:
            MailListView(
                destination: destination,
                barsHidden: $barsHidden,
                onSelect: { selectedThread = $0 }
            )
        case .categories:
            CategoriesView()
        case .actions:
            ActionsView()
        case .search:
            SearchView()
        case .more:
            MoreHubView(onOpenSettings: {
                withAnimation(.easeInOut(duration: MSMotion.settingsPanel)) { showSettings = true }
            })
        }
    }
}

struct TopBar: View {
    let title: String
    var onMenu: () -> Void
    var onProfile: () -> Void

    var body: some View {
        HStack {
            Button(action: onMenu) {
                Image(systemName: "line.3.horizontal")
                    .frame(width: MSShape.minHitTarget, height: MSShape.minHitTarget)
            }
            .accessibilityLabel("Open navigation drawer")
            Text(title)
                .font(.system(size: MSTypography.titleMedium, weight: .semibold))
            Spacer()
            Button(action: onProfile) {
                Circle()
                    .fill(Color.accentColor.opacity(0.25))
                    .frame(width: MSShape.avatarSize, height: MSShape.avatarSize)
                    .overlay(Text("D").font(.system(size: 14, weight: .bold)))
            }
            .accessibilityLabel("Account switcher")
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(.ultraThinMaterial)
    }
}

struct TabBar: View {
    @Binding var selection: AppTab
    var body: some View {
        HStack {
            ForEach(AppTab.allCases) { tab in
                Button {
                    withAnimation(.easeInOut(duration: 0.25)) { selection = tab }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.systemImage)
                        Text(tab.title).font(.system(size: 10))
                    }
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(selection == tab ? Color.accentColor : .secondary)
                }
                .accessibilityLabel(tab.title)
                .accessibilityAddTraits(selection == tab ? .isSelected : [])
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(.ultraThinMaterial)
    }
}
