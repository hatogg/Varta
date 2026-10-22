package ua.x44to.varta.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import ua.x44to.varta.scan.DeviceIssue

/** Переходи в системні налаштування. Виробники ховають екрани по-різному, тож пробуємо кілька варіантів. */
object Actions {
    fun open(ctx: Context, vararg intents: Intent): Boolean {
        for (i in intents) {
            try {
                ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: Exception) {
                // наступний варіант
            }
        }
        return false
    }

    fun uninstall(ctx: Context, pkg: String) = open(ctx, Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")), appInfoIntent(pkg))

    fun appInfoIntent(pkg: String) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))

    fun appInfo(ctx: Context, pkg: String) = open(ctx, appInfoIntent(pkg))

    fun accessibility(ctx: Context) = open(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun notificationAccess(ctx: Context) = open(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun deviceAdmins(ctx: Context) = open(
        ctx,
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$DeviceAdminSettingsActivity")),
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings")),
        Intent(Settings.ACTION_SECURITY_SETTINGS),
    )

    fun playProtect(ctx: Context) = open(
        ctx,
        Intent().setComponent(ComponentName("com.google.android.gms", "com.google.android.gms.security.settings.VerifyAppsSettingsActivity")),
        Intent("com.google.android.gms.settings.VERIFY_APPS_SETTINGS"),
    )

    /** Чи є для проблеми куди перейти. */
    fun hasFix(i: DeviceIssue) = i != DeviceIssue.ROOT && i != DeviceIssue.BOOTLOADER

    fun fixTitle(i: DeviceIssue) = when (i) {
        DeviceIssue.NO_SCREEN_LOCK -> ua.x44to.varta.data.t("Налаштувати блокування", "Set up screen lock")
        DeviceIssue.OLD_PATCH, DeviceIssue.VERY_OLD_PATCH -> ua.x44to.varta.data.t("Перевірити оновлення", "Check for updates")
        DeviceIssue.USB_DEBUG, DeviceIssue.WIFI_DEBUG -> ua.x44to.varta.data.t("Параметри розробника", "Developer options")
        DeviceIssue.USER_CA -> ua.x44to.varta.data.t("Відкрити сертифікати", "Open certificates")
        DeviceIssue.VPN -> ua.x44to.varta.data.t("Налаштування VPN", "VPN settings")
        DeviceIssue.PROXY -> ua.x44to.varta.data.t("Налаштування Wi‑Fi", "Wi‑Fi settings")
        DeviceIssue.ROOT, DeviceIssue.BOOTLOADER -> ""
    }

    fun fix(ctx: Context, i: DeviceIssue) = when (i) {
        DeviceIssue.NO_SCREEN_LOCK -> open(ctx, Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD), Intent(Settings.ACTION_SECURITY_SETTINGS))
        DeviceIssue.OLD_PATCH, DeviceIssue.VERY_OLD_PATCH -> open(
            ctx, Intent("android.settings.SYSTEM_UPDATE_SETTINGS"), Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        )
        DeviceIssue.USB_DEBUG, DeviceIssue.WIFI_DEBUG -> open(ctx, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        DeviceIssue.USER_CA -> open(
            ctx, Intent("com.android.settings.TRUSTED_CREDENTIALS_USER"), Intent("com.android.settings.TRUSTED_CREDENTIALS"),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )
        DeviceIssue.VPN -> open(ctx, Intent(Settings.ACTION_VPN_SETTINGS), Intent(Settings.ACTION_WIRELESS_SETTINGS))
        DeviceIssue.PROXY -> open(ctx, Intent(Settings.ACTION_WIFI_SETTINGS))
        DeviceIssue.ROOT, DeviceIssue.BOOTLOADER -> false
    }

    fun web(ctx: Context, url: String) = open(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
