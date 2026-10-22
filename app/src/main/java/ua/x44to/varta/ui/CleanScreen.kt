package ua.x44to.varta.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.x44to.varta.clean.Clean
import ua.x44to.varta.clean.Cleaner
import ua.x44to.varta.clean.Junk
import ua.x44to.varta.clean.JunkItem
import ua.x44to.varta.data.L
import ua.x44to.varta.data.bytes
import ua.x44to.varta.data.t

private fun junkTitle(j: Junk) = when (j) {
    Junk.APP_CACHE -> t("Кеш програм", "App cache")
    Junk.EMPTY_DIRS -> t("Порожні папки", "Empty folders")
    Junk.TEMP -> t("Тимчасові файли й журнали", "Temporary files and logs")
    Junk.THUMBS -> t("Мініатюри", "Thumbnails")
    Junk.LEFTOVERS -> t("Залишки видалених програм", "Leftovers of removed apps")
    Junk.APKS -> t("Файли APK", "APK files")
    Junk.LARGE -> t("Великі файли", "Large files")
}

private fun junkText(j: Junk) = when (j) {
    Junk.APP_CACHE -> t("Тимчасові файли програм. Програми створять їх знову, коли знадобиться.", "Apps' temporary files. Apps recreate them when needed.")
    Junk.EMPTY_DIRS -> t("Лишилися від програм і завантажень.", "Left behind by apps and downloads.")
    Junk.TEMP -> t(".tmp, .log і недокачані файли.", ".tmp, .log and unfinished downloads.")
    Junk.THUMBS -> t("Маленькі копії фото для галереї. Галерея створить їх знову.", "Small copies of photos for the gallery. It recreates them.")
    Junk.LEFTOVERS -> t("Файли програм, яких уже немає на телефоні.", "Files of apps that are no longer installed.")
    Junk.APKS -> t("Інсталятори програм. Позначено ті, що вже встановлені.", "App installers. The already installed ones are selected.")
    Junk.LARGE -> t("Від 100 МБ. Перегляньте самі: нічого не позначено.", "100 MB and more. Review them yourself: nothing is selected.")
}

private fun junkIcon(j: Junk): ImageVector = when (j) {
    Junk.APP_CACHE -> Icons.Rounded.Cached
    Junk.EMPTY_DIRS -> Icons.Rounded.FolderOff
    Junk.TEMP -> Icons.Rounded.Description
    Junk.THUMBS -> Icons.Rounded.Image
    Junk.LEFTOVERS -> Icons.Rounded.DeleteSweep
    Junk.APKS -> Icons.Rounded.Android
    Junk.LARGE -> Icons.Rounded.Movie
}

@Composable
fun CleanScreen(pad: PaddingValues) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick = FileAccess.tick
    val files = remember(tick) { Cleaner.hasFiles(ctx) }
    var ask by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { Clean.measure(ctx, scope) }
    val junk = Clean.junk

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 96.dp),
        ) {
            item { SpaceCard() }
            Clean.freed?.let { f -> item { DoneCard(f) } }
            when {
                !files -> item { AccessCard() }
                Clean.running -> item { Searching() }
                junk == null -> item {
                    Button(onClick = { Clean.scan(ctx, scope) }, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                        Text(t("Знайти сміття", "Find junk"))
                    }
                }
                junk.values.all { it.isEmpty() } -> item {
                    Text(
                        t("Сміття не знайдено. Чисто!", "No junk found. All clean!"),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(20.dp),
                    )
                }
                else -> {
                    for (j in Junk.entries) {
                        val items = junk[j].orEmpty()
                        if (items.isNotEmpty()) item(key = "j:" + j.name) { Category(j, items) }
                    }
                    item {
                        TextButton(onClick = { Clean.scan(ctx, scope) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(t("Шукати ще раз", "Search again"))
                        }
                    }
                }
            }
            item { CacheCard(files) }
        }
        val n = Clean.selectedCount()
        if (files && junk != null && !Clean.running && n > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = pad.calculateBottomPadding()),
            ) {
                Button(onClick = { ask = true }, modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp).height(52.dp)) {
                    Text(t("Очистити ${bytes(Clean.selectedSize())}", "Clean ${bytes(Clean.selectedSize())}"), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text(t("Видалити вибране?", "Delete the selection?")) },
        text = { Text(t("${L.files(Clean.selectedCount())}, ${bytes(Clean.selectedSize())}. Відновити не вийде.", "${L.files(Clean.selectedCount())}, ${bytes(Clean.selectedSize())}. This can't be undone.")) },
        confirmButton = { TextButton(onClick = { ask = false; Clean.clean(ctx, scope) }) { Text(t("Видалити", "Delete")) } },
        dismissButton = { TextButton(onClick = { ask = false }) { Text(t("Скасувати", "Cancel")) } },
    )
}

@Composable
private fun SpaceCard() {
    val s = Clean.space
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(24.dp)) {
            Text(t("Пам'ять телефону", "Phone storage"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                if (s.total > 0) t("Вільно ${bytes(s.free)}", "${bytes(s.free)} free") else "—",
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (s.total > 0) s.used.toFloat() / s.total else 0f },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            )
            Spacer(Modifier.height(8.dp))
            if (s.total > 0) Text(
                t("Зайнято ${bytes(s.used)} з ${bytes(s.total)}", "${bytes(s.used)} of ${bytes(s.total)} used"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DoneCard(freed: Long) {
    Surface(color = LevelColors.bg(ua.x44to.varta.scan.Level.OK), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = LevelColors.fg(ua.x44to.varta.scan.Level.OK))
            Spacer(Modifier.width(12.dp))
            Text(t("Звільнено ${bytes(freed)}", "Freed ${bytes(freed)}"), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AccessCard() {
    val ctx = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(t("Потрібен доступ до файлів", "File access needed"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                t("Щоб знайти сміття, «Варта» має бачити файли в пам'яті телефону. Нічого не видаляється без вашого підтвердження й нікуди не надсилається.",
                    "To find junk, Varta needs to see files in the phone's storage. Nothing is deleted without your confirmation or sent anywhere."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { FileAccess.request(ctx) }) { Text(t("Дати доступ", "Allow access")) }
        }
    }
}

@Composable
private fun Searching() {
    Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
        Spacer(Modifier.width(16.dp))
        Text(t("Шукаю сміття…", "Looking for junk…"), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Category(j: Junk, items: List<JunkItem>) {
    var open by remember { mutableStateOf(false) }
    val sel = Clean.selected
    val picked = items.count { it.file.path in sel }
    val state = when (picked) {
        0 -> ToggleableState.Off
        items.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val size = items.sumOf { it.size }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp).animateContentSize()) {
        Column {
            Row(Modifier.clickable { open = !open }.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TriStateCheckbox(state = state, onClick = {
                    val paths = items.map { it.file.path }
                    Clean.selected = if (state == ToggleableState.On) sel - paths.toSet() else sel + paths
                })
                Icon(junkIcon(j), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(junkTitle(j), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (j == Junk.EMPTY_DIRS) L.folders(items.size) else "${bytes(size)} · ${L.files(items.size)}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (open) {
                Text(
                    junkText(j), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                )
                for (it in items.take(150)) ItemRow(j, it)
                if (items.size > 150) Text(
                    t("…і ще ${items.size - 150}", "…and ${items.size - 150} more"),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp, 8.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ItemRow(j: Junk, it: JunkItem) {
    val on = it.file.path in Clean.selected
    val apk = if (j == Junk.APKS) Clean.apks[it.file.path] else null
    val name = when {
        apk != null -> apk.label
        j == Junk.LEFTOVERS && it.pkg != null -> it.pkg
        else -> it.file.name
    }
    val sub = buildString {
        if (j != Junk.EMPTY_DIRS) append(bytes(it.size)).append(" · ")
        if (apk?.installed == true) append(t("вже встановлено · ", "already installed · "))
        append(it.file.parent?.removePrefix(Cleaner.root.path)?.ifEmpty { "/" } ?: "")
        if (apk != null) append("/").append(it.file.name)
    }
    Row(
        Modifier.fillMaxWidth().clickable { Clean.selected = if (on) Clean.selected - it.file.path else Clean.selected + it.file.path }
            .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = on, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CacheCard(files: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var done by remember { mutableStateOf<Boolean?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        done = r.resultCode == Activity.RESULT_OK
        Clean.cacheCleared(ctx, scope)
    }
    val tick = FileAccess.tick
    val usage = remember(tick) { Cleaner.hasUsage(ctx) }
    val caches = Clean.caches
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(t("Кеш усіх програм", "Cache of all apps"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    caches.isNotEmpty() -> t("Зараз: ${bytes(caches.sumOf { it.cache })} у ${L.apps(caches.size)}.", "Now: ${bytes(caches.sumOf { it.cache })} in ${L.apps(caches.size)}.")
                    Build.VERSION.SDK_INT >= 30 -> t("Android покаже, скільки місця звільниться.", "Android will show how much space it frees.")
                    else -> t("На Android 10 і старших кеш програм чиститься в налаштуваннях кожної програми.", "On Android 10 and older, app cache is cleared in each app's settings.")
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (done) {
                true -> Text(t("Готово: кеш очищено.", "Done: cache cleared."), style = MaterialTheme.typography.bodyMedium, color = LevelColors.fg(ua.x44to.varta.scan.Level.OK))
                false -> Text(t("Скасовано.", "Cancelled."), style = MaterialTheme.typography.bodyMedium)
                null -> {}
            }
            Spacer(Modifier.height(12.dp))
            val intent = Cleaner.clearCacheIntent()
            if (intent != null) Button(onClick = {
                if (!files) FileAccess.request(ctx) else runCatching { launcher.launch(intent) }.onFailure { done = false }
            }) { Text(t("Очистити кеш програм", "Clear app cache")) }
            if (caches.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(t("Найбільше кешу", "Most cache"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                for (c in caches.take(5)) Row(
                    Modifier.fillMaxWidth().clickable { Actions.appInfo(ctx, c.pkg) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(c.pkg, 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(c.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(bytes(c.cache), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (!usage) {
                OutlinedButton(onClick = { Actions.open(ctx, Cleaner.usageIntent()) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(t("Показати, у кого найбільше кешу", "Show which apps cache the most"))
                }
            }
        }
    }
}
