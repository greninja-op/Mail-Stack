package com.greninjaop.mailorganizer.ui.mail

import com.greninjaop.mailorganizer.core.email.AttachmentMeta
import com.greninjaop.mailorganizer.core.email.HtmlSanitizer
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.data.local.ThreadRecord
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.data.repository.MailRepository
import kotlinx.coroutines.flow.first

/**
 * Demo inbox for a fresh install (Phase 6, expanded 2026-10-13).
 *
 * On a fresh install with no accounts, the inbox seeds a realistic demo
 * mailbox — mails from many real-world companies with full bodies — so the
 * app reads like a real mailbox and the body viewer is verifiable without
 * a live Gmail sync. Rules:
 *
 * - Every fixture id starts with `fixture-` ([FIXTURE_ID_PREFIX]); fixture
 *   rows are trivially distinguishable from real synced rows in the DB.
 * - Fixture `gmailMessageId`s are null: these rows were never synced from
 *   Gmail, and nothing pretends otherwise.
 * - [seedIfEmpty] only runs when the database has **zero** accounts — it
 *   never touches real user data.
 * - Fixture HTML is passed through [HtmlSanitizer], exactly like production
 *   mail, so the viewer exercises the real safe-rendering path.
 * - Gmail labels mirror what Gmail would assign (INBOX / CATEGORY_SOCIAL /
 *   CATEGORY_PROMOTIONS / SPAM), so every mailbox destination populates.
 *
 * Also covers the phase §49/§50 test cases: normal, long subject, HTML
 * (with a hostile `javascript:` link that must NOT become clickable, and a
 * remote image that must NOT load), attachment metadata, Unicode
 * (Malayalam + emoji), missing fields, a multi-message thread, and a
 * starred message.
 */
class SampleMailboxSeeder(
    private val accounts: AccountRepository,
    private val mail: MailRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    companion object {
        // Single source of truth for the prefix lives on AccountRecord
        // (the sync engine's demo-account check reads it from there).
        const val FIXTURE_ID_PREFIX = AccountRecord.FIXTURE_ID_PREFIX

        /** True for fixture account ids — drives the "Sample data" UI banner. */
        fun isFixtureAccount(accountId: String): Boolean =
            accountId.startsWith(FIXTURE_ID_PREFIX)

        const val ACCT_ALEX = "${FIXTURE_ID_PREFIX}acct-alex"
        const val ACCT_PRIYA = "${FIXTURE_ID_PREFIX}acct-priya"
    }

    /**
     * Seeds the fixture mailbox. Returns true when seeding happened.
     * No-op when any account already exists (real or fixture).
     */
    suspend fun seedIfEmpty(): Boolean {
        if (accounts.observeAll().first().isNotEmpty()) return false
        val now = clock()

        accounts.upsert(
            AccountRecord(
                accountId = ACCT_ALEX,
                emailAddress = "alex.reader@example.com",
                displayName = "Alex Reader",
                createdAtEpochMs = now,
                connectionState = ConnectionState.DISCONNECTED,
            ),
        )
        accounts.upsert(
            AccountRecord(
                accountId = ACCT_PRIYA,
                emailAddress = "priya.dev@example.com",
                displayName = "Priya Nair",
                createdAtEpochMs = now,
                connectionState = ConnectionState.DISCONNECTED,
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t1",
            accountId = ACCT_ALEX,
            subject = "Your account information",
            messages = listOf(
                msg(
                    id = "m1", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t1",
                    fromName = "Google", fromAddress = "no-reply@accounts.example.com",
                    subject = "Your account information",
                    snippet = "Here's a summary of your recent account activity…",
                    bodyText = "Hi Alex,\n\nHere's a summary of recent activity on your " +
                        "Google Account.\n\n— The Accounts team",
                    timestamp = now - 5 * 60_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t2",
            accountId = ACCT_ALEX,
            subject = "This is an intentionally very long subject line designed to " +
                "exercise ellipsis and line limits in the mail list row rendering " +
                "pipeline without breaking the layout",
            messages = listOf(
                msg(
                    id = "m2", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t2",
                    fromName = "Long Subject Tester", fromAddress = "long@example.com",
                    subject = "This is an intentionally very long subject line designed to " +
                        "exercise ellipsis and line limits in the mail list row rendering " +
                        "pipeline without breaking the layout",
                    snippet = "Body.",
                    bodyText = "Body.",
                    timestamp = now - 2 * 3_600_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t3",
            accountId = ACCT_ALEX,
            subject = "Your weekly digest",
            messages = listOf(
                msg(
                    id = "m3", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t3",
                    fromName = "Weekly Digest", fromAddress = "digest@example.com",
                    subject = "Your weekly digest",
                    snippet = "Hi Alex, here are your highlights…",
                    bodyText = "Hi Alex, here are your highlights: Item one, Item two. " +
                        "Read more online.",
                    bodyHtml = HtmlSanitizer.sanitize(
                        "<h1>This week</h1>" +
                            "<p>Hi Alex, here are your <b>highlights</b>:</p>" +
                            "<ul><li>Item one</li><li>Item <i>two</i></li></ul>" +
                            "<p>Read more <a href=\"https://example.com/digest\">online</a>.</p>" +
                            "<p><a href=\"javascript:alert('xss')\">do not click</a></p>" +
                            "<img src=\"https://example.com/pixel.png\" alt=\"Weekly banner\">",
                    ),
                    attachments = listOf(
                        AttachmentMeta(
                            filename = "invoice.pdf",
                            mimeType = "application/pdf",
                            sizeBytes = 1_258_291,
                            attachmentId = "${FIXTURE_ID_PREFIX}att-1",
                        ),
                    ),
                    timestamp = now - 26 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t4",
            accountId = ACCT_ALEX,
            subject = "വാർത്താക്കുറിപ്പ്",
            messages = listOf(
                msg(
                    id = "m4a", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "പ്രിയ", fromAddress = "priya@example.com",
                    subject = "വാർത്താക്കുറിപ്പ്",
                    snippet = "ഹായ്!",
                    bodyText = "ഹായ്!",
                    timestamp = now - 3 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "m4b", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "Alex Reader", fromAddress = "alex.reader@example.com",
                    subject = "വാർത്താക്കുറിപ്പ്",
                    snippet = "Thanks, Priya! Looks great.",
                    bodyText = "Thanks, Priya! Looks great.",
                    timestamp = now - 3 * 86_400_000 + 3_600_000,
                    unread = false,
                ),
                msg(
                    id = "m4c", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "പ്രിയ", fromAddress = "priya@example.com",
                    subject = "വാർത്താക്കുറിപ്പ്",
                    snippet = "ശരി, നമുക്ക് തുടരാം.",
                    bodyText = "ശരി, നമുക്ക് തുടരാം.",
                    timestamp = now - 2 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "m4d", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t4",
                    fromName = "Priya", fromAddress = "priya@example.com",
                    subject = "വാർത്താക്കുറിപ്പ്",
                    snippet = "Latest update — all green",
                    bodyText = "Latest update — all green",
                    timestamp = now - 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t5",
            accountId = ACCT_ALEX,
            subject = "",
            messages = listOf(
                msg(
                    id = "m5", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t5",
                    fromName = null, fromAddress = "",
                    subject = "",
                    snippet = null,
                    bodyText = null,
                    timestamp = now - 8 * 86_400_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t6",
            accountId = ACCT_ALEX,
            subject = "Quarterly report",
            messages = listOf(
                msg(
                    id = "m6", accountId = ACCT_ALEX, threadId = "${FIXTURE_ID_PREFIX}thread-t6",
                    fromName = "Finance", fromAddress = "finance@example.com",
                    subject = "Quarterly report",
                    snippet = "Attached is the quarterly report.",
                    bodyText = "Attached is the quarterly report.",
                    timestamp = now - 40L * 86_400_000,
                    unread = false,
                    starred = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-t7",
            accountId = ACCT_PRIYA,
            subject = "Sprint planning",
            messages = listOf(
                msg(
                    id = "m7a", accountId = ACCT_PRIYA, threadId = "${FIXTURE_ID_PREFIX}thread-t7",
                    fromName = "Sam", fromAddress = "sam@example.com",
                    subject = "Sprint planning",
                    snippet = "Planning at 10?",
                    bodyText = "Planning at 10?",
                    timestamp = now - 5 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "m7b", accountId = ACCT_PRIYA, threadId = "${FIXTURE_ID_PREFIX}thread-t7",
                    fromName = "Priya Nair", fromAddress = "priya.dev@example.com",
                    subject = "Sprint planning",
                    snippet = "10 works.",
                    bodyText = "10 works.",
                    timestamp = now - 5 * 86_400_000 + 1_800_000,
                    unread = true,
                ),
            ),
        )

        // 2026-10-13: realistic demo inbox — mails from many companies with
        // full bodies, so the app reads like a real mailbox on first launch
        // (and the body viewer is verifiable without a live Gmail sync).
        // Gmail labels mirror what Gmail would assign, so Primary /
        // Promotions / Social / Spam all populate in the demo.
        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-adobe",
            accountId = ACCT_ALEX,
            subject = "Your Creative Cloud subscription renews in 7 days",
            messages = listOf(
                msg(
                    id = "adobe-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-adobe",
                    fromName = "Adobe", fromAddress = "no-reply@adobe.com",
                    subject = "Your Creative Cloud subscription renews in 7 days",
                    snippet = "Your All Apps plan renews on 20 October. Here is what to expect.",
                    bodyText = "Hi Alex,\n\nYour Adobe Creative Cloud All Apps plan renews on 20 October 2026.\n\nPlan: Creative Cloud All Apps (100 GB storage)\nAmount: Rs. 4,230.30 per month\nPayment method: Visa ending in 4242\n\nYou will keep access to Photoshop, Illustrator, Premiere Pro, Acrobat Pro " +
                        "and 20+ other apps. No action is needed — we will charge your saved " +
                        "payment method on the renewal date.\n\nTo change your plan or cancel, visit Account > Plans and payment before " +
                        "20 October.\n\nThanks,\nThe Adobe Team",
                    timestamp = now - 40 * 60_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-amazon",
            accountId = ACCT_ALEX,
            subject = "Your Amazon order has been delivered",
            messages = listOf(
                msg(
                    id = "amz-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-amazon",
                    fromName = "Amazon.in", fromAddress = "shipment-tracking@amazon.in",
                    subject = "Your Amazon order has shipped",
                    snippet = "Order #402-9184763-2938471 is on its way.",
                    bodyText = "Hi Alex,\n\nYour order #402-9184763-2938471 has shipped.\n\nItem: Anker 65W USB-C GaN Charger (Black)\nQuantity: 1\nSeller: Anker India\n\nTrack your package in the Amazon app. Estimated delivery: tomorrow by 9 PM.",
                    timestamp = now - 2 * 86_400_000,
                    unread = false,
                ),
                msg(
                    id = "amz-2", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-amazon",
                    fromName = "Amazon.in", fromAddress = "shipment-tracking@amazon.in",
                    subject = "Your Amazon order has been delivered",
                    snippet = "Your package was delivered at 4:32 PM.",
                    bodyText = "Hi Alex,\n\nYour package from order #402-9184763-2938471 was delivered at 4:32 PM.\n\nDelivered to: Alex Reader, Kochi\n\nIf you have any issue with this delivery, reply to this email or visit " +
                        "Your Orders in the Amazon app within 7 days.\n\nThanks for shopping with us.",
                    timestamp = now - 86_400_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-github-pr",
            accountId = ACCT_ALEX,
            subject = "A pull request was merged: Fix login redirect loop",
            messages = listOf(
                msg(
                    id = "gh-pr", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-github-pr",
                    fromName = "GitHub", fromAddress = "notifications@github.com",
                    subject = "A pull request was merged: Fix login redirect loop",
                    snippet = "priya-dev merged 1 commit into main from fix/login-redirect.",
                    bodyText = "priya-dev merged 1 commit into main from fix/login-redirect in " +
                        "greninja-op/mail-organizer.\n\nFix login redirect loop (#142)\n- Root cause: the OAuth state parameter was dropped on cold start.\n- Fix: persist state in DataStore before launching the browser.\n- Tests: added 3 regression tests, all passing.\n\nView the pull request on GitHub to see the full diff and review comments.",
                    timestamp = now - 5 * 3_600_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-github-sec",
            accountId = ACCT_ALEX,
            subject = "Dependabot: lodash has a high-severity vulnerability",
            messages = listOf(
                msg(
                    id = "gh-sec", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-github-sec",
                    fromName = "GitHub", fromAddress = "notifications@github.com",
                    subject = "Dependabot: lodash has a high-severity vulnerability",
                    snippet = "Dependabot found a vulnerability in lodash in your repository.",
                    bodyText = "Dependabot alert — high severity\n\nRepository: greninja-op/mail-organizer\nPackage: lodash (via package-lock.json)\nPatched version: 4.17.21\n\nSummary: Prototype pollution in lodash allows an attacker to modify object " +
                        "prototypes when merging untrusted input.\n\nA pull request with the fix is ready for review. Merge it to close this alert.",
                    timestamp = now - 7 * 3_600_000,
                    unread = true,
                    starred = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-netflix",
            accountId = ACCT_ALEX,
            subject = "New sign-in to your Netflix account",
            messages = listOf(
                msg(
                    id = "nflx-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-netflix",
                    fromName = "Netflix", fromAddress = "info@mailer.netflix.com",
                    subject = "New sign-in to your Netflix account",
                    snippet = "A new device signed in to your account. Was this you?",
                    bodyText = "Hi Alex,\n\nA new device signed in to your Netflix account.\n\nDevice: POCO X6 Pro 5G (Android)\nLocation: Kochi, Kerala, India\nTime: Today, 9:14 AM\n\nIf this was you, no action is needed. If you do not recognise this sign-in, " +
                        "change your password and sign out all devices from Account > Security.",
                    timestamp = now - 3 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-spotify",
            accountId = ACCT_ALEX,
            subject = "Your receipt from Spotify",
            messages = listOf(
                msg(
                    id = "spt-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-spotify",
                    fromName = "Spotify", fromAddress = "no-reply@spotify.com",
                    subject = "Your receipt from Spotify",
                    snippet = "Thanks for your Premium subscription payment of Rs. 119.00.",
                    bodyText = "Hi Alex,\n\nThanks for your payment. Here is your receipt.\n\nPlan: Premium Individual\nAmount: Rs. 119.00\nBilling date: 1 October 2026\nNext billing date: 1 November 2026\nPayment method: Visa ending in 4242\n\nYour Premium benefits stay active: ad-free music, offline downloads and " +
                        "high-quality audio.",
                    timestamp = now - 12 * 86_400_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-google-sec",
            accountId = ACCT_ALEX,
            subject = "Security alert: new sign-in on Windows",
            messages = listOf(
                msg(
                    id = "gsec-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-google-sec",
                    fromName = "Google", fromAddress = "no-reply@accounts.google.com",
                    subject = "Security alert: new sign-in on Windows",
                    snippet = "Your Google Account was just signed in to from a new Windows device.",
                    bodyText = "Hi Alex,\n\nYour Google Account was just signed in to from a new device.\n\nDevice: Windows PC (Chrome)\nLocation: Kochi, Kerala, India\nTime: Today, 8:02 AM\n\nIf this was you, you can ignore this email. If it was not you, secure your " +
                        "account immediately: review recent activity and change your password.",
                    timestamp = now - 6 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-linkedin-search",
            accountId = ACCT_ALEX,
            subject = "You appeared in 47 searches this week",
            messages = listOf(
                msg(
                    id = "li-s", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-linkedin-search",
                    fromName = "LinkedIn", fromAddress = "notifications-noreply@linkedin.com",
                    subject = "You appeared in 47 searches this week",
                    snippet = "Recruiters and hiring managers searched for people like you.",
                    bodyText = "Hi Alex,\n\nYou appeared in 47 searches this week.\n\nTop search terms: Android developer, Kotlin, Jetpack Compose\nCompanies searching: 12, including 3 in fintech\n\nYour profile is getting noticed. Add your latest project to stand out even more.",
                    timestamp = now - 2 * 86_400_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_SOCIAL"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-linkedin-req",
            accountId = ACCT_ALEX,
            subject = "Ananya Sharma wants to connect",
            messages = listOf(
                msg(
                    id = "li-r", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-linkedin-req",
                    fromName = "LinkedIn", fromAddress = "invitations@linkedin.com",
                    subject = "Ananya Sharma wants to connect",
                    snippet = "Ananya Sharma, Product Designer at Figma, wants to connect with you.",
                    bodyText = "Hi Alex,\n\nAnanya Sharma wants to connect with you on LinkedIn.\n\nAnanya Sharma — Product Designer at Figma\nMutual connections: 14\n\nMessage: \"Hi Alex, I came across your Mailstack project and loved the " +
                        "attention to detail. Would love to connect!\"\n\nAccept the invitation to grow your network.",
                    timestamp = now - 9 * 3_600_000,
                    unread = true,
                    labels = listOf("INBOX", "CATEGORY_SOCIAL"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-youtube",
            accountId = ACCT_ALEX,
            subject = "Fireship uploaded: Kotlin in 100 Seconds",
            messages = listOf(
                msg(
                    id = "yt-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-youtube",
                    fromName = "YouTube", fromAddress = "no-reply@youtube.com",
                    subject = "Fireship uploaded: Kotlin in 100 Seconds",
                    snippet = "A new video from a channel you subscribed to.",
                    bodyText = "Fireship just uploaded a new video:\n\nKotlin in 100 Seconds\nDuration: 2:41\n\nKotlin is a modern language that runs on the JVM, powers Android development " +
                        "and is officially supported by Google. Watch the video to see why " +
                        "developers love it.",
                    timestamp = now - 30 * 3_600_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_SOCIAL"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-reddit",
            accountId = ACCT_ALEX,
            subject = "Your weekly recap: top posts from your communities",
            messages = listOf(
                msg(
                    id = "rd-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-reddit",
                    fromName = "Reddit", fromAddress = "noreply@redditmail.com",
                    subject = "Your weekly recap: top posts from your communities",
                    snippet = "See what happened this week in r/androiddev and r/kotlin.",
                    bodyText = "Hi Alex,\n\nHere is your weekly recap.\n\nr/androiddev — \"Material 3 Expressive: what actually changed\" (2.4k upvotes)\n" +
                        "r/kotlin — \"Kotlin 2.3: what is new\" (1.1k upvotes)\nr/productivity — \"My setup for deep work\" (890 upvotes)\n\nYou received 24 upvotes on your comments this week.",
                    timestamp = now - 4 * 86_400_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_SOCIAL"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-canva",
            accountId = ACCT_ALEX,
            subject = "Ananya shared a design with you: Q3 Launch Deck",
            messages = listOf(
                msg(
                    id = "cv-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-canva",
                    fromName = "Canva", fromAddress = "notify@canva.com",
                    subject = "Ananya shared a design with you: Q3 Launch Deck",
                    snippet = "Ananya Sharma shared the design \"Q3 Launch Deck\" with you.",
                    bodyText = "Hi Alex,\n\nAnanya Sharma shared a design with you.\n\nDesign: Q3 Launch Deck\nAccess: Can edit\n\nOpen it in Canva to review the slides, leave comments, or present it " +
                        "directly from the editor.",
                    timestamp = now - 11 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-dribbble",
            accountId = ACCT_ALEX,
            subject = "Marcus liked your shot \"Mail App — Inbox Concept\"",
            messages = listOf(
                msg(
                    id = "dr-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-dribbble",
                    fromName = "Dribbble", fromAddress = "notifications@dribbble.com",
                    subject = "Marcus liked your shot \"Mail App — Inbox Concept\"",
                    snippet = "Your shot got a new like from Marcus Chen.",
                    bodyText = "Hi Alex,\n\nMarcus Chen liked your shot \"Mail App — Inbox Concept\".\n\nYour shot now has 128 likes and 12 comments. Keep sharing your work — " +
                        "the community is watching.",
                    timestamp = now - 3 * 86_400_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_SOCIAL"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-x-login",
            accountId = ACCT_ALEX,
            subject = "There was a login to your account from a new device",
            messages = listOf(
                msg(
                    id = "x-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-x-login",
                    fromName = "X", fromAddress = "verify@x.com",
                    subject = "There was a login to your account from a new device",
                    snippet = "A new device logged in to your X account.",
                    bodyText = "Hi Alex,\n\nThere was a login to your X account from a new device.\n\nDevice: Chrome on Windows\nLocation: Kochi, India\nTime: Today, 7:48 AM\n\nIf this was you, no action is needed. If not, change your password and " +
                        "review your active sessions.",
                    timestamp = now - 8 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-duolingo",
            accountId = ACCT_ALEX,
            subject = "Your streak is in danger!",
            messages = listOf(
                msg(
                    id = "duo-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-duolingo",
                    fromName = "Duolingo", fromAddress = "hello@duolingo.com",
                    subject = "Your streak is in danger!",
                    snippet = "You have not practised Malayalam today. Keep your 42-day streak alive.",
                    bodyText = "Hi Alex,\n\nYour 42-day Malayalam streak is in danger!\n\nYou have not practised today. Complete one lesson in the next 4 hours to " +
                        "keep your streak alive.\n\nTip: a 5-minute lesson is enough. Your future self will thank you.",
                    timestamp = now - 5 * 3_600_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_PROMOTIONS"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-clickup",
            accountId = ACCT_ALEX,
            subject = "Task assigned: Review Q3 roadmap",
            messages = listOf(
                msg(
                    id = "cu-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-clickup",
                    fromName = "ClickUp", fromAddress = "notifications@clickup.com",
                    subject = "Task assigned: Review Q3 roadmap",
                    snippet = "Priya assigned you a task in the Mailstack workspace.",
                    bodyText = "Hi Alex,\n\nPriya Nair assigned you a task in the Mailstack workspace.\n\nTask: Review Q3 roadmap\nDue: Friday, 17 October\nPriority: High\nStatus: To do\n\nDescription: Review the Q3 roadmap doc and leave comments on the two open " +
                        "questions before the planning call.",
                    timestamp = now - 4 * 3_600_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-aws",
            accountId = ACCT_ALEX,
            subject = "Your AWS bill for September 2026",
            messages = listOf(
                msg(
                    id = "aws-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-aws",
                    fromName = "Amazon Web Services", fromAddress = "no-reply@amazon.com",
                    subject = "Your AWS bill for September 2026",
                    snippet = "Your AWS bill for September is $12.47.",
                    bodyText = "Hello Alex,\n\nYour AWS bill for September 2026 is ready.\n\nTotal: $12.47\nPrevious balance: $0.00\n\nTop services:\n- Amazon EC2: $6.20\n- Amazon S3: $3.11\n- Data transfer: $2.04\n- Other: $1.12\n\nView the full bill in the Billing console.",
                    timestamp = now - 6 * 86_400_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-apple",
            accountId = ACCT_ALEX,
            subject = "Your receipt from Apple",
            messages = listOf(
                msg(
                    id = "apl-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-apple",
                    fromName = "Apple", fromAddress = "no_reply@email.apple.com",
                    subject = "Your receipt from Apple",
                    snippet = "Your purchase of Bear 2 was successful.",
                    bodyText = "Hi Alex,\n\nThank you for your purchase.\n\nItem: Bear 2 — Notes (Annual)\nOrder ID: MH7X2K9PQ1\nDate: 5 October 2026\nTotal: Rs. 1,499.00\n\nThis email confirms your purchase. No action is needed.",
                    timestamp = now - 8 * 86_400_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-bank",
            accountId = ACCT_ALEX,
            subject = "Your credit card statement is ready",
            messages = listOf(
                msg(
                    id = "bnk-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-bank",
                    fromName = "HDFC Bank", fromAddress = "alerts@hdfcbank.net",
                    subject = "Your credit card statement is ready",
                    snippet = "Your HDFC credit card statement for September is ready to view.",
                    bodyText = "Dear Alex,\n\nYour HDFC Bank credit card statement for September 2026 is ready.\n\nTotal amount due: Rs. 18,420.00\nMinimum amount due: Rs. 921.00\nPayment due date: 21 October 2026\n\nPay on time to avoid late fees. View the detailed statement in the app or " +
                        "on NetBanking.",
                    timestamp = now - 2 * 86_400_000,
                    unread = true,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-indigo",
            accountId = ACCT_ALEX,
            subject = "Booking confirmed: Kochi to Bengaluru, 24 Oct",
            messages = listOf(
                msg(
                    id = "ind-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-indigo",
                    fromName = "IndiGo", fromAddress = "noreply@goindigo.in",
                    subject = "Booking confirmed: Kochi to Bengaluru, 24 Oct",
                    snippet = "Your IndiGo booking is confirmed. PNR: X7K9PQ.",
                    bodyText = "Hi Alex,\n\nYour IndiGo booking is confirmed.\n\nPNR: X7K9PQ\nFlight: 6E 482 | Kochi (COK) to Bengaluru (BLR)\nDate: Saturday, 24 October 2026\nDeparture: 08:15 AM | Arrival: 09:25 AM\nPassenger: Alex Reader\nSeat: 14A (Window)\n\nCheck in from 48 hours before departure on the IndiGo app or website. " +
                        "Carry a valid photo ID.",
                    timestamp = now - 5 * 86_400_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-swiggy",
            accountId = ACCT_ALEX,
            subject = "Your order is on its way",
            messages = listOf(
                msg(
                    id = "swg-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-swiggy",
                    fromName = "Swiggy", fromAddress = "noreply@swiggy.in",
                    subject = "Your order is on its way",
                    snippet = "Your biryani is on its way. Arriving in ~25 minutes.",
                    bodyText = "Hi Alex,\n\nYour order is on its way.\n\nRestaurant: Thalassery Biryani House\nItems: Chicken Biryani (1), Raita (1)\nOrder total: Rs. 349.00\nEstimated delivery: 25 minutes\n\nTrack your delivery partner live in the Swiggy app.",
                    timestamp = now - 90 * 60_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_PROMOTIONS"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-flipkart",
            accountId = ACCT_ALEX,
            subject = "Your Flipkart order has been delivered",
            messages = listOf(
                msg(
                    id = "fk-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-flipkart",
                    fromName = "Flipkart", fromAddress = "noreply@flipkart.com",
                    subject = "Your Flipkart order has been delivered",
                    snippet = "Your order OD3391884756123 was delivered.",
                    bodyText = "Hi Alex,\n\nYour Flipkart order OD3391884756123 has been delivered.\n\nItem: boAt Airdopes 141 ANC TWS Earbuds (Black)\nDelivered on: 10 October 2026\n\nEnjoy your purchase. Rate the product in the Flipkart app to help other buyers.",
                    timestamp = now - 3 * 86_400_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_PROMOTIONS"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-openrouter",
            accountId = ACCT_ALEX,
            subject = "Your weekly API usage summary",
            messages = listOf(
                msg(
                    id = "or-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-openrouter",
                    fromName = "OpenRouter", fromAddress = "team@openrouter.ai",
                    subject = "Your weekly API usage summary",
                    snippet = "You used $4.82 of credits this week across 3 models.",
                    bodyText = "Hi Alex,\n\nHere is your weekly OpenRouter usage summary.\n\nSpend: $4.82\nRequests: 1,284\nTop models: claude-sonnet (61%), gpt-5-mini (28%), gemini-flash (11%)\nRemaining credits: $15.18\n\nTop up credits or set a spend limit anytime in Settings > Billing.",
                    timestamp = now - 7 * 86_400_000,
                    unread = false,
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-glasp",
            accountId = ACCT_ALEX,
            subject = "Your weekly highlights are ready",
            messages = listOf(
                msg(
                    id = "gl-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-glasp",
                    fromName = "Glasp", fromAddress = "hello@glasp.co",
                    subject = "Your weekly highlights are ready",
                    snippet = "You highlighted 14 passages this week. Here are the top ones.",
                    bodyText = "Hi Alex,\n\nYou highlighted 14 passages this week. Here are the top ones.\n\n\"Simplicity is the soul of efficiency.\" — from Design Systems Weekly\n" +
                        "\"The best interface is the one you do not notice.\" — from UX Collective\n" +
                        "\"Write code for the reader, not the machine.\" — from The Pragmatic Programmer\n\nOpen Glasp to review all your highlights and export them to your notes.",
                    timestamp = now - 9 * 86_400_000,
                    unread = false,
                    labels = listOf("INBOX", "CATEGORY_SOCIAL"),
                ),
            ),
        )

        seedThread(
            threadId = "${FIXTURE_ID_PREFIX}thread-spam",
            accountId = ACCT_ALEX,
            subject = "CONGRATULATIONS! You have won the lottery",
            messages = listOf(
                msg(
                    id = "spm-1", accountId = ACCT_ALEX,
                    threadId = "${FIXTURE_ID_PREFIX}thread-spam",
                    fromName = "International Lottery Board", fromAddress = "winner@lottery-wins.example",
                    subject = "CONGRATULATIONS! You have won the lottery",
                    snippet = "You have been selected as the lucky winner of $5,000,000.",
                    bodyText = "Dear Lucky Winner,\n\nCongratulations! Your email address was selected as the winner of " +
                        "$5,000,000 in the International Lottery Draw.\n\nTo claim your prize, reply with your bank details.\n\n(This is a demo spam message seeded to exercise the Spam destination.)",
                    timestamp = now - 4 * 86_400_000,
                    unread = false,
                    labels = listOf("SPAM"),
                ),
            ),
        )

        return true
    }

    private fun msg(
        id: String,
        accountId: String,
        threadId: String,
        fromName: String?,
        fromAddress: String,
        subject: String,
        snippet: String?,
        bodyText: String?,
        bodyHtml: String? = null,
        attachments: List<AttachmentMeta> = emptyList(),
        timestamp: Long,
        unread: Boolean,
        starred: Boolean = false,
        labels: List<String> = listOf("INBOX"),
    ): MessageRecord = MessageRecord(
        messageId = "$FIXTURE_ID_PREFIX" + "msg-$id",
        gmailMessageId = null, // fixtures were never synced — honest null
        threadId = threadId,
        accountId = accountId,
        fromAddress = fromAddress,
        fromName = fromName,
        subject = subject,
        snippet = snippet,
        bodyText = bodyText,
        bodyHtml = bodyHtml,
        attachments = attachments,
        timestampEpochMs = timestamp,
        unread = unread,
        starred = starred,
        labels = labels,
    )

    private suspend fun seedThread(
        threadId: String,
        accountId: String,
        subject: String,
        messages: List<MessageRecord>,
    ) {
        val latest = messages.maxByOrNull { it.timestampEpochMs }
        val participants = messages
            .mapNotNull { it.fromName?.takeIf { n -> n.isNotBlank() } ?: it.fromAddress.takeIf { a -> a.isNotBlank() } }
            .distinct()
        mail.saveThreadWithMessages(
            ThreadRecord(
                threadId = threadId,
                gmailThreadId = null, // fixtures were never synced
                accountId = accountId,
                subject = subject,
                participantDisplayNames = participants,
                messageCount = messages.size,
                unreadCount = messages.count { it.unread },
                latestMessageId = latest?.messageId,
                latestMessageEpochMs = latest?.timestampEpochMs ?: 0L,
                updatedAtEpochMs = clock(),
            ),
            messages,
        )
    }
}
