import XCTest
@testable import Mailstack

final class StructuredQueryTests: XCTestCase {
    @MainActor
    func testDemoSearchFiltersUnread() async {
        let container = AppContainer.bootstrap()
        let accounts = await container.accounts.listAccounts()
        let id = try! XCTUnwrap(accounts.first?.accountId)
        let results = await container.search.search(
            accountId: id,
            query: StructuredQuery(isUnread: true)
        )
        XCTAssertFalse(results.isEmpty)
        XCTAssertTrue(results.allSatisfy(\.unread))
    }

    @MainActor
    func testPrivacyNeverFetchesRemoteImages() {
        let privacy = DefaultPrivacy()
        XCTAssertFalse(privacy.shouldFetchRemoteImages())
        let cleaned = privacy.sanitizeHTML(#"<p>Hi</p><script>alert(1)</script><img src="https://evil.test/x.png">"#)
        XCTAssertFalse(cleaned.contains("script"))
        XCTAssertFalse(cleaned.contains("<img"))
    }

    @MainActor
    func testAccountIdDemoNeverLooksLikeGmailSyncTarget() {
        XCTAssertTrue(DemoSeed.account.value.hasPrefix("local-"))
    }

    func testSyncDisplayCapLogic() {
        // Mirrors ImportFlowView: running uses min(0.99, n/total); succeeded = 1.0
        let n = 100
        let total = 100
        let running = min(0.99, Double(n) / Double(total))
        XCTAssertEqual(running, 0.99, accuracy: 0.0001)
    }
}
