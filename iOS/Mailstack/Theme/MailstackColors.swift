import SwiftUI

/// Android `Color.kt` — Mailstack Expressive. Solid tonal surfaces, no glass.
enum MailstackColors {
    static let brand = Color(hex: 0x5238E8)
    static let brandDark = Color(hex: 0xB9A8FF)
    static let onBrand = Color.white
    static let onBrandDark = Color(hex: 0x2E1B8A)

    static let primaryContainer = Color(hex: 0xE3DCFF)
    static let onPrimaryContainer = Color(hex: 0x1D0F6E)
    static let secondaryContainer = Color(hex: 0xE4E0FF)
    static let onSecondaryContainer = Color(hex: 0x241A5E)

    static let background = Color(hex: 0xF6F3FF)
    static let surface = Color(hex: 0xFDFBFF)
    static let onSurface = Color(hex: 0x1C1A26)
    static let onSurfaceVariant = Color(hex: 0x48455A)
    static let surfaceContainer = Color(hex: 0xEFEAFB)
    static let surfaceContainerHigh = Color(hex: 0xE7E0F6)
    static let outlineVariant = Color(hex: 0xCBC5DE)

    static let backgroundDark = Color(hex: 0x131120)
    static let surfaceDark = Color(hex: 0x1A1826)
    static let onSurfaceDark = Color(hex: 0xF0EDFB)
    static let onSurfaceVariantDark = Color(hex: 0xCBC6DE)
    static let surfaceContainerDark = Color(hex: 0x262340)
    static let surfaceContainerHighDark = Color(hex: 0x322D4E)
    static let primaryContainerDark = Color(hex: 0x4634A8)
    static let secondaryContainerDark = Color(hex: 0x4D4096)
    static let onSecondaryContainerDark = Color(hex: 0xC9BEFF)

    static let needsAttention = Color(hex: 0xB45309)
    static let promotions = Color(hex: 0xE67E22)
    static let updates = Color(hex: 0x3B82C4)
    static let work = Color(hex: 0x1D4ED8)
    static let everythingElse = brand

    static let cardRadius: CGFloat = 28
    static let barContentHeight: CGFloat = 80
    static let topBarHeight: CGFloat = 64
    static let avatar: CGFloat = 40
    static let rowMinHeight: CGFloat = 88

    static func background(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? backgroundDark : background
    }

    static func surface(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? surfaceDark : surface
    }

    static func onSurface(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? onSurfaceDark : onSurface
    }

    static func onSurfaceVariant(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? onSurfaceVariantDark : onSurfaceVariant
    }

    static func surfaceContainer(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? surfaceContainerDark : surfaceContainer
    }

    static func surfaceContainerHigh(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? surfaceContainerHighDark : surfaceContainerHigh
    }

    static func brand(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? brandDark : brand
    }

    static func secondaryContainer(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? secondaryContainerDark : secondaryContainer
    }

    static func onSecondaryContainer(_ scheme: ColorScheme) -> Color {
        scheme == .dark ? onSecondaryContainerDark : onSecondaryContainer
    }
}

extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}
