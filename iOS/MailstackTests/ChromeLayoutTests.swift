import XCTest
@testable import Mailstack

final class ChromeLayoutTests: XCTestCase {
    func testNoSystemInsetLeavesNoGap() {
        XCTAssertEqual(ChromeLayout.contentBottomPadding(systemBottomInset: 0, barsAmount: 1), 80)
        XCTAssertEqual(ChromeLayout.barTotalHeight(systemBottomInset: 0), 80)
    }

    func testHiddenAppBarKeepsSystemInset() {
        XCTAssertEqual(ChromeLayout.contentBottomPadding(systemBottomInset: 34, barsAmount: 0), 34)
        XCTAssertEqual(ChromeLayout.contentBottomPadding(systemBottomInset: 0, barsAmount: 0), 0)
    }

    func testHomeIndicatorSitsOutsideTheBar() {
        XCTAssertEqual(ChromeLayout.contentBottomPadding(systemBottomInset: 34, barsAmount: 1), 114)
        XCTAssertEqual(ChromeLayout.barTotalHeight(systemBottomInset: 34), 114)
    }

    func testThreeButtonSizedInsetIsNotHardcoded() {
        XCTAssertEqual(ChromeLayout.contentBottomPadding(systemBottomInset: 48, barsAmount: 1), 128)
    }
}
