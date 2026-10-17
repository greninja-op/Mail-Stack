import SwiftUI

struct PadSettings: View {
    @EnvironmentObject private var container: PadAppContainer
    var body: some View {
        List {
            Section("Account") {
                ForEach(container.store.accounts, id: \.id.value) { a in
                    Text(a.email)
                }
            }
            Section("Privacy") {
                Label("No remote mail images", systemImage: "eye.slash")
                Label("Tokens in Keychain only", systemImage: "key")
            }
            Section {
                VStack(spacing: 8) {
                    Text("Privacy Policy • Terms of Service").font(.caption).foregroundStyle(.secondary)
                    Text("Mailstack on iPad — demo never contacts Google.")
                        .font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            }
        }
        .navigationTitle("Settings")
    }
}
