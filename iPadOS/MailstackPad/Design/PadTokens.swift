import SwiftUI

enum PadColor {
    static let brandVioletLight = Color(red: 82/255, green: 56/255, blue: 232/255)
    static let brandVioletDark = Color(red: 185/255, green: 168/255, blue: 255/255)
    static let darkBackground = Color(red: 19/255, green: 17/255, blue: 32/255)
    static let cardRadius: CGFloat = 28
    static let avatar: CGFloat = 40
}

enum PadMotion {
    static let settings: Double = 0.35
    static let reveal: Double = 0.35
}

enum PadLayoutState: String, CaseIterable {
    case compactSingle   // narrow / Slide Over
    case regularSplit    // sidebar + list
    case expandedTriple  // sidebar + list + detail
    case inspectorOpen   // + inspector
}
