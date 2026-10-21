import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case home, mail, categories, companies, actions
    var id: String { rawValue }
    var label: String {
        switch self {
        case .home: "Home"
        case .mail: "Mail"
        case .categories: "Categories"
        case .companies: "Companies"
        case .actions: "Actions"
        }
    }
    var symbol: String {
        switch self {
        case .home: "house.fill"
        case .mail: "envelope.fill"
        case .categories: "list.bullet"
        case .companies: "person.crop.rectangle.fill"
        case .actions: "checkmark.circle.fill"
        }
    }
}

@MainActor
final class AppModel: ObservableObject {
    @Published var tab: AppTab = .mail
    /// 1 = bars shown, 0 = app bars translated off screen. System inset stays.
    @Published var barsAmount: CGFloat = 1
    @Published var settingsOpen = false
    @Published var searchText = ""
    @Published var searchOpen = false
    @Published var threadId: String?
    @Published var folder: MailFolder = .inbox
    @Published var bucket: MailBucket = .primary
    @Published var drawerOpen = false
    @Published var themeOverride: ColorScheme?
    @Published var reduceMotion = false
    let mailbox = DemoMailbox()

    func openThread(_ id: String) {
        mailbox.markRead(id)
        threadId = id
    }

    func setBars(hidden: Bool) {
        withAnimation(reduceMotion ? .none : .easeInOut(duration: 0.25)) {
            barsAmount = hidden ? 0 : 1
        }
    }
}
