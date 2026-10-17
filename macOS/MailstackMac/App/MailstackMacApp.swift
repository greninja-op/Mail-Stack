import SwiftUI

@main
struct MailstackMacApp: App {
    @StateObject private var store = MacStore.demo()
    var body: some Scene {
        WindowGroup {
            MacWorkspaceView()
                .environmentObject(store)
                .frame(minWidth: 900, minHeight: 600)
        }
        .commands {
            CommandGroup(replacing: .newItem) {}
            CommandMenu("Mailbox") {
                Button("All Inbox") { store.destination = .allInbox }
                    .keyboardShortcut("1", modifiers: .command)
                Button("Starred") { store.destination = .starred }
                    .keyboardShortcut("3", modifiers: .command)
            }
        }
    }
}
