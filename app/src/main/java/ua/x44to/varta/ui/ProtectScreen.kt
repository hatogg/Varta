package ua.x44to.varta.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.x44to.varta.clean.Cleaner
import ua.x44to.varta.data.L
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.Ioc
import ua.x44to.varta.scan.Level
import ua.x44to.varta.scan.Scan
import ua.x44to.varta.scan.ScanResult
import java.io.File

@Composable
fun ProtectScreen(pad: PaddingValues, onSelect: (Sel) -> Unit, onShowApps: () -> Unit) {
    val r = Scan.result
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 16.dp)) {
        item { Hero(r) }
        if (r != null && !Scan.running) {
            val danger = r.apps.filter { it.level == Level.DANGER }
            val warnApps = r.apps.filter { it.level == Level.WARN }
            val warnDev = r.device.filter { it.level == Level.WARN || it.level == Level.DANGER }
            val tips = r.device.filter { it.level == Level.INFO }
            if (danger.isNotEmpty() || r.files.isNotEmpty()) {
                item { SectionTitle(t("Небезпечно", "Dangerous")) }
                items(danger, key = { "d:" + it.pkg }) { a ->
                    FindingRow(a.label, shortReason(a), a.level, onClick = { onSelect(Sel.App(a.pkg)) }) { AppIcon(a.pkg) }
                }
                items(r.files, key = { "f:" + it.path }) { f ->
                    FindingRow(File(f.path).name, Texts.iocTitle(f.ioc), Level.DANGER, onClick = { onSelect(Sel.ApkFile(f)) }) {
                        RoundIcon(Icons.Rounded.Description, Level.DANGER)
                    }
                }
            }
            if (warnApps.isNotEmpty() || warnDev.isNotEmpty()) {
                item { SectionTitle(t("Варто перевірити", "Worth checking")) }
                items(warnApps, key = { "w:" + it.pkg }) { a ->
                    FindingRow(a.label, shortReason(a), a.level, onClick = { onSelect(Sel.App(a.pkg)) }) { AppIcon(a.pkg) }
                }
                items(warnDev, key = { "wd:" + it.issue }) { f ->
                    FindingRow(Texts.issueTitle(f.issue), firstSentence(Texts.issueText(f.issue, f.detail)), f.level, onClick = { onSelect(Sel.Device(f)) }) {
                        RoundIcon(issueIcon(f.issue), f.level)
                    }
                }
            }
            if (tips.isNotEmpty()) {
                item { SectionTitle(t("Поради", "Tips")) }
                items(tips, key = { "i:" + it.issue }) { f ->
                    FindingRow(Texts.issueTitle(f.issue), firstSentence(Texts.issueText(f.issue, f.detail)), f.level, onClick = { onSelect(Sel.Device(f)) }) {
                        RoundIcon(issueIcon(f.issue), f.level)
                    }
                }
            }
            val broad = r.apps.count { it.level == Level.INFO }
            if (broad > 0) item {
                TextButton(onClick = onShowApps, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(t("Ще ${L.apps(broad)} з широкими правами", "${L.apps(broad).replaceFirstChar { it.uppercase() }} with broad access"))
                }
            }
            if (!r.filesChecked) item { FilesHint() }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item { PlayProtectCard() }
        item { AboutCard() }
    }
}

private fun firstSentence(s: String): String {
    val i = s.indexOf(". ")
    return if (i in 1..140) s.substring(0, i + 1) else s
}

@Composable
private fun Hero(r: ScanResult?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val running = Scan.running
    val level = when {
        running || r == null -> null
        else -> r.worst
    }
    val bg = level?.let { LevelColors.bg(if (it == Level.INFO) Level.OK else it) } ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = level?.let { LevelColors.fg(if (it == Level.INFO) Level.OK else it) } ?: MaterialTheme.colorScheme.primary
    val icon: ImageVector = when (level) {
        null -> Icons.Rounded.Shield
        Level.DANGER -> Icons.Rounded.GppBad
        Level.WARN -> Icons.Rounded.GppMaybe
        else -> Icons.Rounded.GppGood
    }
    val (title, sub) = when {
        running -> t("Перевіряю…", "Checking…") to Scan.current.ifEmpty { t("Налаштування телефону", "Phone settings") }
        r == null -> t("Ще не перевіряли", "Not checked yet") to t("Програми, налаштування й файли — за хвилину.", "Apps, settings and files in about a minute.")
        r.danger > 0 -> (if (r.danger == 1) t("Знайдено загрозу", "Threat found") else t("Знайдено загрози: ${r.danger}", "Threats found: ${r.danger}")) to
            t("Перевірено ${L.apps(r.apps.size)} · ${L.ago(r.at)}", "Checked ${L.apps(r.apps.size)} · ${L.ago(r.at)}")
        r.warn > 0 -> t("Є що перевірити: ${r.warn}", "Worth checking: ${r.warn}") to
            t("Шпигунок не знайдено · ${L.ago(r.at)}", "No stalkerware found · ${L.ago(r.at)}")
        else -> t("Загроз не знайдено", "No threats found") to
            t("Перевірено ${L.apps(r.apps.size)} · ${L.ago(r.at)}", "Checked ${L.apps(r.apps.size)} · ${L.ago(r.at)}")
    }
    Surface(color = bg, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(16.dp).animateContentSize()) {
        Column(Modifier.padding(24.dp)) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(20.dp))
            if (running) {
                LinearProgressIndicator(progress = { Scan.progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = fg)
            } else {
                Button(
                    onClick = { Scan.start(ctx, scope) },
                    colors = if (level == Level.DANGER) ButtonDefaults.buttonColors(containerColor = fg, contentColor = MaterialTheme.colorScheme.surface) else ButtonDefaults.buttonColors(),
                ) { Text(if (r == null) t("Перевірити телефон", "Check the phone") else t("Перевірити ще раз", "Check again")) }
            }
        }
    }
}

@Composable
private fun FilesHint() {
    val ctx = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(t("Файли APK не перевірено", "APK files weren't checked"), style = MaterialTheme.typography.titleMedium)
            Text(
                t("Дайте доступ до файлів — і «Варта» перевірить інсталятори в «Завантаженнях» і папках месенджерів, перш ніж ви їх відкриєте.",
                    "Allow file access and Varta will check installers in Downloads and messenger folders before you open them."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { FileAccess.request(ctx) }) { Text(t("Дати доступ до файлів", "Allow file access")) }
        }
    }
}

@Composable
private fun PlayProtectCard() {
    val ctx = LocalContext.current
    Surface(
        onClick = { if (!Actions.playProtect(ctx)) Actions.web(ctx, "https://support.google.com/googleplay/answer/2812853") },
        color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(t("Перевірте ще й Play Захистом", "Also run Play Protect"), style = MaterialTheme.typography.titleMedium)
                Text(
                    t("У Google своя база вірусів. Разом надійніше.", "Google has its own malware database. Together it's more reliable."),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val ctx = LocalContext.current
    var open by rememberSaveable { mutableStateOf(false) }
    val ioc = Ioc.get(ctx)
    Surface(
        onClick = { open = !open },
        color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp).animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Що перевіряє «Варта»", "What Varta checks"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (open) {
                val body = MaterialTheme.typography.bodyMedium
                Text(
                    t("• Шпигунки й бекдори: кожну програму звіряє з базою Echap — ${ioc.spyCount} відомих стежилок і ${ioc.watchCount} програм стеження за назвою пакета, сертифікатом і хешем файлу.",
                        "• Stalkerware and backdoors: every app is compared with the Echap database — ${ioc.spyCount} known stalkerware and ${ioc.watchCount} monitoring apps — by package name, certificate and file hash."),
                    style = body,
                )
                Text(
                    t("• Підозрілу поведінку: спецможливості, адміністратор, доступ до сповіщень і SMS, програми без значка й не з магазину.",
                        "• Suspicious behaviour: accessibility, device admin, notification and SMS access, apps without an icon or not from a store."),
                    style = body,
                )
                Text(
                    t("• Налаштування: блокування екрана, оновлення безпеки, налагодження, root, чужі сертифікати, VPN і проксі.",
                        "• Settings: screen lock, security updates, debugging, root, user certificates, VPN and proxy."),
                    style = body,
                )
                Text(
                    t("Чесно про межі: без root жодна програма не бачить файлів інших програм, тож «Варта» не замінює повноцінний антивірус із хмарною базою. Нового невідомого вірусу в базі може не бути.",
                        "Honest limits: without root no app can look inside other apps' files, so Varta doesn't replace a full antivirus with a cloud database. A brand-new unknown virus may not be in the database."),
                    style = body, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (ioc.date.isNotEmpty()) Text(
                    t("База від ${dateText(ioc.date)}, CC BY 4.0.", "Database from ${dateText(ioc.date)}, CC BY 4.0."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!Cleaner.hasFiles(ctx)) Text(
                    t("Без доступу до файлів інсталятори APK не перевіряються.", "Without file access APK installers aren't checked."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun dateText(iso: String): String {
    val p = iso.split("-").mapNotNull { it.toIntOrNull() }
    return if (p.size == 3) L.date(p[0], p[1], p[2]) else iso
}
