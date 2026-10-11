package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.greninjaop.mailorganizer.domain.write.GmailWriteOperation
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Explicit confirmation dialog for Gmail write actions (Phase 22, phase §15).
 *
 * Every write shows: what will happen, which message, which account, and
 * reversibility. Higher-risk actions (archive, trash) get stronger copy
 * (phase §16). Confirming is the *only* path that sets the use case's
 * confirmation flag — there is no other way to execute a write.
 */
@Composable
fun GmailWriteConfirmDialog(
    pending: PendingGmailWrite,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = pending.message
    val subject = MailFormatting.subjectDisplay(message.subject).ifBlank { "(no subject)" }
    val sender = MailFormatting.senderDisplay(message.fromName, message.fromAddress)
    val copy = writeDialogCopy(pending.operation)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                Text(
                    text = "“$subject”",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "From: $sender",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                pending.accountEmail?.let { email ->
                    Text(
                        text = "Account: $email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = copy.whatHappens,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = copy.reversibility,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(copy.confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Incremental-authorization upgrade prompt (phase §9, §10, §34).
 *
 * Shown when a write fails with HTTP 403: the account's grant lacks
 * gmail.modify. Explains *what* the permission allows and *why* before
 * offering to launch the upgrade flow — never a vague "we need more
 * permissions".
 */
@Composable
fun WritePermissionUpgradeDialog(
    accountEmail: String?,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow Gmail changes?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                if (accountEmail != null) {
                    Text(
                        text = "Account: $accountEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Mailstack needs permission to modify your Gmail " +
                        "before it can mark messages read, archive them, or " +
                        "move them to trash.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "It will only ever change mail when you explicitly " +
                        "confirm an action — never automatically, never in the " +
                        "background. Sending mail and permanent deletion are " +
                        "not included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGrant) {
                Text("Allow Gmail changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
    )
}

private data class WriteCopy(
    val title: String,
    val whatHappens: String,
    val reversibility: String,
    val confirmLabel: String,
)

/** Per-operation dialog copy (phase §16: stronger copy for higher-risk). */
private fun writeDialogCopy(operation: GmailWriteOperation): WriteCopy = when (operation) {
    GmailWriteOperation.MARK_READ -> WriteCopy(
        title = "Mark as read?",
        whatHappens = "This message will be marked as read in Gmail.",
        reversibility = "You can mark it unread again any time.",
        confirmLabel = "Mark as read",
    )
    GmailWriteOperation.MARK_UNREAD -> WriteCopy(
        title = "Mark as unread?",
        whatHappens = "This message will be marked as unread in Gmail.",
        reversibility = "You can mark it read again any time.",
        confirmLabel = "Mark as unread",
    )
    GmailWriteOperation.STAR -> WriteCopy(
        title = "Star this message?",
        whatHappens = "A star will be added to this message in Gmail.",
        reversibility = "You can remove the star any time.",
        confirmLabel = "Star",
    )
    GmailWriteOperation.UNSTAR -> WriteCopy(
        title = "Remove star?",
        whatHappens = "The star will be removed from this message in Gmail.",
        reversibility = "You can star it again any time.",
        confirmLabel = "Remove star",
    )
    GmailWriteOperation.ARCHIVE -> WriteCopy(
        title = "Archive this message?",
        whatHappens = "It will be removed from your inbox, but stays in " +
            "Gmail — find it in All Mail or search.",
        reversibility = "This is not delete. You can move it back to the " +
            "inbox from Gmail.",
        confirmLabel = "Archive",
    )
    GmailWriteOperation.TRASH -> WriteCopy(
        title = "Move to trash?",
        whatHappens = "This message goes to Gmail Trash.",
        reversibility = "Trash is emptied automatically after 30 days. You " +
            "can restore it from Trash before then.",
        confirmLabel = "Move to trash",
    )
    GmailWriteOperation.UNTRASH -> WriteCopy(
        title = "Restore from trash?",
        whatHappens = "This message will be removed from Trash in Gmail.",
        reversibility = "You can move it back to trash any time.",
        confirmLabel = "Restore",
    )
    GmailWriteOperation.ADD_LABEL -> WriteCopy(
        title = "Add Gmail label?",
        whatHappens = "The label will be added to this message in Gmail.",
        reversibility = "You can remove the label any time.",
        confirmLabel = "Add label",
    )
    GmailWriteOperation.REMOVE_LABEL -> WriteCopy(
        title = "Remove Gmail label?",
        whatHappens = "The label will be removed from this message in Gmail.",
        reversibility = "The message itself is not deleted.",
        confirmLabel = "Remove label",
    )
}
