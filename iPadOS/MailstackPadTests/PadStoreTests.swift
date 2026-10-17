import XCTest
@testable import MailstackPad

@MainActor
final class PadStoreTests: XCTestCase {
    func testDemoAccountNeverLooksLikeLiveGmail() {
        let store = PadStore.demo()
        XCTAssertTrue(store.accounts.first!.id.value.hasPrefix("local-"))
    }

    func testPrivacyBlocksRemoteImages() {
        XCTAssertFalse(PadPrivacy().fetchesRemoteImages)
    }
}
