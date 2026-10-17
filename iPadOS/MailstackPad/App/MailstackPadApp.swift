import SwiftUI

@main
struct MailstackPadApp: App {
    @StateObject private var container = PadAppContainer.bootstrap()
    var body: some Scene {
        WindowGroup {
            PadRootView()
                .environmentObject(container)
                .tint(PadColor.brandVioletLight)
        }
    }
}
