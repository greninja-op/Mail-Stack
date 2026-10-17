import SwiftUI

/// Full-screen settings sliding from the right (350ms).
struct SettingsPanel: View {
    @EnvironmentObject private var container: AppContainer
    @Binding var isPresented: Bool

    var body: some View {
        NavigationStack {
            List {
                Section("Account") {
                    NavigationLink("Connected accounts") { Text("Manage accounts in the account switcher.") }
                    NavigationLink("Sync") { ImportFlowView() }
                }
                Section("Privacy") {
                    NavigationLink("Privacy & data") { PrivacySettingsView() }
                    NavigationLink("Security") { Text("Tokens stay in Keychain. Mail bodies are sanitized.") }
                }
                Section("Intelligence") {
                    NavigationLink("Rules & corrections") { RulesView() }
                    NavigationLink("Automation") { AutomationView() }
                    NavigationLink("Optional AI") { AISettingsView() }
                }
                Section("App") {
                    NavigationLink("Accessibility") { AccessibilityNotesView() }
                    NavigationLink("Offline & notifications") { OfflineSettingsView() }
                    NavigationLink("Integrations") { IntegrationsView() }
                }
                // Non-important entries as Gmail-style footer text — not buttons.
                Section {
                    VStack(spacing: 8) {
                        Text("Privacy Policy • Terms of Service")
                            .font(.system(size: MSTypography.labelSmall))
                            .foregroundStyle(.secondary)
                        Text("Mailstack organizes Gmail on your device. Demo mode never contacts Google.")
                            .font(.system(size: MSTypography.labelSmall))
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        withAnimation(.easeInOut(duration: MSMotion.settingsPanel)) {
                            isPresented = false
                        }
                    }
                }
            }
        }
        .background(Color(.systemBackground))
    }
}
