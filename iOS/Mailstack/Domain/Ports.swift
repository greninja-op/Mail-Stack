import Foundation
import Combine

protocol AccountSessionPort: AnyObject {
    var activeAccountPublisher: AnyPublisher<AccountId?, Never> { get }
    func listAccounts() async -> [AccountSnapshot]
    func switchAccount(_ id: AccountId) async
    func addDemoAccount(email: String) async
}

protocol MailboxReaderPort: AnyObject {
    func observeThreads(accountId: AccountId, destination: MailDestination) -> AnyPublisher<[ThreadSummary], Never>
    func message(accountId: AccountId, threadId: String) async -> ThreadSummary?
    func setStarred(threadId: String, starred: Bool) async
}

protocol SyncCoordinatorPort: AnyObject {
    func observeProgress(accountId: AccountId) -> AnyPublisher<SyncProgress, Never>
    func startSync(accountId: AccountId) async
    func cancelSync(accountId: AccountId) async
}

protocol SearchPort: AnyObject {
    func search(accountId: AccountId, query: StructuredQuery) async -> [ThreadSummary]
}

protocol ClassificationPort: AnyObject {
    func category(for threadId: String) async -> CategoryGroup
    func grouped(accountId: AccountId) async -> [(CategoryGroup, [ThreadSummary])]
}

protocol ActionEnginePort: AnyObject {
    func propose(accountId: AccountId) async -> [ProposedAction]
    func execute(actionId: String) async -> Bool
}

protocol AuthPort: AnyObject {
    var isSignedInPublisher: AnyPublisher<Bool, Never> { get }
    func beginSignIn() async -> Result<Void, Error>
    func signOut() async
    func hasModifyScope() async -> Bool
}

protocol PersistencePort: AnyObject {
    func loadBootstrap() async
    func wipeAllLocalData() async
}

protocol PrivacyPort: AnyObject {
    func sanitizeHTML(_ html: String) -> String
    func decodeEntities(_ text: String) -> String
    func shouldFetchRemoteImages() -> Bool
}

protocol OfflinePort: AnyObject {
    var isOnlinePublisher: AnyPublisher<Bool, Never> { get }
}

protocol AIPort: AnyObject {
    var isEnabled: Bool { get }
    func summarize(threadId: String) async -> String?
}

protocol AutomationPort: AnyObject {
    func listRules() async -> [String]
}

protocol DiagnosticsPort: AnyObject {
    func log(_ event: String, fields: [String: String])
}
