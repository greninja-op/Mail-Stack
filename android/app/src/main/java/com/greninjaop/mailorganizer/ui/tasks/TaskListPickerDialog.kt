package com.greninjaop.mailorganizer.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.greninjaop.mailorganizer.data.tasks.DEFAULT_TASK_LIST_ID
import com.greninjaop.mailorganizer.data.tasks.GoogleTaskList
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Destination task-list picker (Phase 16 §11–§12).
 *
 * Shows list *names* — never raw API ids as the primary label. The
 * user's default list (`@default`) is labeled as such; no list is
 * pre-selected by API order — [selectedListId] is the user's stored
 * choice (or `@default` when they never chose).
 *
 * Presentational only: the caller loads [taskLists], persists the
 * choice via `TasksRepository.setSelectedTaskListId`, and dismisses.
 * Not yet wired into navigation (parent wires it).
 */
@Composable
fun TaskListPickerDialog(
    taskLists: List<GoogleTaskList>,
    selectedListId: String,
    onSelect: (GoogleTaskList) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose task list") },
        text = {
            if (taskLists.isEmpty()) {
                Text(
                    "No task lists found in your Google account.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
                ) {
                    items(taskLists, key = { it.id }) { list ->
                        val selected = list.id == selectedListId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(list) }
                                .semantics {
                                    contentDescription =
                                        "${list.displayTitle}" +
                                            if (list.id == DEFAULT_TASK_LIST_ID) ", your default list" else "" +
                                            if (selected) ", selected" else ""
                                }
                                .padding(vertical = MoSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MoSpacing.sm),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelect(list) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    list.displayTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (list.id == DEFAULT_TASK_LIST_ID) {
                                    Text(
                                        "Your default list",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
