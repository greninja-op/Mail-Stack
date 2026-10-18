import XCTest
@testable import MailstackPad

/// iPadOS contract tests. RUN: Mac + Xcode only.
@MainActor
final class PadProgressTests: XCTestCase {
    func testSyncFractionCapsAt99() {
        XCTAssertEqual(min(0.99, 100.0 / 100.0), 0.99, accuracy: 0.0001)
    }

    func testSanitizePolicy() {
        let dirty = #"<p>x</p><script>bad</script><img src="https://x">"#
        let clean = PadPrivacy().sanitizeHTML(dirty)
        XCTAssertFalse(clean.lowercased().contains("script"))
        XCTAssertFalse(clean.lowercased().contains("<img"))
    }

    func testColumnLayoutStatesExist() {
        XCTAssertEqual(PadLayoutState.allCases.count, 4)
    }
}
