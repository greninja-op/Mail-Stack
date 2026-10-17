package com.greninjaop.mailorganizer.shared.domain

import com.greninjaop.mailorganizer.shared.core.AccountId
import com.greninjaop.mailorganizer.shared.core.ThreadSummary

/**
 * Deterministic demo mailbox for Simulator / Linux validation of shared
 * contracts. Mirrors Android fixture-* / local-* demo policy: never sync.
 */
object DemoMailbox {
    val DEMO_ACCOUNT = AccountId("local-demo")

    fun threads(): List<ThreadSummary> = listOf(
        thread(
            id = "t1",
            subject = "Welcome to Mailstack",
            snippet = "Your inbox stays organized without leaving your device.",
            sender = "Mailstack",
            address = "hello@mailstack.app",
            domain = "mailstack.app",
            unread = true,
            starred = false,
            offsetHours = 1,
        ),
        thread(
            id = "t2",
            subject = "Invoice ready",
            snippet = "Your monthly invoice is ready to review.",
            sender = "Billing",
            address = "billing@acme.example",
            domain = "acme.example",
            unread = true,
            starred = true,
            offsetHours = 5,
        ),
        thread(
            id = "t3",
            subject = "Team standup notes",
            snippet = "Notes from today's standup are attached.",
            sender = "Alex Rivera",
            address = "alex@example.com",
            domain = null,
            unread = false,
            starred = false,
            offsetHours = 26,
        ),
        thread(
            id = "t4",
            subject = "Flash sale this weekend",
            snippet = "Save 20% on selected items — ends Sunday.",
            sender = "Shop",
            address = "deals@shop.example",
            domain = "shop.example",
            unread = false,
            starred = false,
            offsetHours = 48,
        ),
        thread(
            id = "t5",
            subject = "Security alert",
            snippet = "A new sign-in was detected on your account.",
            sender = "Security",
            address = "noreply@secure.example",
            domain = "secure.example",
            unread = true,
            starred = false,
            offsetHours = 3,
        ),
    )

    private fun thread(
        id: String,
        subject: String,
        snippet: String,
        sender: String,
        address: String,
        domain: String?,
        unread: Boolean,
        starred: Boolean,
        offsetHours: Int,
    ): ThreadSummary {
        val now = 1_700_000_000_000L
        return ThreadSummary(
            threadId = id,
            accountId = DEMO_ACCOUNT,
            subject = subject,
            snippet = snippet,
            senderName = sender,
            senderAddress = address,
            senderDomain = domain,
            timestampEpochMs = now - offsetHours * 3_600_000L,
            unread = unread,
            starred = starred,
            accountBadgeLabel = "D",
        )
    }
}
