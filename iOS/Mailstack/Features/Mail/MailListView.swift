import SwiftUI
import Combine

struct MailListView: View {
    @EnvironmentObject private var container: AppContainer
    let destination: MailDestination
    @Binding var barsHidden: Bool
    var onSelect: (ThreadSummary) -> Void

    @State private var threads: [ThreadSummary] = []
    @State private var accountId: AccountId?
    @State private var lastOffset: CGFloat = 0

    var body: some View {
        Group {
            if threads.isEmpty {
                EmptyStateView(title: "No conversations", subtitle: "Mail for \(destination.title) will show up here.")
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(threads) { thread in
                            Button { onSelect(thread) } label: {
                                MailRowView(thread: thread) {
                                    Task { await container.mailbox.setStarred(threadId: thread.threadId, starred: !thread.starred) }
                                }
                            }
                            .buttonStyle(.plain)
                            Divider().padding(.leading, 68)
                        }
                    }
                    .background(
                        GeometryReader { geo in
                            Color.clear.preference(key: ScrollOffsetKey.self, value: geo.frame(in: .named("mailScroll")).minY)
                        }
                    )
                }
                .coordinateSpace(name: "mailScroll")
                .onPreferenceChange(ScrollOffsetKey.self) { y in
                    let delta = y - lastOffset
                    if delta < -8 { barsHidden = true }
                    else if delta > 8 { barsHidden = false }
                    lastOffset = y
                }
                .refreshable {
                    if let id = accountId {
                        await container.sync.startSync(accountId: id)
                    }
                }
            }
        }
        .onAppear { barsHidden = false }
        .task { await bind() }
    }

    private func bind() async {
        let accounts = await container.accounts.listAccounts()
        accountId = accounts.first(where: \.isActive)?.accountId ?? accounts.first?.accountId
        guard let accountId else { return }
        let pub = container.mailbox.observeThreads(accountId: accountId, destination: destination)
        for await value in pub.values {
            threads = value
        }
    }
}

private struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}
