import SwiftUI

struct PadAuthGate: View {
    @EnvironmentObject private var container: PadAppContainer
    var body: some View {
        VStack(spacing: 20) {
            Text("Mailstack").font(.largeTitle.bold())
            Text("Organize Gmail on iPad.").foregroundStyle(.secondary)
            Button("Continue with demo") { container.store.signedIn = true }
                .buttonStyle(.borderedProminent)
            Text("Live Google OAuth requires Mac + client IDs.")
                .font(.caption).foregroundStyle(.secondary)
        }
        .padding()
    }
}
