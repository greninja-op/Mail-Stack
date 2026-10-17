import SwiftUI

struct PadSearch: View {
    @EnvironmentObject private var container: PadAppContainer
    @State private var q = ""
    var body: some View {
        List {
            TextField("Search", text: $q).textInputAutocapitalization(.never)
            ForEach(results) { t in
                VStack(alignment: .leading) {
                    Text(t.senderName).fontWeight(.semibold)
                    Text(t.subject)
                }
            }
        }
        .navigationTitle("Search")
    }
    private var results: [PadThread] {
        let needle = q.lowercased()
        guard !needle.isEmpty else { return container.store.threads }
        return container.store.threads.filter {
            ($0.subject + $0.snippet + $0.senderName).lowercased().contains(needle)
        }
    }
}
