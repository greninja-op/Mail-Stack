import SwiftUI

/// Full-screen search over the local demo mailbox. Empty query lists nothing.
struct SearchScreen: View {
    @ObservedObject private var model: AppModel
    @Environment(\.colorScheme) private var scheme
    @Environment(\.scenePhase) private var scenePhase
    @FocusState private var fieldFocused: Bool
    @State private var topInset: CGFloat = 0
    @State private var bottomInset: CGFloat = 0

    init(model: AppModel) {
        _model = ObservedObject(wrappedValue: model)
    }

    var body: some View {
        VStack(spacing: 0) {
            searchBar
                .padding(.top, topInset)
                .background(MailstackColors.surface(scheme))

            Group {
                if model.searchText.isEmpty {
                    hint("Search senders, subjects, and snippets.")
                } else if matches.isEmpty {
                    hint("No messages match.")
                } else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(matches) { message in
                                Button {
                                    model.openThread(message.id)
                                    model.searchOpen = false
                                } label: {
                                    SearchMailRow(message: message, scheme: scheme)
                                }
                                .buttonStyle(SearchRowButtonStyle(scheme: scheme))
                            }
                        }
                    }
                    .scrollDismissesKeyboard(.interactively)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.bottom, bottomInset)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(MailstackColors.background(scheme))
        .onAppear {
            refreshInsets()
            fieldFocused = true
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshInsets() }
        }
    }

    private var searchBar: some View {
        HStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .accessibilityHidden(true)
                TextField(
                    "",
                    text: $model.searchText,
                    prompt: Text("Search mail")
                        .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                )
                .focused($fieldFocused)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.search)
                .foregroundStyle(MailstackColors.onSurface(scheme))
                .accessibilityLabel("Search mail")
            }
            .padding(.horizontal, 14)
            .frame(minHeight: 44)
            .background(MailstackColors.surfaceContainerHigh(scheme), in: Capsule())

            Button {
                model.searchText = ""
                model.searchOpen = false
            } label: {
                Text("Cancel")
                    .font(.body)
                    .foregroundStyle(MailstackColors.brand(scheme))
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    private var matches: [DemoMessage] {
        let query = model.searchText
        if query.isEmpty { return [] }
        return model.mailbox.messages.filter { message in
            message.sender.localizedCaseInsensitiveContains(query)
                || message.subject.localizedCaseInsensitiveContains(query)
                || message.snippet.localizedCaseInsensitiveContains(query)
        }
    }

    private func hint(_ text: String) -> some View {
        Text(text)
            .font(.body)
            .multilineTextAlignment(.center)
            .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
            .padding(.horizontal, 32)
            .padding(.top, 48)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func refreshInsets() {
        let insets = ChromeLayout.currentWindowInsets()
        topInset = insets.top
        bottomInset = insets.bottom
    }
}

/// Flat 3-line mail row: 40pt initial, sender / subject / snippet. Unread is semibold.
private struct SearchMailRow: View {
    let message: DemoMessage
    let scheme: ColorScheme

    var body: some View {
        HStack(alignment: .center, spacing: 16) {
            Text(message.initial)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(MailstackColors.onSecondaryContainer(scheme))
                .frame(width: MailstackColors.avatar, height: MailstackColors.avatar)
                .background(MailstackColors.secondaryContainer(scheme), in: Circle())
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
                        .fixedSize(horizontal: true, vertical: false)
                }
                Text(message.subject)
                    .font(.subheadline.weight(message.unread ? .semibold : .regular))
                    .foregroundStyle(MailstackColors.onSurface(scheme))
                    .lineLimit(1)
                Text(message.snippet)
                    .font(.subheadline)
                    .foregroundStyle(MailstackColors.onSurfaceVariant(scheme))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if message.starred {
                Image(systemName: "star.fill")
                    .font(.system(size: 14))
                    .foregroundStyle(MailstackColors.needsAttention)
                    .accessibilityHidden(true)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, minHeight: MailstackColors.rowMinHeight, alignment: .leading)
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
        .accessibilityAddTraits(.isButton)
    }

    private var accessibilityText: String {
        var parts = [
            message.unread ? "Unread" : "Read",
            message.sender,
            message.subject,
            message.snippet,
            message.timeLabel
        ]
        if message.starred {
            parts.append("Starred")
        }
        return parts.joined(separator: ", ")
    }
}

/// Solid press fill on the flat row. No material, no shadow.
private struct SearchRowButtonStyle: ButtonStyle {
    let scheme: ColorScheme

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed ? MailstackColors.surfaceContainer(scheme) : Color.clear
            )
    }
}
