import SwiftUI

struct MoTopBar: View {
    @ObservedObject var model: AppModel
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        HStack(spacing: 12) {
            Button {
                model.drawerOpen = true
            } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 20, weight: .medium))
                    .frame(width: 40, height: 40)
            }
            .accessibilityLabel("Open mailbox menu")

            Button {
                model.searchOpen = true
            } label: {
                HStack {
                    Image(systemName: "magnifyingglass")
                    Text(model.searchText.isEmpty ? "Search mail" : model.searchText)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                }
                .font(.system(size: 16))
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                .padding(.horizontal, 14)
                .frame(height: 40)
                .background(MailstackColors.surfaceContainerHigh(scheme), in: Capsule())
            }
            .buttonStyle(.plain)

            Button {
                model.settingsOpen = true
            } label: {
                Text(String(DemoMailbox.accountName.first ?? "M"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(MailstackColors.onPrimaryContainer)
                    .frame(width: 40, height: 40)
                    .background(MailstackColors.primaryContainer, in: Circle())
            }
            .accessibilityLabel("Account and settings")
        }
        .padding(.horizontal, 12)
        .frame(height: MailstackColors.topBarHeight)
        .foregroundStyle(MailstackColors.onSurface(scheme))
        .background(MailstackColors.surface(scheme))
    }
}

struct MoNavBar: View {
    @ObservedObject var model: AppModel
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        HStack(spacing: 0) {
            ForEach(AppTab.allCases) { tab in
                let selected = model.tab == tab
                Button {
                    if model.tab != tab {
                        model.tab = tab
                        model.threadId = nil
                        model.setBars(hidden: false)
                    }
                } label: {
                    VStack(spacing: 2) {
                        ZStack {
                            if selected {
                                Capsule()
                                    .fill(MailstackColors.secondaryContainer(scheme))
                                    .frame(width: 64, height: 32)
                            }
                            Image(systemName: tab.symbol)
                                .font(.system(size: 22))
                                .foregroundStyle(selected ? MailstackColors.onSecondaryContainer(scheme) : MailstackColors.onSurfaceVariant(scheme))
                        }
                        .frame(height: 32)
                        .padding(.top, 12)
                        Text(tab.label)
                            .font(.system(size: 12, weight: selected ? .semibold : .regular))
                            .foregroundStyle(selected ? MailstackColors.onSurface(scheme) : MailstackColors.onSurfaceVariant(scheme))
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(tab.label) tab")
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
        .frame(height: MailstackColors.barContentHeight)
        .background(MailstackColors.surfaceContainer(scheme))
    }
}
