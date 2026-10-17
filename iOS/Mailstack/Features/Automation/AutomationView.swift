import SwiftUI

struct AutomationView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var rules: [String] = []
    var body: some View {
        List {
            if rules.isEmpty {
                Text("No automation rules yet.")
            } else {
                ForEach(rules, id: \.self) { Text($0) }
            }
        }
        .navigationTitle("Automation")
        .task { rules = await container.automation.listRules() }
    }
}
