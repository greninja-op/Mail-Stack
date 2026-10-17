import Foundation

/// Phase 15 — adaptive tiers, not device names. Never claim 120 FPS.
enum CapabilityTier: String {
    case constrained, balanced, expressive
}

struct CapabilityProfile {
    var tier: CapabilityTier = .balanced
    var allowExpressiveMotion: Bool { tier == .expressive }
    var syncBatchSize: Int {
        switch tier {
        case .constrained: return 25
        case .balanced: return 50
        case .expressive: return 100
        }
    }

    static func current(reducedMotion: Bool, lowPower: Bool) -> CapabilityProfile {
        if reducedMotion || lowPower {
            return CapabilityProfile(tier: .constrained)
        }
        return CapabilityProfile(tier: .balanced)
    }
}
