import SwiftUI

/// Centralized tokens from execution plan/iOS/design.md — no per-screen magic values.
enum MSColor {
    static let brandVioletLight = Color(hex: 0x5238E8)
    static let brandVioletDark = Color(hex: 0xB9A8FF)
    static let darkBackground = Color(hex: 0x131120)
    static let darkSurface = Color(hex: 0x1C1A2E)
    static let lightBackground = Color(hex: 0xFFFFFF)
    static let lightSurface = Color(hex: 0xF3F0FF)

    static func brand(for scheme: ColorScheme) -> Color {
        scheme == .dark ? brandVioletDark : brandVioletLight
    }

    static func background(for scheme: ColorScheme) -> Color {
        scheme == .dark ? darkBackground : lightBackground
    }

    static func surface(for scheme: ColorScheme) -> Color {
        scheme == .dark ? darkSurface : lightSurface
    }

    // Category accents
    static let needsAttention = Color(hex: 0xE6A817)
    static let promotions = Color(hex: 0xE07020)
    static let updates = Color(hex: 0x2BA3D4)
    static let workLearning = Color(hex: 0x3B5BDB)
}

enum MSTypography {
    static let displayLarge: CGFloat = 64
    static let displayMedium: CGFloat = 57
    static let titleLarge: CGFloat = 22
    static let titleMedium: CGFloat = 17
    static let titleSmall: CGFloat = 16
    static let bodyLarge: CGFloat = 16
    static let bodyMedium: CGFloat = 14
    static let labelLarge: CGFloat = 14
    static let labelSmall: CGFloat = 12
}

enum MSShape {
    static let cardRadius: CGFloat = 28
    static let avatarSize: CGFloat = 40
    static let mailRowMinHeight: CGFloat = 88
    static let minHitTarget: CGFloat = 44
}

enum MSMotion {
    static let settingsPanel: Double = 0.35
    static let scrollHideBars: Double = 0.25
    static let messageReveal: Double = 0.35
    static let settingsUnderlyingShift: CGFloat = 1.0 / 12.0
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}
