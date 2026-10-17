import SwiftUI

struct IntegrationsView: View {
    var body: some View {
        List {
            Label("Google Calendar", systemImage: "calendar")
            Label("Google Tasks", systemImage: "checklist")
            Text("Seams only until OAuth modify scopes are granted incrementally.")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Integrations")
    }
}
