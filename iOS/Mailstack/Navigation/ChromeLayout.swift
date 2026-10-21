import SwiftUI
import UIKit

/// Android `PrimaryContent` + `MoNavBar` inset contract, mapped to iPhone.
///
/// The app bar is always 80pt of content. The system bottom inset (home
/// indicator, or 0 when the device has none or the indicator is hidden)
/// sits OUTSIDE that 80pt. It is never a hardcoded gap.
///
/// When the app bar hides on scroll, only the 80pt collapses. The system
/// inset stays, so the last row cannot slide under the home indicator.
enum ChromeLayout {
    static let barContentHeight: CGFloat = MailstackColors.barContentHeight
    static let topBarHeight: CGFloat = MailstackColors.topBarHeight

    static func contentTopPadding(systemTopInset: CGFloat, barsAmount: CGFloat) -> CGFloat {
        (systemTopInset + topBarHeight) * clamp(barsAmount)
    }

    static func contentBottomPadding(systemBottomInset: CGFloat, barsAmount: CGFloat) -> CGFloat {
        max(0, systemBottomInset) + barContentHeight * clamp(barsAmount)
    }

    static func barTotalHeight(systemBottomInset: CGFloat) -> CGFloat {
        barContentHeight + max(0, systemBottomInset)
    }

    static func clamp(_ amount: CGFloat) -> CGFloat {
        min(1, max(0, amount))
    }

    /// Live window insets. Zero when there is no home indicator and no
    /// status-bar inset, so the bars sit on the screen edge.
    @MainActor
    static func currentWindowInsets() -> (top: CGFloat, bottom: CGFloat) {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap(\.windows).first { $0.isKeyWindow } ?? scenes.flatMap(\.windows).first
        let insets = window?.safeAreaInsets ?? .zero
        return (insets.top, insets.bottom)
    }
}
