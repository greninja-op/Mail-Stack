import SwiftUI

@main
struct MailstackApp: App {
    @StateObject private var container = AppContainer.bootstrap()

    var body: some Scene {
        WindowGroup {
            AuthGateView()
                .environmentObject(container)
                .tint(MSColor.brandVioletLight)
                .preferredColorScheme(nil) // follow system; dark uses #131120 via tokens where applied
        }
    }
}
