package com.greninjaop.mailorganizer.shared

import com.greninjaop.mailorganizer.shared.core.AccountId
import com.greninjaop.mailorganizer.shared.domain.DemoMailbox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AccountIdTest {
    @Test
    fun rejectsBlank() {
        assertFailsWith<IllegalArgumentException> { AccountId("  ") }
    }

    @Test
    fun demoThreadsAreNonEmptyAndScoped() {
        val threads = DemoMailbox.threads()
        assertTrue(threads.isNotEmpty())
        assertTrue(threads.all { it.accountId == DemoMailbox.DEMO_ACCOUNT })
    }

    @Test
    fun demoAccountIdIsStable() {
        assertEquals("local-demo", DemoMailbox.DEMO_ACCOUNT.value)
    }
}
