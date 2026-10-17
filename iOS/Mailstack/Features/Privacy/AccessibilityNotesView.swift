import SwiftUI

struct AccessibilityNotesView: View {
    var body: some View {
        List {
            Text("VoiceOver labels on drawer, tabs, stars, and account switcher.")
            Text("Dynamic Type uses system fonts; 44pt minimum hit targets.")
            Text("Reduce Motion: prefer fades over large slides when enabled (Phase 15).")
        }
        .navigationTitle("Accessibility")
    }
}
