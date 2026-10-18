import SwiftUI

/// Import experience — progress from real sync only; demo completes instantly.
/// Conduit motion is HELD (do not build). Static arc + ring-only progress.
struct ImportFlowView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var progress: SyncProgress = .idle
    @State private var fraction: Double = 0

    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Image(systemName: "envelope.fill").font(.largeTitle)
                Spacer()
                StaticConduitArc()
                Spacer()
                Image(systemName: "tray.full.fill").font(.largeTitle).foregroundStyle(Color.accentColor)
            }
            .padding(.horizontal, 32)

            // ONLY the progress ring moves as the progress bar.
            ZStack {
                Circle().stroke(Color.secondary.opacity(0.2), lineWidth: 8)
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(Color.accentColor, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Text("\(Int(fraction * 100))%")
                    .font(.system(size: MSTypography.titleMedium, weight: .bold))
            }
            .frame(width: 120, height: 120)

            Text(statusText).foregroundStyle(.secondary)

            Button("Start demo sync") {
                Task {
                    let accounts = await container.accounts.listAccounts()
                    guard let id = accounts.first?.accountId else { return }
                    await container.sync.startSync(accountId: id)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(isRunning)

            Spacer()
        }
        .padding()
        .navigationTitle("Import")
        .task {
            let accounts = await container.accounts.listAccounts()
            guard let id = accounts.first?.accountId else { return }
            for await p in container.sync.observeProgress(accountId: id).values {
                progress = p
                fraction = displayFraction(p)
            }
        }
    }

    private var isRunning: Bool {
        if case .running = progress { return true }
        return false
    }

    private var statusText: String {
        switch progress {
        case .idle: return "Ready"
        case .running(let stage, let n, let total):
            return "\(stage.rawValue) — \(n)/\(total ?? 0)"
        case .succeeded: return "Done"
        case .failed(let m): return m
        }
    }

    /// Cap at 99% while running; Done = 100%.
    private func displayFraction(_ p: SyncProgress) -> Double {
        switch p {
        case .idle: return 0
        case .running(_, let n, let total):
            guard let total, total > 0 else { return 0 }
            return min(0.99, Double(n) / Double(total))
        case .succeeded: return 1
        case .failed: return fraction
        }
    }
}

/// Static conduit between Gmail and Mailstack marks — motion HELD pending design go-ahead.
struct StaticConduitArc: View {
    var body: some View {
        Capsule()
            .fill(Color.accentColor.opacity(0.35))
            .frame(height: 4)
            .accessibilityHidden(true)
    }
}
