import SwiftUI

struct AISettingsView: View {
    @EnvironmentObject private var container: AppContainer
    var body: some View {
        List {
            Toggle("Optional AI fallback", isOn: .constant(container.ai.isEnabled))
                .disabled(true)
            Text("Off by default. On-device / opted-in only when configured on Mac.")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("AI")
    }
}
