package com.greninjaop.mailorganizer.tablet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.greninjaop.mailorganizer.tablet.ui.TabletApp
import com.greninjaop.mailorganizer.tablet.ui.theme.MailstackTabletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MailstackTabletTheme {
                TabletApp()
            }
        }
    }
}
