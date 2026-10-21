import SwiftUI

/// iPad chrome motion. Durations match the phone and Android contract:
/// scroll-hide is 0.25s (`AppModel.setBars`, Android bars tween 250ms)
/// and the settings slide is 0.35s (`RootShell`, Android `MoMotion.panelSlide`
/// 350ms). The axis is flipped. Phone chrome hides vertically — the bottom
/// bar translates down. iPad's persistent sidebar hides horizontally — it
/// translates off the leading edge. Narrow widths still use the bottom axis.
enum PadMotion {
    /// Scroll-hide / scroll-show. Pure translate. Phone uses `easeInOut`.
    static let barHideDuration: TimeInterval = 0.25

    /// Settings panel slide. Same length as the phone settings transition.
    static let settingsSlideDuration: TimeInterval = 0.35

    static let barHide = Animation.easeInOut(duration: barHideDuration)
    static let settingsSlide = Animation.easeInOut(duration: settingsSlideDuration)
}

/// Which way a piece of chrome leaves the screen.
///
/// `bottom` and `leading` are opposites. Bottom chrome slides down
/// (`+height`). Leading chrome slides off the left (`-width`).
enum PadChromeAxis {
    /// Bottom bar, including the narrow-width phone fallback. Slides down.
    case bottom
    /// Sidebar or navigation rail along the leading edge. Slides off the left.
    case leading
}

/// Translation that hides chrome.
///
/// `hidden` is how far the chrome has left: `0` fully shown, `1` fully
/// off screen. It is the complement of the phone `barsAmount` and the
/// Android `barsOffset` (those are `1` when the bars are shown). `span`
/// is that chrome's own height (`bottom`) or width (`leading`) — the
/// distance it must travel to clear the screen. It is not a safe-area
/// inset, and it is not a stand-in 34pt or 49pt iPhone tab-bar hole.
///
/// The two axes are opposites:
/// - `bottom`: height = `+span * hidden` (slides down, the phone bottom bar).
/// - `leading`: width = `-span * hidden` (slides off the left).
func padHideOffset(axis: PadChromeAxis, hidden: CGFloat, span: CGFloat) -> CGSize {
    let travel = span * hidden
    switch axis {
    case .bottom:
        return CGSize(width: 0, height: travel)
    case .leading:
        return CGSize(width: -travel, height: 0)
    }
}

/// Edge the incoming section enters from.
///
/// On the phone and on Android, primary destinations sit in a horizontal
/// bar, and the incoming screen enters from the side that destination sits
/// on. iPad flips that axis. The same destinations are stacked top to
/// bottom in the sidebar, so the incoming screen enters from the vertical
/// edge that destination occupies.
///
/// The list is split in half by integer division. An index in the top half
/// (`index < count / 2`) enters from `.top`. An index in the bottom half
/// (`index >= count / 2`) enters from `.bottom`. When `count` is odd the
/// middle index falls in the bottom half, because `count / 2` truncates.
/// For the five product sections that is Home and Mail from the top, and
/// Categories, Companies, and Actions from the bottom.
///
/// A non-positive `count` has no top half, so the edge is `.bottom`.
func padContentEdge(forDestinationIndex index: Int, count: Int) -> Edge {
    guard count > 0 else { return .bottom }
    return index < count / 2 ? .top : .bottom
}
