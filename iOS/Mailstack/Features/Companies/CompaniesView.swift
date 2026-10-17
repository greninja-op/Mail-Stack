import SwiftUI

struct CompaniesView: View {
    var body: some View {
        List {
            Text("Company grouping lives inside a category — not as drawer destinations.")
            Text("Pinning is independent from starring.")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Companies")
    }
}
