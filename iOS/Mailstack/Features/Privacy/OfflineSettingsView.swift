import SwiftUI

struct OfflineSettingsView: View {
    var body: some View {
        List {
            Text("Background refresh and notification categories are registered in Info.plist.")
            Text("Notification copy must never include mail body content.")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Offline")
    }
}
