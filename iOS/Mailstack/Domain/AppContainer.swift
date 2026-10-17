import Foundation
import Combine

/// Dependency bootstrap boundary — views talk to ports, not repositories.
@MainActor
final class AppContainer: ObservableObject {
    let accounts: AccountSessionPort
    let mailbox: MailboxReaderPort
    let sync: SyncCoordinatorPort
    let search: SearchPort
    let classification: ClassificationPort
    let actions: ActionEnginePort
    let auth: AuthPort
    let persistence: PersistencePort
    let privacy: PrivacyPort
    let offline: OfflinePort
    let ai: AIPort
    let automation: AutomationPort
    let diagnostics: DiagnosticsPort

    init(
        accounts: AccountSessionPort,
        mailbox: MailboxReaderPort,
        sync: SyncCoordinatorPort,
        search: SearchPort,
        classification: ClassificationPort,
        actions: ActionEnginePort,
        auth: AuthPort,
        persistence: PersistencePort,
        privacy: PrivacyPort,
        offline: OfflinePort,
        ai: AIPort,
        automation: AutomationPort,
        diagnostics: DiagnosticsPort
    ) {
        self.accounts = accounts
        self.mailbox = mailbox
        self.sync = sync
        self.search = search
        self.classification = classification
        self.actions = actions
        self.auth = auth
        self.persistence = persistence
        self.privacy = privacy
        self.offline = offline
        self.ai = ai
        self.automation = automation
        self.diagnostics = diagnostics
    }

    static func bootstrap() -> AppContainer {
        let store = InMemoryStore()
        let accounts = DemoAccountSession(store: store)
        let mailbox = DemoMailboxReader(store: store)
        let sync = DemoSyncCoordinator(store: store)
        let search = DemoSearch(store: store)
        let classification = DemoClassification(store: store)
        let actions = DemoActionEngine()
        let auth = DemoAuth(store: store)
        let persistence = LocalPersistence(store: store)
        let privacy = DefaultPrivacy()
        let offline = DefaultOffline()
        let ai = StubAI()
        let automation = StubAutomation()
        let diagnostics = SafeDiagnostics()
        return AppContainer(
            accounts: accounts,
            mailbox: mailbox,
            sync: sync,
            search: search,
            classification: classification,
            actions: actions,
            auth: auth,
            persistence: persistence,
            privacy: privacy,
            offline: offline,
            ai: ai,
            automation: automation,
            diagnostics: diagnostics
        )
    }
}
