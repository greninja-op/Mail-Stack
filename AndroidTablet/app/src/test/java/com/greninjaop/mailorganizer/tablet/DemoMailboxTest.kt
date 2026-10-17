package com.greninjaop.mailorganizer.tablet

import com.greninjaop.mailorganizer.tablet.data.DemoMailbox
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoMailboxTest {
    @Test
    fun demoAccountNeverSyncsTarget() {
        assertTrue(DemoMailbox.ACCOUNT.value.startsWith("local-"))
        assertTrue(DemoMailbox.threads.isNotEmpty())
    }
}
