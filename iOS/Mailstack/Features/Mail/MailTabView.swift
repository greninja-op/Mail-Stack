import SwiftUI

struct MailTabView: View {
    @ObservedObject var model: AppModel
    @ObservedObject private var mailbox: DemoMailbox
    @Environment(\.colorScheme) private var scheme

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
        _mailbox = ObservedObject(wrappedValue: model.mailbox)
    }

    var body: some View {
        let messages = mailbox.visible(folder: model.folder, bucket: model.bucket)
        VStack(spacing: 0) {
            if model.folder == .inbox {
                bucketChips
            }
            if messages.isEmpty {
                emptyFolder
            } else {
                BarHidingScroll(model: model) {
                    VStack(spacing: 0) {
                        ForEach(messages) { message in
                            MailMessageRow(message: message, model: model)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(MailstackColors.background(scheme))
    }

    private var bucketChips: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                chip(.primary)
                chip(.promotional)
                chip(.social)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .scrollIndicators(.hidden)
    }

    private func chip(_ bucket: MailBucket) -> some View {
        let selected = model.bucket == bucket
        return Button {
            model.bucket = bucket
        } label: {
            Text(bucket.label)
                .font(.subheadline.weight(selected ? .semibold : .regular))
                .foregroundStyle(
                    selected
                        ? MailstackColors.onSecondaryContainer(scheme)
                        : MailstackColors.onSurfaceVariant(scheme)
                )
                .padding(.horizontal, 14)
                .frame(minHeight: 32)
                .background(
                    selected
                        ? MailstackColors.secondaryContainer(scheme)
                        : MailstackColors.surfaceContainerHigh(scheme),
                    in: Capsule()
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(bucket.label)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var emptyFolder: some View {
        VStack(spacing: 4) {
            Text("Nothing here")
                .font(.body.weight(.semibold))
                .foregroundStyle(MailstackColors.onSurface(scheme))
            Text("This folder has no demo messages.")
                .font(.subheadline)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                .multilineTextAlignment(.center)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Gmail-style mailbox drawer. Covers the screen from the leading edge,
/// including the app bars, because the shell draws it above them.
struct MailDrawer: View {
    @ObservedObject var model: AppModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion
    @State private var revealed: Bool

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
        _revealed = State(initialValue: model.reduceMotion)
    }

    var body: some View {
        let insets = ChromeLayout.currentWindowInsets()
        GeometryReader { geo in
            let width = min(304, max(240, geo.size.width - 56))
            ZStack(alignment: .leading) {
                Color.black.opacity(revealed ? 0.32 : 0)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .contentShape(Rectangle())
                    .onTapGesture { model.drawerOpen = false }
                    .allowsHitTesting(revealed)
                    .accessibilityLabel("Close mailbox menu")
                    .accessibilityAddTraits(.isButton)
                    .accessibilityAction { model.drawerOpen = false }

                VStack(alignment: .leading, spacing: 0) {
                    accountHeader
                        .padding(.top, insets.top + 12)
                        .padding(.bottom, 12)
                    Rectangle()
                        .fill(MailstackColors.surfaceContainerHigh(scheme))
                        .frame(height: 1)
                    VStack(spacing: 0) {
                        folderButton(.inbox, symbol: "tray", selectedSymbol: "tray.fill")
                        folderButton(.starred, symbol: "star", selectedSymbol: "star.fill")
                        folderButton(.spam, symbol: "exclamationmark.triangle", selectedSymbol: "exclamationmark.triangle.fill")
                        drawerButton(
                            title: AppTab.categories.label,
                            symbol: AppTab.categories.symbol,
                            selected: model.tab == .categories
                        ) {
                            model.tab = .categories
                            model.drawerOpen = false
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.top, 8)
                    Spacer(minLength: 0)
                }
                .padding(.bottom, insets.bottom)
                .frame(width: width, height: geo.size.height, alignment: .top)
                .background(MailstackColors.surface(scheme))
                .offset(x: revealed ? 0 : -width)
                .zIndex(1)
            }
        }
        .onAppear(perform: reveal)
    }

    private var accountHeader: some View {
        HStack(spacing: 12) {
            Text(String(DemoMailbox.accountName.first ?? "D").uppercased())
                .font(.body.weight(.semibold))
                .foregroundStyle(MailstackColors.onPrimaryContainer)
                .frame(width: MailstackColors.avatar, height: MailstackColors.avatar)
                .background(MailstackColors.primaryContainer, in: Circle())
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(DemoMailbox.accountName)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                    .lineLimit(1)
                Text(DemoMailbox.accountEmail)
                    .font(.caption)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .lineLimit(1)
                Text("Local demo")
                    .font(.caption)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .accessibilityElement(children: .combine)
    }

    private func folderButton(_ folder: MailFolder, symbol: String, selectedSymbol: String) -> some View {
        let selected = model.tab != .categories && model.folder == folder
        return drawerButton(
            title: folder.label,
            symbol: selected ? selectedSymbol : symbol,
            selected: selected
        ) {
            model.folder = folder
            model.drawerOpen = false
        }
    }

    private func drawerButton(
        title: String,
        symbol: String,
        selected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: symbol)
                    .font(.system(size: 20))
                    .frame(width: 24, height: 24)
                Text(title)
                    .font(.subheadline.weight(selected ? .semibold : .regular))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 12)
            .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
            .foregroundStyle(
                selected
                    ? MailstackColors.onSecondaryContainer(scheme)
                    : MailstackColors.onSurfaceVariant(scheme)
            )
            .background(
                selected ? MailstackColors.secondaryContainer(scheme) : Color.clear,
                in: RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous)
            )
            .contentShape(RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func reveal() {
        guard !revealed else { return }
        if model.reduceMotion || systemReduceMotion {
            revealed = true
        } else {
            withAnimation(.easeOut(duration: 0.25)) {
                revealed = true
            }
        }
    }
}

/// Flat mail row. The letter circle is the sender initial for both
/// companies and people — no generated logo.
struct MailMessageRow: View {
    let message: DemoMessage
    @ObservedObject var model: AppModel
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        HStack(alignment: .center, spacing: 4) {
            Button {
                model.openThread(message.id)
            } label: {
                HStack(alignment: .center, spacing: 16) {
                    Text(message.initial)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(MailstackColors.onPrimaryContainer)
                        .frame(width: MailstackColors.avatar, height: MailstackColors.avatar)
                        .background(MailstackColors.primaryContainer, in: Circle())
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: 2) {
                        HStack(alignment: .firstTextBaseline, spacing: 8) {
                            Text(message.sender)
                                .font(.body.weight(message.unread ? .semibold : .regular))
                                .foregroundStyle(MailstackColors.onSurface(scheme))
                                .lineLimit(1)
                            Spacer(minLength: 8)
                            Text(message.timeLabel)
                                .font(.caption)
                                .foregroundStyle(
                                    message.unread
                                        ? MailstackColors.brand(scheme)
                                        : MailstackColors.onSurfaceVariant(scheme)
                                )
                                .lineLimit(1)
                                .layoutPriority(1)
                        }
                        Text(message.subject)
                            .font(.subheadline.weight(message.unread ? .semibold : .regular))
                            .foregroundStyle(MailstackColors.onSurface(scheme))
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(message.snippet)
                            .font(.subheadline)
                            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .frame(maxWidth: .infinity, minHeight: MailstackColors.rowMinHeight, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(openLabel)

            Button {
                model.mailbox.toggleStar(message.id)
            } label: {
                Image(systemName: message.starred ? "star.fill" : "star")
                    .font(.system(size: 18))
                    .foregroundStyle(
                        message.starred
                            ? MailstackColors.brand(scheme)
                            : MailstackColors.onSurfaceVariant(scheme)
                    )
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(message.starred ? "Starred" : "Star")
            .accessibilityHint(message.starred ? "Removes the star" : "Stars this message")
            .accessibilityAddTraits(message.starred ? .isSelected : [])
        }
        .padding(.leading, 16)
        .padding(.trailing, 4)
    }

    private var openLabel: String {
        let state = message.unread ? "Unread" : "Read"
        return "\(state), \(message.sender), \(message.subject), \(message.timeLabel)"
    }
}

/// Scroll-down hides the app bars; scroll-up shows them.
/// `minY` shrinks when the list scrolls downward (content moves up),
/// which is Android `available.y < 0`. A finger drag uses the same mapping:
/// finger up scrolls downward and hides; finger down scrolls upward and shows.
struct BarHidingScroll<Content: View>: View {
    @ObservedObject var model: AppModel
    @ViewBuilder var content: () -> Content
    @State private var sample = ScrollSample()

    var body: some View {
        ScrollView {
            content()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(offsetReader)
        }
        .scrollContentBackground(.hidden)
        .coordinateSpace(name: "mailstackBarScroll")
        .onPreferenceChange(BarScrollOffsetKey.self, perform: consumeOffset)
        .simultaneousGesture(
            DragGesture(minimumDistance: 24, coordinateSpace: .local)
                .onChanged { value in
                    // Finger up scrolls the list downward and hides the bars.
                    if value.translation.height < -24 {
                        apply(hidden: true)
                    } else if value.translation.height > 24 {
                        apply(hidden: false)
                    }
                }
        )
    }

    private var offsetReader: some View {
        GeometryReader { geo in
            Color.clear.preference(
                key: BarScrollOffsetKey.self,
                value: geo.frame(in: .named("mailstackBarScroll")).minY
            )
        }
    }

    private func consumeOffset(_ offset: CGFloat) {
        let prior = sample.lastOffset
        sample.lastOffset = offset
        guard let prior else { return }
        let delta = offset - prior
        if abs(delta) < 0.5 { return }
        if (delta < 0 && sample.accumulated > 0) || (delta > 0 && sample.accumulated < 0) {
            sample.accumulated = 0
        }
        sample.accumulated += delta
        // Downward scroll shrinks minY. Upward scroll grows it.
        if sample.accumulated < -24 {
            sample.accumulated = 0
            apply(hidden: true)
        } else if sample.accumulated > 24 {
            sample.accumulated = 0
            apply(hidden: false)
        }
    }

    private func apply(hidden: Bool) {
        guard Date() > sample.suppressUntil else { return }
        if hidden {
            guard model.barsAmount >= 0.01 else { return }
        } else {
            guard model.barsAmount <= 0.99 else { return }
        }
        sample.suppressUntil = Date().addingTimeInterval(0.3)
        model.setBars(hidden: hidden)
    }
}

private struct BarScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = .zero
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private final class ScrollSample {
    var lastOffset: CGFloat?
    var accumulated: CGFloat = 0
    var suppressUntil = Date.distantPast
}
