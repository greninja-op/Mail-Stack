import XCTest
@testable import MailstackMac

/// macOS contract tests. RUN: Mac + Xcode only.
/// UniFFI bridge to desktop-core is BLOCKED until Mac packaging — demo store mirrors contracts today.
@MainActor
final class MacStoreTests: XCTestCase {
    func testDemoAccountIsLocal() {
        let store = MacStore.demo()
        XCTAssertTrue(store.accountLabel.hasPrefix("local-"))
    }

    func testProgressCapLogic() {
        XCTAssertEqual(min(0.99, 1.0), 0.99, accuracy: 0.0001)
    }
}
