import SwiftUI

/// Single-cavity category card — 28pt radius, clipped highlight.
struct CategoryGroupCard: View {
    let group: CategoryGroup
    let threads: [ThreadSummary]
    @State private var expanded = true

    var body: some View {
        VStack(spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) { expanded.toggle() }
            } label: {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color(hex: group.accent).opacity(0.14))
                        Image(systemName: group.systemImage)
                            .foregroundStyle(Color(hex: group.accent))
                    }
                    .frame(width: 40, height: 40)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(group.title)
                            .font(.system(size: MSTypography.titleSmall, weight: .semibold))
                        Text("\(threads.count) conversations")
                            .font(.system(size: MSTypography.labelSmall))
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: "chevron.down")
                        .rotationEffect(.degrees(expanded ? 0 : -90))
                }
                .padding(16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if expanded {
                ForEach(threads) { t in
                    MailRowView(thread: t)
                }
            }
        }
        .background(Color.secondary.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: MSShape.cardRadius, style: .continuous))
    }
}
