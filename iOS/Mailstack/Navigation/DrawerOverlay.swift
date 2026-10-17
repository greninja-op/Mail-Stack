import SwiftUI

/// Drawer covers the top section (Gmail parity) — not below a hoisted bar.
struct DrawerOverlay: View {
    @Binding var destination: MailDestination
    @Binding var isPresented: Bool

    var body: some View {
        ZStack(alignment: .leading) {
            Color.black.opacity(0.35)
                .ignoresSafeArea()
                .onTapGesture { withAnimation { isPresented = false } }

            VStack(alignment: .leading, spacing: 0) {
                Text("Mailstack")
                    .font(.system(size: MSTypography.titleLarge, weight: .bold))
                    .padding(20)
                ForEach(MailDestination.allCases) { dest in
                    Button {
                        destination = dest
                        withAnimation { isPresented = false }
                    } label: {
                        HStack(spacing: 16) {
                            Image(systemName: dest.systemImage)
                            Text(dest.title)
                            Spacer()
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 14)
                        .background(destination == dest ? Color.accentColor.opacity(0.12) : Color.clear)
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
            }
            .frame(width: min(320, UIScreen.main.bounds.width * 0.82))
            .frame(maxHeight: .infinity)
            .background(Color(.systemBackground))
            .ignoresSafeArea(edges: .top)
        }
    }
}
