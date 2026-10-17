import SwiftUI

struct PadDetail: View {
    @EnvironmentObject private var container: PadAppContainer
    let thread: PadThread
    @State private var expanded = true
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(thread.subject).font(.largeTitle.bold())
                Text("\(thread.senderName) <\(thread.senderAddress)>").foregroundStyle(.secondary)
                if expanded {
                    Text(thread.snippet).font(.body)
                }
                Text("Remote images blocked: \(!container.privacy.fetchesRemoteImages)")
                    .font(.caption).foregroundStyle(.secondary)
            }
            .padding()
        }
        .toolbar {
            ToolbarItem {
                Button {
                    if let i = container.store.threads.firstIndex(where: { $0.threadId == thread.threadId }) {
                        container.store.threads[i].starred.toggle()
                    }
                } label: { Image(systemName: thread.starred ? "star.fill" : "star") }
            }
        }
    }
}
