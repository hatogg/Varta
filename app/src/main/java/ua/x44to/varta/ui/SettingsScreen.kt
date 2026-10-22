@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ua.x44to.varta.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ua.x44to.varta.BuildConfig
import ua.x44to.varta.data.Store
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.AppScanner
import ua.x44to.varta.scan.Ioc
import ua.x44to.varta.scan.Scan
import ua.x44to.varta.work.DailyCheck

@Composable
fun SettingsScreen(pad: PaddingValues) {
    val ctx = LocalContext.current
    // без дозволу на сповіщення щоденна перевірка нікому нічого не скаже — тож вмикаємо лише з ним
    val notify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        Store.chooseDaily(ok)
        DailyCheck.schedule(ctx, ok)
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp),
    ) {
        SectionTitle(t("Тема", "Theme"))
        Choice(listOf(t("Як у системі", "System"), t("Світла", "Light"), t("Темна", "Dark")), Store.theme) { Store.chooseTheme(it) }

        SectionTitle(t("Мова", "Language"))
        val langIdx = when (Store.lang) { "uk" -> 1; "en" -> 2; else -> 0 }
        Choice(listOf(t("Як у системі", "System"), "Українська", "English"), langIdx) {
            Store.chooseLang(when (it) { 1 -> "uk"; 2 -> "en"; else -> null })
        }

        SectionTitle(t("Перевірка", "Checking"))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("Щодня перевіряти нові програми", "Check new apps daily"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        t("Раз на день «Варта» перевірить усе, що встановилось чи оновилось, і сповістить, якщо знайде стежилку.",
                            "Once a day Varta checks everything installed or updated and notifies you if it finds stalkerware."),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = Store.daily, onCheckedChange = { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Store.chooseDaily(on)
                        DailyCheck.schedule(ctx, on)
                    }
                })
            }
        }

        val trusted = Store.trusted.toList().sorted()
        if (trusted.isNotEmpty()) {
            SectionTitle(t("Довірені програми", "Trusted apps"))
            val scanner = AppScanner(ctx)
            Card {
                for (p in trusted) Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(p, 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(scanner.labelOf(p) ?: p, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { Store.trust(p, false); Scan.refresh(ctx) }) { Text(t("Прибрати", "Remove")) }
                }
            }
        }

        SectionTitle(t("Про «Варту»", "About Varta"))
        val ioc = Ioc.get(ctx)
        Card {
            Text(t("Версія ${BuildConfig.VERSION_NAME}", "Version ${BuildConfig.VERSION_NAME}"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                t("База загроз: ${ioc.spyCount} шпигунських програм і ${ioc.watchCount} програм стеження від проєкту Echap" + (if (ioc.date.isNotEmpty()) ", оновлено ${dateText(ioc.date)}." else "."),
                    "Threat database: ${ioc.spyCount} stalkerware and ${ioc.watchCount} monitoring apps from the Echap project" + (if (ioc.date.isNotEmpty()) ", updated ${dateText(ioc.date)}." else ".")),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                t("«Варта» нічого не надсилає в інтернет: перевірка й очищення відбуваються на телефоні. Реклами немає.",
                    "Varta sends nothing to the internet: checking and cleaning happen on the phone. No ads."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = { Actions.web(ctx, "https://github.com/AssoEchap/stalkerware-indicators") }) { Text(t("База Echap (CC BY 4.0)", "Echap database (CC BY 4.0)")) }
                TextButton(onClick = { Actions.web(ctx, "https://github.com/hatogg/Varta") }) { Text(t("Код", "Source")) }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Choice(options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = i == selected, onClick = { onPick(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(o, maxLines = 1) }
        }
    }
}
