import SwiftUI

struct AccountSwitcherSheet: View {
    @EnvironmentObject private var container: AppContainer
    @Binding var isPresented: Bool
    var onOpenSettings: () -> Void
    @State private var accounts: [AccountSnapshot] = []

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(accounts) { account in
                        Button {
                            Task {
                                await container.accounts.switchAccount(account.accountId)
                                isPresented = false
                            }
                        } label: {
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(account.displayName ?? account.email)
                                        .font(.system(size: MSTypography.bodyLarge, weight: .semibold))
                                    Text(account.email)
                                        .font(.system(size: MSTypography.labelSmall))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                if account.isActive {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundStyle(Color.accentColor)
                                }
                            }
                        }
                    }
                }
                Section {
                    Button("Add another account") {
                        Task {
                            await container.accounts.addDemoAccount(email: "second@mailstack.local")
                            accounts = await container.accounts.listAccounts()
                        }
                    }
                    Button("Settings", action: onOpenSettings)
                }
            }
            .navigationTitle("Accounts")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { isPresented = false }
                }
            }
            .task { accounts = await container.accounts.listAccounts() }
        }
        .presentationDetents([.medium, .large])
    }
}
