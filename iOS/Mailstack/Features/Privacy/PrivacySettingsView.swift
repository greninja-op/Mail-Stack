import SwiftUI

struct PrivacySettingsView: View {
    @EnvironmentObject private var container: AppContainer
    var body: some View {
        List {
            Label("Remote images never auto-fetched", systemImage: "eye.slash")
            Label("HTML sanitized before render", systemImage: "shield")
            Label("Tokens only in Keychain", systemImage: "key")
            Button("Wipe local data", role: .destructive) {
                Task { await container.persistence.wipeAllLocalData() }
            }
        }
        .navigationTitle("Privacy")
    }
}
