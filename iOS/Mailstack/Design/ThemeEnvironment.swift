import SwiftUI

struct MSTheme {
    let scheme: ColorScheme
    var brand: Color { MSColor.brand(for: scheme) }
    var background: Color { MSColor.background(for: scheme) }
    var surface: Color { MSColor.surface(for: scheme) }
}

private struct MSThemeKey: EnvironmentKey {
    static let defaultValue = MSTheme(scheme: .light)
}

extension EnvironmentValues {
    var msTheme: MSTheme {
        get { self[MSThemeKey.self] }
        set { self[MSThemeKey.self] = newValue }
    }
}
