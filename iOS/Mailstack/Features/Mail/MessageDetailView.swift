import SwiftUI

struct MessageDetailView: View {
    @EnvironmentObject private var container: AppContainer
    let thread: ThreadSummary
    @State private var expanded = true
    @State private var bodyText: String = ""

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(thread.subject)
                    .font(.system(size: MSTypography.titleLarge, weight: .bold))
                HStack {
                    AvatarView(name: thread.senderName, domain: thread.senderDomain)
                    VStack(alignment: .leading) {
                        Text(thread.senderName).fontWeight(.semibold)
                        Text(thread.senderAddress)
                            .font(.system(size: MSTypography.labelSmall))
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button {
                        withAnimation(.easeInOut(duration: MSMotion.messageReveal)) {
                            expanded.toggle()
                        }
                    } label: {
                        Image(systemName: "chevron.down")
                            .rotationEffect(.degrees(expanded ? 0 : -90))
                            .frame(width: MSShape.minHitTarget, height: MSShape.minHitTarget)
                    }
                    .accessibilityLabel(expanded ? "Collapse message" : "Expand message")
                }
                if expanded {
                    Text(bodyText)
                        .font(.system(size: MSTypography.bodyLarge))
                        .transition(.opacity.combined(with: .move(edge: .top)))
                }
                if container.ai.isEnabled {
                    // Optional AI — off by default (Phase 13)
                }
            }
            .padding()
        }
        .navigationTitle("Mail")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            let raw = thread.snippet
            bodyText = container.privacy.decodeEntities(raw)
            // Remote images never fetched (privacy).
            _ = container.privacy.shouldFetchRemoteImages()
        }
    }
}
