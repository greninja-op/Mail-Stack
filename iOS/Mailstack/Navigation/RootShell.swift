import SwiftUI

struct RootShell: View {
    @ObservedObject var model: AppModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var topInset: CGFloat = 0
    @State private var bottomInset: CGFloat = 0

    var body: some View {
        let bars = ChromeLayout.clamp(model.barsAmount)
        ZStack {
            MailstackColors.background(scheme).ignoresSafeArea()
            tabBody
                .padding(.top, ChromeLayout.contentTopPadding(systemTopInset: topInset, barsAmount: bars))
                .padding(.bottom, ChromeLayout.contentBottomPadding(systemBottomInset: bottomInset, barsAmount: bars))
            VStack(spacing: 0) {
                MoTopBar(model: model)
                    .padding(.top, topInset)
                    .offset(y: (1 - bars) * -(topInset + ChromeLayout.topBarHeight))
                Spacer(minLength: 0)
                MoNavBar(model: model)
                    .padding(.bottom, bottomInset)
                    .offset(y: (1 - bars) * ChromeLayout.barTotalHeight(systemBottomInset: bottomInset))
            }
            if model.drawerOpen {
                MailDrawer(model: model)
            }
            if model.settingsOpen {
                SettingsPanel(model: model)
                    .transition(.move(edge: .trailing))
            }
            if model.searchOpen {
                SearchScreen(model: model)
            }
            if let id = model.threadId {
                ThreadScreen(model: model, messageId: id)
                    .transition(.move(edge: .trailing))
            }
        }
        .ignoresSafeArea()
        .animation(model.reduceMotion ? .none : .easeInOut(duration: 0.35), value: model.settingsOpen)
        .animation(model.reduceMotion ? .none : .easeInOut(duration: 0.35), value: model.threadId)
        .onAppear(perform: refreshInsets)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshInsets() }
        }
    }

    @ViewBuilder
    private var tabBody: some View {
        switch model.tab {
        case .home: HomeTabView(model: model)
        case .mail: MailTabView(model: model)
        case .categories: CategoriesTabView(model: model)
        case .companies: CompaniesTabView(model: model)
        case .actions: ActionsTabView(model: model)
        }
    }

    private func refreshInsets() {
        let insets = ChromeLayout.currentWindowInsets()
        topInset = insets.top
        bottomInset = insets.bottom
    }
}
