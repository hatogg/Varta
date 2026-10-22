package ua.x44to.varta.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.x44to.varta.data.L
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.AppReport
import ua.x44to.varta.scan.Flag
import ua.x44to.varta.scan.Level
import ua.x44to.varta.scan.Risk
import ua.x44to.varta.scan.Scan

enum class AppFilter { RISKY, SIDELOADED, ALL }

/** Список програм із тим, що вони вміють. Спершу найризикованіші. */
@Composable
fun AppsScreen(pad: PaddingValues, filter: AppFilter, onFilter: (AppFilter) -> Unit, onSelect: (Sel) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var system by rememberSaveable { mutableStateOf(false) }
    val r = Scan.result
    if (r == null) {
        Column(Modifier.fillMaxSize().padding(top = pad.calculateTopPadding()).padding(24.dp)) {
            Text(t("Спершу перевірте телефон", "Check the phone first"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                t("Тут буде список програм: що кожна вміє і звідки встановлена.", "Here you'll see every app: what it can do and where it came from."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (!Scan.running) Button(onClick = { Scan.start(ctx, scope) }) { Text(t("Перевірити телефон", "Check the phone")) }
        }
        return
    }
    val list = r.apps.asSequence()
        .filter { system || !it.system || it.ioc != null }
        .filter {
            when (filter) {
                AppFilter.RISKY -> it.level != Level.OK || it.ioc != null
                AppFilter.SIDELOADED -> Risk.sideloaded(it.install.source)
                AppFilter.ALL -> true
            }
        }
        .sortedWith(compareBy<AppReport> { it.level.ordinal }.thenByDescending { it.score }.thenBy { it.label.lowercase() })
        .toList()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 16.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(filter == AppFilter.RISKY, { onFilter(AppFilter.RISKY) }, { Text(t("Ризиковані", "Risky")) })
                FilterChip(filter == AppFilter.SIDELOADED, { onFilter(AppFilter.SIDELOADED) }, { Text(t("Не з магазину", "Not from a store")) })
                FilterChip(filter == AppFilter.ALL, { onFilter(AppFilter.ALL) }, { Text(t("Усі", "All")) })
                FilterChip(system, { system = !system }, { Text(t("Системні", "System")) })
            }
        }
        item {
            Text(
                L.apps(list.size).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        if (list.isEmpty()) item {
            Text(
                if (filter == AppFilter.RISKY) t("Ризикованих програм немає.", "No risky apps.") else t("Нічого не знайдено.", "Nothing here."),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp),
            )
        }
        items(list, key = { it.pkg }) { a -> AppRow(a) { onSelect(Sel.App(a.pkg)) } }
    }
}

@Composable
private fun AppRow(a: AppReport, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(a.pkg)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(a.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val line = when {
                    a.ioc != null -> Texts.iocTitle(a.ioc)
                    a.trusted -> t("Довірена", "Trusted")
                    else -> Texts.source(a.install.source, null, null)
                }
                Text(
                    line, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (a.ioc != null) LevelColors.fg(a.level) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val tags = topFlags(a.flags - Flag.SIDELOADED)
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Tags(tags.map { Texts.flagTitle(it) })
                }
            }
            Spacer(Modifier.width(8.dp))
            LevelDot(a.level)
        }
    }
}
