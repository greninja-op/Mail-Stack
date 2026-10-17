import SwiftUI

struct LoadingStateView: View {
    var message: String = "Loading…"
    var body: some View {
        VStack(spacing: 12) {
            ProgressView()
            Text(message)
                .font(.system(size: MSTypography.bodyMedium))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(message)
    }
}

struct EmptyStateView: View {
    let title: String
    var subtitle: String? = nil
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "tray")
                .font(.system(size: 36))
                .foregroundStyle(.secondary)
            Text(title)
                .font(.system(size: MSTypography.titleMedium, weight: .semibold))
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: MSTypography.bodyMedium))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ErrorStateView: View {
    let message: String
    var retryTitle: String = "Try again"
    var onRetry: (() -> Void)?
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 32))
            Text(message)
                .multilineTextAlignment(.center)
            if let onRetry {
                Button(retryTitle, action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct OfflineBanner: View {
    var body: some View {
        Text("You're offline — showing saved mail.")
            .font(.system(size: MSTypography.labelSmall))
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
            .background(Color.orange.opacity(0.2))
            .accessibilityLabel("Offline. Showing saved mail.")
    }
}
