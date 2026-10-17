import SwiftUI

struct RulesView: View {
    var body: some View {
        List {
            Text("User corrections and temporal rules will appear here.")
            Text("Shared classification owns the engine; iOS hosts the editor.")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Rules")
    }
}
