package ua.x44to.varta.scan

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Налаштування телефону, через які за вами можуть стежити або які полегшують зламування. */
class DeviceScanner(private val ctx: Context) {

    fun scan(): List<DeviceFinding> {
        val out = ArrayList<DeviceFinding>()
        val cr = ctx.contentResolver

        runCatching {
            if (ctx.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == false)
                out += DeviceFinding(DeviceIssue.NO_SCREEN_LOCK, Level.WARN)
        }

        patchAge(Build.VERSION.SECURITY_PATCH)?.let { days ->
            when {
                days > 365 -> out += DeviceFinding(DeviceIssue.VERY_OLD_PATCH, Level.WARN, Build.VERSION.SECURITY_PATCH)
                days > 120 -> out += DeviceFinding(DeviceIssue.OLD_PATCH, Level.INFO, Build.VERSION.SECURITY_PATCH)
            }
        }

        runCatching {
            if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 1)
                out += DeviceFinding(DeviceIssue.USB_DEBUG, Level.INFO)
        }
        if (Build.VERSION.SDK_INT >= 30) runCatching {
            if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1)
                out += DeviceFinding(DeviceIssue.WIFI_DEBUG, Level.WARN)
        }

        rootSigns().takeIf { it.isNotEmpty() }?.let { out += DeviceFinding(DeviceIssue.ROOT, Level.WARN, it.joinToString(", ")) }

        userCertificates().takeIf { it.isNotEmpty() }?.let { out += DeviceFinding(DeviceIssue.USER_CA, Level.WARN, it.joinToString("\n")) }

        runCatching {
            val cm = ctx.getSystemService(ConnectivityManager::class.java)
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
                out += DeviceFinding(DeviceIssue.VPN, Level.INFO, vpnApps().joinToString(", "))
            val proxy = cm.defaultProxy
            val host = proxy?.host ?: System.getProperty("http.proxyHost")
            if (!host.isNullOrBlank()) {
                val port = proxy?.port?.takeIf { it > 0 } ?: System.getProperty("http.proxyPort")?.toIntOrNull()
                out += DeviceFinding(DeviceIssue.PROXY, Level.WARN, if (port != null) "$host:$port" else host)
            }
        }

        val vbs = prop("ro.boot.verifiedbootstate")
        if (vbs == "orange" || prop("ro.boot.flash.locked") == "0")
            out += DeviceFinding(DeviceIssue.BOOTLOADER, Level.INFO)

        return out
    }

    /** Ознаки root: файл su, тестові ключі збірки, програми керування root. */
    fun rootSigns(): List<String> {
        val signs = ArrayList<String>()
        val su = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/data/adb/magisk")
        su.firstOrNull { File(it).exists() }?.let { signs += it }
        if (Build.TAGS?.contains("test-keys") == true && !isEmulator()) signs += "test-keys"
        val managers = listOf("com.topjohnwu.magisk", "eu.chainfire.supersu", "com.koushikdutta.superuser", "com.noshufou.android.su", "me.weishu.kernelsu", "me.bmax.apatch")
        for (p in managers) if (installed(p)) signs += p
        return signs
    }

    private fun isEmulator() = Build.FINGERPRINT.contains("generic") || Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish")

    @Suppress("DEPRECATION")
    private fun installed(p: String) = runCatching { ctx.packageManager.getPackageInfo(p, 0); true }.getOrDefault(false)

    /** Сертифікати, які додав користувач (або хтось за нього): з ними можна читати зашифрований трафік. */
    fun userCertificates(): List<String> = runCatching {
        val ks = KeyStore.getInstance("AndroidCAStore")
        ks.load(null)
        ks.aliases().toList().filter { it.startsWith("user:") }.map { alias ->
            val c = ks.getCertificate(alias) as? X509Certificate
            val dn = c?.subjectX500Principal?.name ?: alias
            Certs.attr(dn, "CN") ?: Certs.attr(dn, "O") ?: dn
        }
    }.getOrDefault(emptyList())

    @Suppress("DEPRECATION")
    private fun vpnApps(): List<String> = runCatching {
        val pm = ctx.packageManager
        pm.queryIntentServices(Intent("android.net.VpnService"), PackageManager.GET_META_DATA)
            .map { it.serviceInfo.applicationInfo.loadLabel(pm).toString() }.distinct()
    }.getOrDefault(emptyList())

    private fun prop(key: String): String? = runCatching {
        val p = ProcessBuilder("getprop", key).redirectErrorStream(true).start()
        p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    companion object {
        /** Скільки днів минуло від патча безпеки «2024-05-01». */
        fun patchAge(patch: String?, today: LocalDate = LocalDate.now()): Long? = runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(patch!!.trim()), today)
        }.getOrNull()
    }
}
