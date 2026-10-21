package com.greninjaop.mailstack.tablet.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Home dashboard for the tablet workspace.
 *
 * Sections sit beside each other once each one has room. A narrow width
 * stacks them. Rows stay flat on the background — cards are not used here.
 */
@Composable
fun TabletHomePane() {
    val colors = workspaceColors()
    val messages = WorkspaceDemo.messages
    val sections = homeSections(messages)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (sections.isEmpty()) {
            WorkspaceEmpty(
                title = "Nothing here",
                message = "This mailbox has no demo messages.",
                colors = colors,
            )
        } else {
            val columns = workspaceColumns(
                availableWidth = maxWidth - WorkspacePanePadding * 2,
                itemCount = sections.size,
                minItem = 280.dp,
                gap = 16.dp,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WorkspacePanePadding),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HomeHeader(colors)
                sections.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        row.forEach { section ->
                            HomeSection(
                                title = section.title,
                                rows = section.rows,
                                colors = colors,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top),
                            )
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(colors: WorkspaceColors) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = WorkspaceDemo.accountName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
        Text(
            text = WorkspaceDemo.accountEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = "This mailbox is a local demo, not a connected Gmail account.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeSection(
    title: String,
    rows: List<WorkspaceDemo.Message>,
    colors: WorkspaceColors,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = rows.size.toString(),
                modifier = Modifier.semantics {
                    contentDescription = workspaceMessageCount(rows.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        rows.forEach { message ->
            WorkspaceMessageRow(message = message, colors = colors)
        }
    }
}

private data class HomeSectionModel(
    val title: String,
    val rows: List<WorkspaceDemo.Message>,
)

private fun homeSections(messages: List<WorkspaceDemo.Message>): List<HomeSectionModel> {
    if (messages.isEmpty()) return emptyList()
    val attention = messages.filter { it.unread && it.bucket == WorkspaceDemo.Bucket.Primary }
    val starred = messages.filter { it.starred }
    return listOfNotNull(
        attention.takeIf { it.isNotEmpty() }?.let { HomeSectionModel("Needs attention", it) },
        starred.takeIf { it.isNotEmpty() }?.let { HomeSectionModel("Starred", it) },
        HomeSectionModel("Recent", messages),
    )
}

/**
 * Local demo mailbox. Subjects match the shared sample mailbox.
 * This is not a Gmail account and not a sync result.
 */
internal object WorkspaceDemo {
    const val accountName = "Demo inbox"
    const val accountEmail = "demo@mailstack.local"
    const val nothingSent = "From the local demo mailbox. Nothing is sent."

    enum class Bucket { Primary, Promotional, Social, Spam }

    data class Message(
        val id: String,
        val sender: String,
        val address: String,
        val subject: String,
        val snippet: String,
        val body: String,
        val timeLabel: String,
        val unread: Boolean,
        val starred: Boolean,
        val bucket: Bucket,
        val company: String?,
    ) {
        val initial: String
            get() = sender.firstOrNull()?.toString()?.uppercase() ?: "?"
    }

    val messages: List<Message> = listOf(
        Message(
            id = "m1",
            sender = "Amazon",
            address = "auto-confirm@amazon.com",
            subject = "Your Amazon order has been delivered",
            snippet = "Your package was left at the door.",
            body = "Your Amazon order has been delivered. This is local demo mail, not a live order.",
            timeLabel = "9:14 AM",
            unread = true,
            starred = false,
            bucket = Bucket.Primary,
            company = "Amazon",
        ),
        Message(
            id = "m2",
            sender = "Ananya Sharma",
            address = "ananya@example.com",
            subject = "Ananya shared a design with you: Q3 Launch Deck",
            snippet = "Take a look when you have a minute.",
            body = "Ananya shared a design with you. Local demo only.",
            timeLabel = "Yesterday",
            unread = false,
            starred = true,
            bucket = Bucket.Primary,
            company = null,
        ),
        Message(
            id = "m3",
            sender = "Netflix",
            address = "info@netflix.com",
            subject = "New sign-in to your Netflix account",
            snippet = "A new device signed in.",
            body = "Security-style demo notice. No live account was checked.",
            timeLabel = "Mon",
            unread = true,
            starred = false,
            bucket = Bucket.Primary,
            company = "Netflix",
        ),
        Message(
            id = "m4",
            sender = "GitHub",
            address = "noreply@github.com",
            subject = "A pull request was merged: Fix login redirect loop",
            snippet = "The pull request is now merged.",
            body = "Demo notification from the sample mailbox.",
            timeLabel = "Sun",
            unread = false,
            starred = false,
            bucket = Bucket.Social,
            company = "GitHub",
        ),
        Message(
            id = "m5",
            sender = "Hiring Desk",
            address = "jobs@example.com",
            subject = "Task assigned: Review Q3 roadmap",
            snippet = "Please review the roadmap this week.",
            body = "A demo task mention. Nothing was created in a real task list.",
            timeLabel = "8:02 AM",
            unread = true,
            starred = false,
            bucket = Bucket.Primary,
            company = null,
        ),
        Message(
            id = "m6",
            sender = "Spotify",
            address = "no-reply@spotify.com",
            subject = "Your receipt from Spotify",
            snippet = "Thanks for your payment.",
            body = "Demo receipt. No payment was made.",
            timeLabel = "Tue",
            unread = false,
            starred = false,
            bucket = Bucket.Promotional,
            company = "Spotify",
        ),
        Message(
            id = "m7",
            sender = "Adobe",
            address = "mail@adobe.com",
            subject = "Your Creative Cloud subscription renews in 7 days",
            snippet = "Your plan renews soon.",
            body = "Demo renewal notice. Not a live subscription.",
            timeLabel = "Tue",
            unread = true,
            starred = false,
            bucket = Bucket.Promotional,
            company = "Adobe",
        ),
        Message(
            id = "m8",
            sender = "Fireship",
            address = "noreply@youtube.com",
            subject = "Fireship uploaded: Kotlin in 100 Seconds",
            snippet = "A new video is up.",
            body = "Demo social notification.",
            timeLabel = "Sat",
            unread = false,
            starred = false,
            bucket = Bucket.Social,
            company = "YouTube",
        ),
        Message(
            id = "m9",
            sender = "Unknown",
            address = "prize@example.invalid",
            subject = "CONGRATULATIONS! You have won the lottery",
            snippet = "This looks like spam.",
            body = "Demo spam. Do not treat this as a real message.",
            timeLabel = "Fri",
            unread = true,
            starred = false,
            bucket = Bucket.Spam,
            company = null,
        ),
        Message(
            id = "m10",
            sender = "AWS",
            address = "no-reply@amazonaws.com",
            subject = "Your AWS bill for September 2026",
            snippet = "Your monthly bill is ready.",
            body = "Demo bill. No AWS account is connected.",
            timeLabel = "Oct 2",
            unread = false,
            starred = false,
            bucket = Bucket.Primary,
            company = "Amazon Web Services",
        ),
    )
}

internal data class WorkspaceColors(
    val background: Color,
    val card: Color,
    val brand: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val outline: Color,
    val dark: Boolean,
)

internal val WorkspaceCardShape = RoundedCornerShape(28.dp)
internal val WorkspacePanePadding = 16.dp
internal val WorkspaceSideBySideMin = 640.dp

@Composable
internal fun workspaceColors(): WorkspaceColors {
    return if (isSystemInDarkTheme()) {
        WorkspaceColors(
            background = Color(0xFF131120),
            card = Color(0xFF262340),
            brand = Color(0xFFB9A8FF),
            onSurface = Color(0xFFF0EDFB),
            onSurfaceVariant = Color(0xFFCBC6DE),
            secondaryContainer = Color(0xFF4D4096),
            onSecondaryContainer = Color(0xFFE4E0FF),
            outline = Color(0xFF48445C),
            dark = true,
        )
    } else {
        WorkspaceColors(
            background = Color(0xFFF6F3FF),
            card = Color(0xFFEFEAFB),
            brand = Color(0xFF5238E8),
            onSurface = Color(0xFF1C1A26),
            onSurfaceVariant = Color(0xFF48455A),
            secondaryContainer = Color(0xFFE4E0FF),
            onSecondaryContainer = Color(0xFF241A5E),
            outline = Color(0xFFCBC5DE),
            dark = false,
        )
    }
}

/**
 * How many items fit beside each other. One column until each item can
 * keep [minItem] of width, so a pane never becomes one stretched column
 * on a wide screen.
 */
internal fun workspaceColumns(
    availableWidth: Dp,
    itemCount: Int,
    minItem: Dp,
    gap: Dp,
): Int {
    if (itemCount <= 1 || availableWidth <= 0.dp) return 1
    var columns = itemCount
    while (columns > 1) {
        val needed = minItem * columns + gap * (columns - 1)
        if (availableWidth >= needed) return columns
        columns -= 1
    }
    return 1
}

internal fun workspaceMessageCount(count: Int): String =
    if (count == 1) "1 message" else "$count messages"

internal fun workspaceSubjectHas(subject: String, needle: String): Boolean =
    subject.contains(needle, ignoreCase = true)

@Composable
internal fun WorkspaceMessageRow(
    message: WorkspaceDemo.Message,
    colors: WorkspaceColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message.initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSecondaryContainer,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.sender,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (message.unread) FontWeight.SemiBold else FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (message.unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (message.unread) colors.brand else colors.onSurfaceVariant,
                )
            }
            Text(
                text = message.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (message.unread) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = message.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (message.starred) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Starred",
                tint = colors.brand,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
internal fun WorkspaceEmpty(
    title: String,
    message: String,
    colors: WorkspaceColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}
