import Foundation

struct AutomationRule: Identifiable, Codable, Equatable {
    let id: String
    var title: String
    var enabled: Bool
    var dryRunOnly: Bool
}

@MainActor
final class AutomationRuleStore: ObservableObject {
    @Published private(set) var rules: [AutomationRule] = []

    func upsert(_ rule: AutomationRule) {
        if let i = rules.firstIndex(where: { $0.id == rule.id }) {
            rules[i] = rule
        } else {
            rules.append(rule)
        }
    }

    func historyStub() -> [String] { [] }
}
