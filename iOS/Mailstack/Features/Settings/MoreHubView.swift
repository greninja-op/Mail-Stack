import SwiftUI

struct MoreHubView: View {
    var onOpenSettings: () -> Void
    var body: some View {
        List {
            Button("Open Settings", action: onOpenSettings)
            NavigationLink("Import / Sync") { ImportFlowView() }
            NavigationLink("Cleanup") { CleanupView() }
            NavigationLink("Companies") { CompaniesView() }
            NavigationLink("About") {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Mailstack").font(.title).bold()
                    Text("iPhone build scaffold. Shared contracts live in shared/ KMP.")
                }.padding()
            }
        }
    }
}
