import SwiftUI

struct PadSidebar: View {
    @EnvironmentObject private var container: PadAppContainer
    var body: some View {
        List {
            Section("Mailstack") {
                ForEach(PadDestination.allCases) { dest in
                    Button {
                        container.store.destination = dest
                        container.store.selected = nil
                    } label: {
                        HStack {
                            Text(dest.title)
                            Spacer()
                            if container.store.destination == dest {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            }
            Section("Workspace") {
                NavigationLink("Categories") { PadCategories() }
                NavigationLink("Search") { PadSearch() }
                NavigationLink("Actions") { PadActions() }
            }
        }
        .navigationTitle("Mailstack")
        .listStyle(.sidebar)
    }
}
