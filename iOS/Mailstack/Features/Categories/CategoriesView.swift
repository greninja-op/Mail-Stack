import SwiftUI

struct CategoriesView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var groups: [(CategoryGroup, [ThreadSummary])] = []

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(groups, id: \.0.id) { pair in
                    CategoryGroupCard(group: pair.0, threads: pair.1)
                        .padding(.horizontal, 16)
                }
            }
            .padding(.vertical, 16)
        }
        .task {
            let accounts = await container.accounts.listAccounts()
            guard let id = accounts.first(where: \.isActive)?.accountId else { return }
            groups = await container.classification.grouped(accountId: id)
        }
    }
}
