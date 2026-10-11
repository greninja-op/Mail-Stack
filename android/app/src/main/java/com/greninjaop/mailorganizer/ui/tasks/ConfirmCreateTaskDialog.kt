package com.greninjaop.mailorganizer.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.greninjaop.mailorganizer.data.tasks.TaskProposal
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Explicit confirmation UI for creating a Google Task (Phase 16 §24).
 *
 * Shows exactly what will happen before anything touches the API:
 * the task title, due date, destination list, and the source mail.
 * The confirm button names the external effect ("Add Task") — never a
 * vague "Okay"/"Done".
 *
 * Presentational only: the caller builds the [TaskProposal] (via
 * `buildTaskProposal`), shows this dialog, and on confirmation calls
 * `ReviewActionUseCase.confirm(...)` — the Action Engine owns the
 * proposal → review → confirmation → execution → verification flow.
 * Not yet wired into navigation (parent wires it).
 */
@Composable
fun ConfirmCreateTaskDialog(
    proposal: TaskProposal,
    /** Human label for the source, e.g. "Career email from Acme". */
    sourceLabel: String,
    isCreating: Boolean,
    /** Error from a failed attempt; null when idle. Shown honestly. */
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Add to Google Tasks?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
            ) {
                Text(proposal.title, style = MaterialTheme.typography.titleSmall)
                proposal.dueEpochMs?.let { due ->
                    Text(
                        "Due: ${MailFormatting.relativeTime(due, System.currentTimeMillis())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Task list: ${proposal.taskListName ?: "My Tasks"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Source: $sourceLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "This creates a task in your Google account. " +
                        "Mailstack never creates tasks without your confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isCreating,
                modifier = Modifier.semantics {
                    contentDescription = "Add Task. Creates a task in Google Tasks."
                },
            ) {
                Text(if (isCreating) "Adding…" else "Add Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating,
            ) {
                Text("Cancel")
            }
        },
    )
}
