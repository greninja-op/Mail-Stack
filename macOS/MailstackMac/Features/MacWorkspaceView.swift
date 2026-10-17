import SwiftUI

struct MacWorkspaceView: View {
    @EnvironmentObject private var store: MacStore
    var body: some View {
        NavigationSplitView {
            List(MacDestination.allCases, selection: Binding(
                get: { store.destination },
                set: { store.destination = $0 ?? .allInbox; store.selected = nil }
            )) { dest in
                Text(dest.title).tag(dest)
            }
            .navigationSplitViewColumnWidth(min: 160, ideal: 200)
            .navigationTitle("Mailstack")
        } content: {
            List(store.filtered, selection: Binding(
                get: { store.selected?.threadId },
                set: { id in store.selected = store.threads.first { $0.threadId == id } }
            )) { t in
                VStack(alignment: .leading) {
                    Text(t.senderName).fontWeight(t.unread ? .bold : .regular)
                    Text(t.subject).lineLimit(1)
                    Text(t.snippet).foregroundStyle(.secondary).lineLimit(1)
                }
                .tag(t.threadId)
            }
            .navigationTitle(store.destination.title)
        } detail: {
            if let t = store.selected {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(t.subject).font(.largeTitle.bold())
                        Text("\(t.senderName) <\(t.senderAddress)>").foregroundStyle(.secondary)
                        Text(t.snippet)
                        Text("Account \(store.accountLabel) — demo never syncs.")
                            .font(.caption).foregroundStyle(.secondary)
                        Text("Remote images blocked.")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } else {
                ContentUnavailableView("Select a conversation", systemImage: "envelope")
            }
        }
    }
}
