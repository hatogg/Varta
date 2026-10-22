@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ua.x44to.varta.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.x44to.varta.data.L
import ua.x44to.varta.data.Store
import ua.x44to.varta.data.bytes
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.ApkFinding
import ua.x44to.varta.scan.AppReport
import ua.x44to.varta.scan.AppScanner
import ua.x44to.varta.scan.DeviceFinding
import ua.x44to.varta.scan.Flag
import ua.x44to.varta.scan.Level
import ua.x44to.varta.scan.Scan
import ua.x44to.varta.scan.Source
import java.io.File

/** Що відкрито в нижній панелі. */
sealed interface Sel {
    data class App(val pkg: String) : Sel
    data class Device(val f: DeviceFinding) : Sel
    data class ApkFile(val f: ApkFinding) : Sel
}

@Composable
fun DetailSheet(sel: Sel, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
        ) {
            when (sel) {
                is Sel.App -> Scan.result?.apps?.firstOrNull { it.pkg == sel.pkg }?.let { AppDetail(it, onDismiss) }
                    ?: Text(t("Програму видалено", "The app was removed"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 24.dp))
                is Sel.Device -> DeviceDetail(sel.f)
                is Sel.ApkFile -> ApkDetail(sel.f, onDismiss)
            }
        }
    }
}

@Composable
private fun Banner(level: Level, title: String, text: String, foot: String? = null) {
    Surface(color = LevelColors.bg(level), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LevelColors.fg(level), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (foot != null) {
                Spacer(Modifier.height(8.dp))
                Text(foot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun summary(a: AppReport): Pair<String, String> = when {
    a.ioc != null -> Texts.iocTitle(a.ioc) to Texts.iocText(a.ioc)
    a.trusted -> t("Ви довіряєте цій програмі", "You trust this app") to t("«Варта» не показує її серед проблем.", "Varta doesn't list it among problems.")
    a.level == Level.WARN -> t("Варто перевірити", "Worth checking") to t(
        "Такий набір прав і таке джерело бувають у шпигунок і банківських вірусів. Якщо не знаєте, що це за програма, або не ставили її самі — видаліть.",
        "This mix of abilities and install source is typical of spyware and banking trojans. If you don't know this app or didn't install it, uninstall it.",
    )
    a.level == Level.INFO -> t("Має широкі права", "Has broad access") to t(
        "Нічого підозрілого в тому, звідки програма, але прав у неї багато. Якщо користуєтесь нею й довіряєте — усе гаразд.",
        "Nothing suspicious about where it came from, but it can do a lot. If you use and trust it, that's fine.",
    )
    else -> t("Нічого підозрілого", "Nothing suspicious") to t("Звичайна програма.", "A regular app.")
}

@Composable
private fun AppDetail(a: AppReport, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scanner = remember { AppScanner(ctx) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(a.pkg, 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(a.label, style = MaterialTheme.typography.titleLarge)
            Text(a.pkg + (a.version?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val (title, text) = summary(a)
    Banner(a.level, title, text, a.ioc?.let { Texts.iocBy(it.by) + t(" · база Echap", " · Echap database") })

    val src = Texts.source(a.install.source, scanner.labelOf(a.install.installer) ?: a.install.installer, scanner.labelOf(a.install.initiator) ?: a.install.initiator)
    Text(t("Звідки", "Installed"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        src + (if (a.firstInstall > 0 && a.install.source != Source.PREINSTALLED) t(", ", ", ") + L.ago(a.firstInstall) else ""),
        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
    )

    val flags = a.flags.filter { it != Flag.SIDELOADED }.sortedByDescending { it.weight }
    if (flags.isNotEmpty()) {
        Text(t("Що може", "What it can do"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        for (f in flags) {
            Row(Modifier.padding(vertical = 8.dp)) {
                Icon(flagIcon(f), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(Texts.flagTitle(f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(Texts.flagText(f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (Flag.DEVICE_ADMIN in a.flags) OutlinedButton(onClick = { Actions.deviceAdmins(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Вимкнути адміністратора", "Turn off device admin"))
        }
        if (Flag.ACCESSIBILITY in a.flags) OutlinedButton(onClick = { Actions.accessibility(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Вимкнути спецможливості", "Turn off accessibility"))
        }
        if (Flag.NOTIFICATIONS in a.flags) OutlinedButton(onClick = { Actions.notificationAccess(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Забрати доступ до сповіщень", "Revoke notification access"))
        }
        if (!a.system) Button(
            onClick = { Actions.uninstall(ctx, a.pkg) },
            modifier = Modifier.fillMaxWidth(),
            colors = if (a.level == Level.DANGER || a.level == Level.WARN) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError,
            ) else ButtonDefaults.buttonColors(),
        ) {
            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(t("Видалити", "Uninstall"))
        }
        OutlinedButton(onClick = { Actions.appInfo(ctx, a.pkg) }, modifier = Modifier.fillMaxWidth()) {
            Text(t("Налаштування програми", "App settings"))
        }
        if (a.level != Level.OK || a.trusted) TextButton(
            onClick = {
                Store.trust(a.pkg, !a.trusted)
                Scan.refresh(ctx)
                if (!a.trusted) onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (a.trusted) t("Більше не довіряю", "Stop trusting") else t("Довіряю цій програмі", "I trust this app")) }
    }
}

@Composable
private fun DeviceDetail(f: DeviceFinding) {
    val ctx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        RoundIcon(issueIcon(f.issue), f.level, 56.dp)
        Spacer(Modifier.width(16.dp))
        Text(Texts.issueTitle(f.issue), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(Texts.issueText(f.issue, f.detail), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(20.dp))
    if (Actions.hasFix(f.issue)) Button(onClick = { Actions.fix(ctx, f.issue) }, modifier = Modifier.fillMaxWidth()) {
        Text(Actions.fixTitle(f.issue))
    }
}

@Composable
private fun ApkDetail(f: ApkFinding, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var ask by remember { mutableStateOf(false) }
    val name = File(f.path).name
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.Rounded.Description, Level.DANGER, 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text(bytes(f.size) + " · " + f.path.removePrefix("/storage/emulated/0/"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Banner(
        if (f.ioc.watchware) Level.WARN else Level.DANGER,
        Texts.iocTitle(f.ioc),
        t("Це інсталятор програми з бази стежилок. Не встановлюйте його — краще видаліть файл.", "This is an installer of a known stalkerware app. Don't install it — delete the file.") +
            (f.label?.let { t("\n\nУсередині: $it", "\n\nInside: $it") + (f.pkg?.let { p -> " ($p)" } ?: "") } ?: ""),
        Texts.iocBy(f.ioc.by),
    )
    Button(
        onClick = { ask = true }, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
    ) { Text(t("Видалити файл", "Delete the file")) }
    if (ask) AlertDialog(
        onDismissRequest = { ask = false },
        title = { Text(t("Видалити $name?", "Delete $name?")) },
        text = { Text(t("Файл не можна буде відновити.", "The file can't be restored.")) },
        confirmButton = {
            TextButton(onClick = {
                File(f.path).delete()
                ask = false
                Scan.refresh(ctx)
                onDismiss()
            }) { Text(t("Видалити", "Delete")) }
        },
        dismissButton = { TextButton(onClick = { ask = false }) { Text(t("Скасувати", "Cancel")) } },
    )
}

fun shortReason(a: AppReport): String = when {
    a.ioc != null -> Texts.iocTitle(a.ioc)
    else -> topFlags(a.flags).joinToString(", ") { Texts.flagTitle(it) }.ifEmpty { Texts.flagTitle(Flag.SIDELOADED) }
}
