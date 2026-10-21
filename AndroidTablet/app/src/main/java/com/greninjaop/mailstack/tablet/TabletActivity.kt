package com.greninjaop.mailstack.tablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.greninjaop.mailstack.tablet.theme.TabletTheme

/**
 * Tablet window. Compose presentation only — no database, OAuth, or sync.
 */
class TabletActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TabletTheme {
                // Agent 2 owns TabletShell.kt.
                com.greninjaop.mailstack.tablet.shell.TabletShell()
            }
        }
    }
}
