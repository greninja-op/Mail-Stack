import SwiftUI

@main
struct MailstackApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootShell(model: model)
                .environmentObject(model)
                .preferredColorScheme(model.themeOverride)
        }
    }
}
