import SwiftUI

/// Structured query only — UI never builds SQL (Phase 7).
struct SearchView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var text = ""
    @State private var unreadOnly = false
    @State private var starredOnly = false
    @State private var results: [ThreadSummary] = []
    @State private var task: Task<Void, Never>?

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass")
                TextField("Search mail", text: $text)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .onChange(of: text) { _, _ in scheduleSearch() }
            }
            .padding()
            Toggle("Unread", isOn: $unreadOnly).padding(.horizontal).onChange(of: unreadOnly) { _, _ in scheduleSearch() }
            Toggle("Starred", isOn: $starredOnly).padding(.horizontal).onChange(of: starredOnly) { _, _ in scheduleSearch() }
            List(results) { thread in
                MailRowView(thread: thread)
            }
        }
        .task { scheduleSearch() }
    }

    private func scheduleSearch() {
        task?.cancel()
        task = Task {
            try? await Task.sleep(nanoseconds: 300_000_000) // 300ms debounce
            guard !Task.isCancelled else { return }
            let accounts = await container.accounts.listAccounts()
            guard let id = accounts.first(where: \.isActive)?.accountId else { return }
            let q = StructuredQuery(
                freeText: text.isEmpty ? nil : text,
                isUnread: unreadOnly ? true : nil,
                isStarred: starredOnly ? true : nil
            )
            results = await container.search.search(accountId: id, query: q)
        }
    }
}
