package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The mail row carries the sender's canonical company domain (drives the
 * company logo in every row) — null for personal senders, who keep the
 * letter avatar.
 */
class ThreadItemSenderDomainTest {

    private fun thread(latest: MessageRecord?) = ThreadRecord(
        threadId = "t1",
        gmailThreadId = null,
        accountId = "a1",
        subject = "Hello",
        participantDisplayNames = emptyList(),
        messageCount = 1,
        unreadCount = 0,
        latestMessageId = latest?.messageId,
        latestMessageEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )

    private fun msg(from: String) = MessageRecord(
        messageId = "m1",
        gmailMessageId = null,
        threadId = "t1",
        accountId = "a1",
        fromAddress = from,
        fromName = null,
        subject = "Hello",
        snippet = null,
        bodyText = null,
        timestampEpochMs = 0L,
    )

    @Test
    fun `company sender resolves to canonical domain`() {
        val item = thread(msg("noreply@mailer.netflix.com")).toThreadItem(msg("noreply@mailer.netflix.com"))
        assertEquals("netflix.com", item.senderDomain)
    }

    @Test
    fun `redditmail sender resolves to redditmail domain`() {
        val m = msg("noreply@redditmail.com")
        assertEquals("redditmail.com", thread(m).toThreadItem(m).senderDomain)
    }

    @Test
    fun `personal sender has no company domain`() {
        val m = msg("friend@gmail.com")
        assertNull(thread(m).toThreadItem(m).senderDomain)
    }

    @Test
    fun `missing latest message has no company domain`() {
        assertNull(thread(null).toThreadItem(null).senderDomain)
    }
}
