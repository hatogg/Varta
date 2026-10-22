@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ua.x44to.varta

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import ua.x44to.varta.data.Store
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.Scan
import ua.x44to.varta.ui.AppFilter
import ua.x44to.varta.ui.AppsScreen
import ua.x44to.varta.ui.CleanScreen
import ua.x44to.varta.ui.DetailSheet
import ua.x44to.varta.ui.FileAccess
import ua.x44to.varta.ui.ProtectScreen
import ua.x44to.varta.ui.Sel
import ua.x44to.varta.ui.SettingsScreen
import ua.x44to.varta.ui.VartaTheme

class MainActivity : ComponentActivity() {
    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { FileAccess.tick++ }

    /** Програма зі сповіщення щоденної перевірки — відкрити її одразу. */
    private var openPkg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Store.init(this)
        FileAccess.launcher = perms
        openPkg = intent?.getStringExtra(EXTRA_PKG)
        setContent {
            VartaTheme(Store.theme) {
                // мова змінюється без перезапуску: перебудувати все
                key(Store.lang) { App(openPkg) { openPkg = null } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_PKG)?.let { openPkg = it }
    }

    override fun onResume() {
        super.onResume()
        // повернулися з налаштувань чи вікна видалення: оновити дозволи й прибрати видалене
        FileAccess.tick++
        Scan.refresh(this)
    }

    override fun onDestroy() {
        if (FileAccess.launcher === perms) FileAccess.launcher = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PKG = "varta.pkg"
    }
}

private enum class Tab { PROTECT, CLEAN, APPS }

@Composable
private fun App(openPkg: String?, onOpened: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(Tab.PROTECT) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(AppFilter.RISKY) }
    var sel by remember { mutableStateOf<Sel?>(null) }

    // перша перевірка — одразу при запуску, щоб на головному екрані був результат
    LaunchedEffect(Unit) { if (Scan.result == null && !Scan.running) Scan.start(ctx, scope) }
    LaunchedEffect(openPkg, Scan.result) {
        val p = openPkg ?: return@LaunchedEffect
        if (Scan.result?.apps?.any { it.pkg == p } == true) {
            settings = false
            tab = Tab.PROTECT
            sel = Sel.App(p)
            onOpened()
        }
    }
    BackHandler(enabled = settings) { settings = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (settings) t("Налаштування", "Settings") else when (tab) {
                            Tab.PROTECT -> t("Варта", "Varta")
                            Tab.CLEAN -> t("Очищення", "Cleaning")
                            Tab.APPS -> t("Програми", "Apps")
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (settings) IconButton(onClick = { settings = false }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, t("Назад", "Back"))
                    }
                },
                actions = {
                    if (!settings) IconButton(onClick = { settings = true }) { Icon(Icons.Rounded.Settings, t("Налаштування", "Settings")) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (!settings) NavigationBar {
                NavItem(tab == Tab.PROTECT, Icons.Rounded.Shield, t("Захист", "Protection")) { tab = Tab.PROTECT }
                NavItem(tab == Tab.CLEAN, Icons.Rounded.CleaningServices, t("Очищення", "Cleaning")) { tab = Tab.CLEAN }
                NavItem(tab == Tab.APPS, Icons.Rounded.Apps, t("Програми", "Apps")) { tab = Tab.APPS }
            }
        },
    ) { pad: PaddingValues ->
        when {
            settings -> SettingsScreen(pad)
            tab == Tab.PROTECT -> ProtectScreen(pad, onSelect = { sel = it }, onShowApps = { filter = AppFilter.RISKY; tab = Tab.APPS })
            tab == Tab.CLEAN -> CleanScreen(pad)
            else -> AppsScreen(pad, filter, onFilter = { filter = it }, onSelect = { sel = it })
        }
    }
    sel?.let { s -> DetailSheet(s) { sel = null } }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(on: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    NavigationBarItem(selected = on, onClick = onClick, icon = { Icon(icon, null) }, label = { Text(label) })
}
