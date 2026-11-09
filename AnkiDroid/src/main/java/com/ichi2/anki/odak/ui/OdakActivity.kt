/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.ichi2.anki.odak.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ichi2.anki.R
import com.ichi2.anki.odak.OdakHooks
import com.ichi2.anki.odak.data.Repo
import com.ichi2.anki.odak.work.Notifs

/** Odak'ın kendi ekranları: Bugün · Ritim · Rapor · Ayarlar. AnkiDroid menüsünden açılır. */
class OdakActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        Repo.init(applicationContext)
        handleOpen(intent)
        setContent {
            OdakTheme {
                val data by Repo.data.collectAsState()
                if (!data.settings.onboarded) OnboardingScreen() else OdakShell()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpen(intent)
    }

    override fun onResume() {
        super.onResume()
        UiEvents.resumed.value = UiEvents.resumed.value + 1
    }

    private fun handleOpen(i: Intent?) {
        if (i == null) return
        val target = i.getStringExtra(Notifs.EXTRA_OPEN) ?: return
        UiEvents.openRequest.value = target
        i.removeExtra(Notifs.EXTRA_OPEN)
    }
}

private enum class OdakTab(val label: String, val icon: Int) {
    Today("Bugün", R.drawable.home_icon),
    Rhythm("Ritim", R.drawable.calendar_date_range),
    Report("Rapor", R.drawable.ic_bar_chart_black),
    Settings("Ayarlar", R.drawable.ic_settings_black),
}

private enum class Overlay { Srbai, Inbox }

@Composable
private fun OdakShell() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(OdakTab.Today) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }

    val openRequest by UiEvents.openRequest.collectAsState()
    LaunchedEffect(openRequest) {
        if (openRequest == Notifs.OPEN_INBOX) {
            overlay = Overlay.Inbox
            UiEvents.openRequest.value = null
        }
    }

    BackHandler(enabled = overlay != null || tab != OdakTab.Today) {
        if (overlay != null) overlay = null else tab = OdakTab.Today
    }

    Scaffold(
        containerColor = OdakColors.Paper,
        bottomBar = {
            if (overlay == null) {
                NavigationBar(containerColor = OdakColors.Card) {
                    OdakTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(painterResource(t.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = OdakColors.GreenSoft),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (overlay) {
                Overlay.Srbai -> SrbaiScreen(onDone = { overlay = null })
                Overlay.Inbox -> InboxScreen(onBack = { overlay = null })
                null -> when (tab) {
                    OdakTab.Today -> HomeScreen(
                        onStudy = { context.startActivity(OdakHooks.studyIntent(context)) },
                        onSrbai = { overlay = Overlay.Srbai },
                        onDecks = { context.startActivity(OdakHooks.decksIntent(context)) },
                        onInbox = { overlay = Overlay.Inbox },
                    )
                    OdakTab.Rhythm -> RhythmScreen(onSrbai = { overlay = Overlay.Srbai })
                    OdakTab.Report -> ReportScreen()
                    OdakTab.Settings -> SettingsScreen()
                }
            }
        }
    }
}
