@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.x44to.varta.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.x44to.varta.scan.DeviceIssue
import ua.x44to.varta.scan.Flag
import ua.x44to.varta.scan.Level
import java.util.concurrent.ConcurrentHashMap

/** Значки програм: завантажуються у фоні й кешуються. */
object AppIcons {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun load(ctx: Context, pkg: String, px: Int): ImageBitmap? = cache[pkg] ?: runCatching {
        ctx.packageManager.getApplicationIcon(pkg).toBitmap(px, px).asImageBitmap()
    }.getOrNull()?.also { cache[pkg] = it }
}

@Composable
fun AppIcon(pkg: String, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val px = (size.value * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val img by produceState<ImageBitmap?>(null, pkg) { value = withContext(Dispatchers.IO) { AppIcons.load(ctx, pkg, px) } }
    val bmp = img
    if (bmp != null) {
        Image(bmp, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(size / 4.5f)))
    } else {
        Box(Modifier.size(size).clip(RoundedCornerShape(size / 4.5f)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Android, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size * 0.6f))
        }
    }
}

/** Значок у кружечку кольору рівня — для проблем без значка програми. */
@Composable
fun RoundIcon(icon: ImageVector, level: Level, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(LevelColors.bg(level)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = LevelColors.fg(level), modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Рядок знахідки: значок, назва, коротко чому, стрілка. */
@Composable
fun FindingRow(
    title: String,
    subtitle: String,
    level: Level,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) Text(
                    subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            LevelDot(level)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LevelDot(level: Level) {
    if (level == Level.OK || level == Level.INFO) return
    Box(Modifier.padding(end = 4.dp).size(10.dp).clip(CircleShape).background(LevelColors.fg(level)))
}

/** Маленька позначка: «Спецможливості», «Не з магазину». */
@Composable
fun Tag(text: String, strong: Boolean = false, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun Tags(items: List<String>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { items.forEach { Tag(it) } }
}

fun flagIcon(f: Flag): ImageVector = when (f) {
    Flag.ACCESSIBILITY -> Icons.Rounded.Accessibility
    Flag.DEVICE_ADMIN -> Icons.Rounded.AdminPanelSettings
    Flag.NOTIFICATIONS -> Icons.Rounded.Notifications
    Flag.SMS -> Icons.Rounded.Sms
    Flag.CALL_LOG -> Icons.Rounded.Call
    Flag.MIC -> Icons.Rounded.Mic
    Flag.CAMERA -> Icons.Rounded.PhotoCamera
    Flag.LOCATION_BG, Flag.LOCATION -> Icons.Rounded.LocationOn
    Flag.CONTACTS -> Icons.Rounded.Contacts
    Flag.OVERLAY -> Icons.Rounded.Layers
    Flag.INSTALL_APPS -> Icons.Rounded.InstallMobile
    Flag.USAGE_ACCESS -> Icons.Rounded.QueryStats
    Flag.HIDDEN_ICON -> Icons.Rounded.VisibilityOff
    Flag.SIDELOADED -> Icons.Rounded.Download
    Flag.DISGUISED -> Icons.Rounded.TheaterComedy
    Flag.OLD_TARGET -> Icons.Rounded.History
}

fun issueIcon(i: DeviceIssue): ImageVector = when (i) {
    DeviceIssue.NO_SCREEN_LOCK -> Icons.Rounded.LockOpen
    DeviceIssue.OLD_PATCH, DeviceIssue.VERY_OLD_PATCH -> Icons.Rounded.SystemUpdate
    DeviceIssue.USB_DEBUG -> Icons.Rounded.Usb
    DeviceIssue.WIFI_DEBUG -> Icons.Rounded.Wifi
    DeviceIssue.ROOT -> Icons.Rounded.DeveloperMode
    DeviceIssue.USER_CA -> Icons.Rounded.Key
    DeviceIssue.VPN -> Icons.Rounded.VpnKey
    DeviceIssue.PROXY -> Icons.Rounded.Public
    DeviceIssue.BOOTLOADER -> Icons.Rounded.Lock
}

/** Найважливіші позначки програми — для рядка списку (не більше трьох). */
fun topFlags(flags: Set<Flag>, n: Int = 3): List<Flag> =
    flags.filter { it != Flag.LOCATION }.sortedByDescending { it.weight }.take(n)
