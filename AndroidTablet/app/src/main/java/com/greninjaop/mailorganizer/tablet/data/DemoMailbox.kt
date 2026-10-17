package com.greninjaop.mailorganizer.tablet.data

data class AccountId(val value: String) {
    init { require(value.isNotBlank()) }
}

data class ThreadSummary(
    val threadId: String,
    val accountId: AccountId,
    val subject: String,
    val snippet: String,
    val senderName: String,
    val senderAddress: String,
    val senderDomain: String?,
    val unread: Boolean,
    val starred: Boolean,
)

enum class Destination { AllInbox, Primary, Promotional, Social, Spam, Starred }

object DemoMailbox {
    val ACCOUNT = AccountId("local-tablet-demo")
    val threads = listOf(
        ThreadSummary("1", ACCOUNT, "Welcome on tablet", "List-detail workspace.", "Mailstack", "hello@mailstack.app", "mailstack.app", true, false),
        ThreadSummary("2", ACCOUNT, "Invoice ready", "Monthly invoice.", "Billing", "billing@acme.example", "acme.example", true, true),
        ThreadSummary("3", ACCOUNT, "Team notes", "Standup notes.", "Alex", "alex@example.com", null, false, false),
        ThreadSummary("4", ACCOUNT, "Flash sale", "Save 20%.", "Shop", "deals@shop.example", "shop.example", false, false),
        ThreadSummary("5", ACCOUNT, "Security alert", "New sign-in.", "Security", "noreply@secure.example", "secure.example", true, false),
    )
}
