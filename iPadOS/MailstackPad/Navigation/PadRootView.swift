import SwiftUI

struct PadRootView: View {
    @EnvironmentObject private var container: PadAppContainer
    @Environment(\.horizontalSizeClass) private var hSize
    @State private var columnVisibility: NavigationSplitViewVisibility = .all
    @State private var showSettings = false

    var body: some View {
        Group {
            if container.store.signedIn {
                NavigationSplitView(columnVisibility: $columnVisibility) {
                    PadSidebar()
                } content: {
                    PadMailList()
                } detail: {
                    if let t = container.store.selected {
                        PadDetail(thread: t)
                    } else {
                        ContentUnavailableView("Select a conversation", systemImage: "envelope")
                    }
                }
                .navigationSplitViewStyle(.balanced)
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        Button { showSettings = true } label: {
                            Image(systemName: "gearshape")
                        }
                        .accessibilityLabel("Settings")
                    }
                }
                .sheet(isPresented: $showSettings) {
                    NavigationStack { PadSettings() }
                }
            } else {
                PadAuthGate()
            }
        }
        .onAppear {
            if hSize == .compact {
                columnVisibility = .doubleColumn
            }
        }
    }
}
