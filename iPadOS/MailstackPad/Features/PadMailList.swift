import SwiftUI

struct PadMailList: View {
    @EnvironmentObject private var container: PadAppContainer
    var body: some View {
        List(container.store.filtered, selection: Binding(
            get: { container.store.selected?.threadId },
            set: { id in container.store.selected = container.store.threads.first { $0.threadId == id } }
        )) { thread in
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(thread.senderName).fontWeight(thread.unread ? .bold : .regular)
                    Spacer()
                    if thread.starred { Image(systemName: "star.fill").foregroundStyle(.yellow) }
                }
                Text(thread.subject).fontWeight(thread.unread ? .semibold : .regular).lineLimit(1)
                Text(thread.snippet).foregroundStyle(.secondary).lineLimit(2)
            }
            .tag(thread.threadId)
            .frame(minHeight: 72)
        }
        .navigationTitle(container.store.destination.title)
    }
}
