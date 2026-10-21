import SwiftUI

/// Full-screen settings panel. The shell slides it; this view only fills the screen.
struct SettingsPanel: View {
    @ObservedObject private var model: AppModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var topInset: CGFloat = 0
    @State private var bottomInset: CGFloat = 0

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
    }

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.top, topInset)
                .background(MailstackColors.surface(scheme))

            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    accountHeader
                    appearanceCard
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            footer
                .padding(.horizontal, 24)
                .padding(.top, 12)
                .padding(.bottom, 16 + bottomInset)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(MailstackColors.background(scheme))
        .onAppear(perform: refreshInsets)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshInsets() }
        }
    }

    private var header: some View {
        HStack(spacing: 4) {
            Button {
                model.settingsOpen = false
            } label: {
                Image(systemName: "xmark")
                    .font(.body.weight(.semibold))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close")

            Text("Settings")
                .font(.title3.weight(.semibold))
                .foregroundStyle(MailstackColors.onSurface(scheme))

            Spacer(minLength: 0)
        }
        .padding(.trailing, 16)
        .foregroundStyle(MailstackColors.onSurface(scheme))
    }

    private var accountHeader: some View {
        HStack(alignment: .center, spacing: 16) {
            Text(String(DemoMailbox.accountName.first ?? "M"))
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(MailstackColors.onSecondaryContainer(scheme))
                .frame(width: MailstackColors.avatar, height: MailstackColors.avatar)
                .background(MailstackColors.secondaryContainer(scheme), in: Circle())
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(DemoMailbox.accountName)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                Text(DemoMailbox.accountEmail)
                    .font(.subheadline)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                Text("Local demo. Gmail is not connected.")
                    .font(.subheadline)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .combine)
    }

    private var appearanceCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Appearance")
                .font(.headline)
                .foregroundStyle(MailstackColors.onSurface(scheme))

            Text("Theme")
                .font(.subheadline)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))

            HStack(spacing: 8) {
                themeChip("System", selected: model.themeOverride == nil) {
                    model.themeOverride = nil
                }
                themeChip("Light", selected: model.themeOverride == .light) {
                    model.themeOverride = .light
                }
                themeChip("Dark", selected: model.themeOverride == .dark) {
                    model.themeOverride = .dark
                }
            }
            .accessibilityElement(children: .contain)

            Toggle(isOn: $model.reduceMotion) {
                Text("Reduce motion")
                    .font(.body)
                    .foregroundStyle(MailstackColors.onSurface(scheme))
            }
            .tint(MailstackColors.brand(scheme))
            .frame(minHeight: 44)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            MailstackColors.surfaceContainer(scheme),
            in: RoundedRectangle(cornerRadius: MailstackColors.cardRadius, style: .continuous)
        )
    }

    private func themeChip(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: "checkmark")
                    .font(.caption.weight(.bold))
                    .opacity(selected ? 1 : 0)
                    .accessibilityHidden(true)
                Text(title)
                    .font(.subheadline.weight(selected ? .semibold : .regular))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .padding(.horizontal, 10)
            .frame(maxWidth: .infinity, minHeight: 44)
            .foregroundStyle(
                selected
                    ? MailstackColors.onSecondaryContainer(scheme)
                    : MailstackColors.onSurfaceVariant(scheme)
            )
            .background(
                selected
                    ? MailstackColors.secondaryContainer(scheme)
                    : MailstackColors.surface(scheme),
                in: Capsule()
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(title) theme")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    /// Informational footer only — not buttons, and not a connection or import action.
    private var footer: some View {
        VStack(spacing: 8) {
            Text("Privacy · Terms")
                .font(.caption)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
            Text("Mailstack keeps the demo mailbox on this device.")
                .font(.footnote)
                .multilineTextAlignment(.center)
                .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
        }
        .frame(maxWidth: .infinity)
    }

    private func refreshInsets() {
        let insets = ChromeLayout.currentWindowInsets()
        topInset = insets.top
        bottomInset = insets.bottom
    }
}
