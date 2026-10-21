package com.greninjaop.mailstack.tablet.mail

/**
 * Local demo mailbox for the tablet mail panes.
 *
 * Subjects follow the Android sample mailbox (Amazon delivered, Ananya's
 * design, Netflix sign-in, GitHub pull request, Q3 roadmap task, Spotify
 * receipt, Adobe renewal, lottery spam). This is stored copy on the device.
 * It is not a mailbox connection and it does not report sync progress.
 */
object TabletDemoMailbox {
    const val Label = "Local demo"

    fun seed(): List<TabletDemoMessage> = listOf(
        TabletDemoMessage(
            id = "adobe",
            sender = "Adobe",
            address = "no-reply@adobe.com",
            subject = "Your Creative Cloud subscription renews in 7 days",
            snippet = "Your All Apps plan renews on 20 October. Here is what to expect.",
            body = "Hi Alex,\n\n" +
                "Your Adobe Creative Cloud All Apps plan renews on 20 October 2026.\n\n" +
                "Plan: Creative Cloud All Apps (100 GB storage)\n" +
                "Amount: Rs. 4,230.30 per month\n\n" +
                "Local demo. This is not a live subscription.",
            timeLabel = "8:42 AM",
            unread = true,
            starred = false,
            folder = TabletMailFolder.Promotional,
        ),
        TabletDemoMessage(
            id = "netflix",
            sender = "Netflix",
            address = "info@mailer.netflix.com",
            subject = "New sign-in to your Netflix account",
            snippet = "A new device signed in to your account. Was this you?",
            body = "Hi Alex,\n\n" +
                "A new device signed in to your Netflix account.\n\n" +
                "Device: POCO X6 Pro 5G (Android)\n" +
                "Location: Kochi, Kerala, India\n" +
                "Time: Today, 9:14 AM\n\n" +
                "Local demo. No live account was checked.",
            timeLabel = "9:14 AM",
            unread = true,
            starred = false,
            folder = TabletMailFolder.Primary,
        ),
        TabletDemoMessage(
            id = "roadmap",
            sender = "ClickUp",
            address = "notifications@clickup.com",
            subject = "Task assigned: Review Q3 roadmap",
            snippet = "Priya assigned you a task in the Mailstack workspace.",
            body = "Hi Alex,\n\n" +
                "Priya Nair assigned you a task in the Mailstack workspace.\n\n" +
                "Task: Review Q3 roadmap\n" +
                "Due: Friday, 17 October\n" +
                "Priority: High\n" +
                "Status: To do\n\n" +
                "Local demo. Nothing was created in a real task list.",
            timeLabel = "8:20 AM",
            unread = true,
            starred = true,
            folder = TabletMailFolder.Primary,
        ),
        TabletDemoMessage(
            id = "github",
            sender = "GitHub",
            address = "notifications@github.com",
            subject = "A pull request was merged: Fix login redirect loop",
            snippet = "priya-dev merged 1 commit into main from fix/login-redirect.",
            body = "priya-dev merged 1 commit into main from fix/login-redirect.\n\n" +
                "Fix login redirect loop (#142)\n" +
                "The sample note says the OAuth state was dropped on a cold start, " +
                "and the demo fix stores that state before the browser opens.\n\n" +
                "Local demo. This pull request is not live on GitHub.",
            timeLabel = "7:40 AM",
            unread = false,
            starred = false,
            folder = TabletMailFolder.Social,
        ),
        TabletDemoMessage(
            id = "ananya",
            sender = "Ananya Sharma",
            address = "ananya.sharma@example.com",
            subject = "Ananya shared a design with you: Q3 Launch Deck",
            snippet = "Ananya Sharma shared the design \"Q3 Launch Deck\" with you.",
            body = "Hi Alex,\n\n" +
                "Ananya Sharma shared a design with you.\n\n" +
                "Design: Q3 Launch Deck\n" +
                "Access: Can edit\n\n" +
                "Local demo. The design is not a live file.",
            timeLabel = "Yesterday",
            unread = true,
            starred = false,
            folder = TabletMailFolder.Primary,
        ),
        TabletDemoMessage(
            id = "amazon",
            sender = "Amazon.in",
            address = "shipment-tracking@amazon.in",
            subject = "Your Amazon order has been delivered",
            snippet = "Your package was delivered at 4:32 PM.",
            body = "Hi Alex,\n\n" +
                "Your package from order #402-9184763-2938471 was delivered at 4:32 PM.\n\n" +
                "Delivered to: Alex Reader, Kochi\n" +
                "Item: Anker 65W USB-C GaN Charger (Black)\n\n" +
                "Local demo. This is not a live order.",
            timeLabel = "Yesterday",
            unread = true,
            starred = false,
            folder = TabletMailFolder.Primary,
        ),
        TabletDemoMessage(
            id = "spam",
            sender = "International Lottery Board",
            address = "winner@lottery-wins.example",
            subject = "CONGRATULATIONS! You have won the lottery",
            snippet = "Local demo spam. This is not a real prize.",
            body = "This is local demo spam from the sample mailbox lottery subject.\n\n" +
                "It is not a real prize. Nothing was sent, and nothing should be claimed.",
            timeLabel = "Fri",
            unread = false,
            starred = false,
            folder = TabletMailFolder.Spam,
        ),
        TabletDemoMessage(
            id = "spotify",
            sender = "Spotify",
            address = "no-reply@spotify.com",
            subject = "Your receipt from Spotify",
            snippet = "Thanks for your Premium subscription payment of Rs. 119.00.",
            body = "Hi Alex,\n\n" +
                "Thanks for your payment. Here is your receipt.\n\n" +
                "Plan: Premium Individual\n" +
                "Amount: Rs. 119.00\n" +
                "Billing date: 1 October 2026\n\n" +
                "Local demo. No payment was made.",
            timeLabel = "Oct 9",
            unread = false,
            starred = false,
            folder = TabletMailFolder.Promotional,
        ),
    )
}

enum class TabletMailFolder(val label: String) {
    Primary("Primary"),
    Promotional("Promotional"),
    Social("Social"),
    Spam("Spam"),
    ;

    companion object {
        /** Chips inside the list pane. Spam stays a demo message, not a chip. */
        val chips: List<TabletMailFolder> = listOf(Primary, Promotional, Social)
    }
}

data class TabletDemoMessage(
    val id: String,
    val sender: String,
    val address: String,
    val subject: String,
    val snippet: String,
    val body: String,
    val timeLabel: String,
    val unread: Boolean,
    val starred: Boolean,
    val folder: TabletMailFolder,
) {
    val initial: String
        get() = sender.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
