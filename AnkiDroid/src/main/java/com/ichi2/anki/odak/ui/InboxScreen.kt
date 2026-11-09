package com.ichi2.anki.odak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.ichi2.anki.R
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ichi2.anki.odak.data.Catalog
import com.ichi2.anki.odak.data.Repo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

/** 3 · Toplu teslim edilen bildirimler, uygulamaya göre gruplu. */
@Composable
fun InboxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val data by Repo.data.collectAsState()
    val groups = data.batchInbox.groupBy { it.pkg }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_baseline_arrow_back_24), contentDescription = "Geri") }
            Text("Toplu bildirimler", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        if (groups.isEmpty()) {
            Muted("Bekleyen bildirim yok. Bir sonraki toplu saatte burada görünecekler.")
        }
        groups.forEach { (pkg, list) ->
            val label = Catalog.find(pkg)?.label ?: pkg
            Panel {
                Eyebrow("$label · ${list.size}")
                Spacer(Modifier.height(8.dp))
                list.takeLast(8).reversed().forEach { n ->
                    val t = Instant.ofEpochMilli(n.ts).atZone(ZoneId.systemDefault()).format(timeFmt)
                    Text(
                        listOf(n.title, n.text).filter { it.isNotBlank() }.joinToString(": "),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Muted(t)
                    Spacer(Modifier.height(6.dp))
                }
                if (list.size > 8) Muted("+${list.size - 8} bildirim daha")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        Repo.clearInbox(pkg)
                        Perms.launchApp(context, pkg)
                    }) { Text("Uygulamayı aç") }
                    OutlinedButton(onClick = { Repo.clearInbox(pkg) }) { Text("Temizle") }
                }
            }
        }
        if (groups.isNotEmpty()) {
            OutlinedButton(onClick = { Repo.clearInbox() }) { Text("Hepsini temizle") }
        }
    }
}
