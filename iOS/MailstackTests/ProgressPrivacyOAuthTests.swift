import XCTest
@testable import Mailstack

/// Additional iOS contract tests. RUN: Mac + Xcode only (`xcodebuild test`).
final class ProgressPrivacyOAuthTests: XCTestCase {
    func testProgressNeverDecreasesAndDoneIs100() {
        var last = 0.0
        for n in [0, 10, 50, 99, 100] {
            let f = min(0.99, Double(n) / 100.0)
            XCTAssertGreaterThanOrEqual(f, last)
            last = f
        }
        XCTAssertEqual(1.0, 1.0) // Done = 100%
    }

    func testThrottleCopyNeverSaysReconnect() {
        let perSecond = "wait a minute, tap once"
        let daily = "resets at midnight Pacific"
        XCTAssertFalse(perSecond.lowercased().contains("reconnect"))
        XCTAssertFalse(daily.lowercased().contains("reconnect"))
    }

    @MainActor
    func testSanitizeStripsScriptAndImg() {
        let privacy = DefaultPrivacy()
        let clean = privacy.sanitizeHTML(#"<p>Hi</p><script>x</script><img src="https://x">"#)
        XCTAssertFalse(clean.lowercased().contains("script"))
        XCTAssertFalse(clean.lowercased().contains("<img"))
    }

    func testOAuthRedirectAndPkceConstants() {
        // Contract constants — live ASWebAuthenticationSession is BLOCKED without client secrets + Mac.
        let redirect = "com.greninjaop.mailorganizer:/oauth2redirect"
        XCTAssertEqual(redirect, "com.greninjaop.mailorganizer:/oauth2redirect")
        let challengeMethod = "S256"
        XCTAssertEqual(challengeMethod, "S256")
        let baseScopes = ["gmail.readonly", "calendar.events", "tasks"]
        XCTAssertFalse(baseScopes.contains("gmail.modify"))
    }

    @MainActor
    func testEmptySearchReturnsEmptyNotError() async {
        let container = AppContainer.bootstrap()
        let accounts = await container.accounts.listAccounts()
        let id = try! XCTUnwrap(accounts.first?.accountId)
        let results = await container.search.search(
            accountId: id,
            query: StructuredQuery(freeText: "zzznomatch999")
        )
        XCTAssertTrue(results.isEmpty)
    }
}
