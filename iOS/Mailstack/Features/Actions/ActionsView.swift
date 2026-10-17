import SwiftUI

struct ActionsView: View {
    @EnvironmentObject private var container: AppContainer
    @State private var actions: [ProposedAction] = []
    @State private var message: String?

    var body: some View {
        List {
            if let message {
                Text(message).foregroundStyle(.secondary)
            }
            ForEach(actions) { action in
                VStack(alignment: .leading, spacing: 6) {
                    Text(action.title).fontWeight(.semibold)
                    Text(action.rationale)
                        .font(.system(size: MSTypography.bodyMedium))
                        .foregroundStyle(.secondary)
                    Button("Apply") {
                        Task {
                            let ok = await container.actions.execute(actionId: action.id)
                            message = ok ? "Applied: \(action.title)" : "Could not apply"
                        }
                    }
                    .buttonStyle(.bordered)
                }
                .padding(.vertical, 4)
            }
        }
        .navigationTitle("Actions")
        .task {
            let accounts = await container.accounts.listAccounts()
            guard let id = accounts.first(where: \.isActive)?.accountId else { return }
            actions = await container.actions.propose(accountId: id)
        }
    }
}
