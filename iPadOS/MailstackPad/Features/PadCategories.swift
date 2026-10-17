import SwiftUI

struct PadCategories: View {
    @EnvironmentObject private var container: PadAppContainer
    var body: some View {
        List {
            ForEach(PadCategory.allCases) { cat in
                let rows = container.store.threads.filter { container.store.classify($0) == cat }
                if !rows.isEmpty {
                    Section(cat.title) {
                        ForEach(rows) { t in
                            Text(t.subject)
                        }
                    }
                }
            }
        }
        .navigationTitle("Categories")
    }
}
