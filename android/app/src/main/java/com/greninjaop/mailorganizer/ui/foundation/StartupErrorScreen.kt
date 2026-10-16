package com.greninjaop.mailorganizer.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Shown instead of crashing when [com.greninjaop.mailorganizer.MailOrganizerApp]
 * fails to initialize (crash-repair work, 2026-10-11).
 *
 * Previously an init failure rethrew out of Application.onCreate, producing
 * an instant "app keeps stopping" with no actionable information. This
 * screen shows the actual failure and lets the user copy it, so the real
 * cause can be diagnosed from a device without logcat access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupErrorScreen(
    error: Throwable?,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val details = errorDetails(error)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
        topBar = { TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),title = { Text("Mailstack couldn't start") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "The app ran into a problem while starting. " +
                    "Copy the details below and share them so the cause can be fixed — " +
                    "nothing was lost; your mail data is untouched.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(12.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(details))
                }) {
                    Text("Copy details")
                }
                OutlinedButton(onClick = onRestart) {
                    Text("Restart app")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun errorDetails(error: Throwable?): String {
    if (error == null) return "No error details were captured."
    val firstLine = "${error.javaClass.name}: ${error.message}"
    val frames = error.stackTrace.take(12).joinToString("\n") { "    at $it" }
    val cause = error.cause?.let { "\nCaused by: ${it.javaClass.name}: ${it.message}" } ?: ""
    return "$firstLine\n$frames$cause"
}
