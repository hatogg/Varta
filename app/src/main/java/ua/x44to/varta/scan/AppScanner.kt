package ua.x44to.varta.scan

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import ua.x44to.varta.BuildConfig
import ua.x44to.varta.data.Store
import java.io.File

/** Перевіряє встановлені програми: збіги з базою, права, звідки встановлені. */
class AppScanner(private val ctx: Context) {
    private val pm = ctx.packageManager
    private val ioc = Ioc.get(ctx)
    private val ops = ctx.getSystemService(AppOpsManager::class.java)

    /** Що в системі ввімкнено для програм: спецможливості, адміністратори, доступ до сповіщень, значки в меню. */
    class Env(
        val launchable: Set<String>,
        val accessibility: Set<String>,
        val admins: Set<String>,
        val listeners: Set<String>,
        val defaultSms: String?,
    )

    fun env(): Env {
        val launch = HashSet<String>()
        for (cat in listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)) {
            for (r in queryActivities(Intent(Intent.ACTION_MAIN).addCategory(cat))) launch += r.activityInfo.packageName
        }
        val acc = HashSet<String>()
        runCatching {
            ctx.getSystemService(AccessibilityManager::class.java)
                ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?.forEach { s -> s.resolveInfo?.serviceInfo?.packageName?.let { acc += it } }
        }
        runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?.split(':')?.forEach { c -> ComponentName.unflattenFromString(c)?.packageName?.let { acc += it } }
        }
        val admins = runCatching {
            ctx.getSystemService(DevicePolicyManager::class.java)?.activeAdmins?.map { it.packageName }?.toSet()
        }.getOrNull() ?: emptySet()
        val listeners = runCatching { NotificationManagerCompat.getEnabledListenerPackages(ctx) }.getOrNull() ?: emptySet()
        val sms = runCatching { Telephony.Sms.getDefaultSmsPackage(ctx) }.getOrNull()
        return Env(launch, acc, admins, listeners, sms)
    }

    @Suppress("DEPRECATION")
    private fun queryActivities(i: Intent): List<ResolveInfo> = runCatching {
        if (Build.VERSION.SDK_INT >= 33) pm.queryIntentActivities(i, PackageManager.ResolveInfoFlags.of(0))
        else pm.queryIntentActivities(i, 0)
    }.getOrNull() ?: emptyList()

    private val pkgFlags: Int
        @Suppress("DEPRECATION")
        get() = PackageManager.GET_PERMISSIONS or
            (if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    fun installed(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= 33) pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(pkgFlags.toLong()))
        else pm.getInstalledPackages(pkgFlags)

    @Suppress("DEPRECATION")
    fun info(pkg: String): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(pkgFlags.toLong()))
        else pm.getPackageInfo(pkg, pkgFlags)
    }.getOrNull()

    /** Перевірити всі програми. [progress]: скільки зроблено, скільки всього, яку програму зараз. */
    fun scanAll(progress: (Int, Int, String) -> Unit = { _, _, _ -> }): List<AppReport> {
        val env = env()
        val list = installed().filter { !isSelf(it.packageName) }
        val hashes = Store.hashes()
        val keep = HashMap<String, String>()
        val out = ArrayList<AppReport>(list.size)
        list.forEachIndexed { i, pi ->
            val label = pi.applicationInfo?.loadLabel(pm)?.toString() ?: pi.packageName
            progress(i, list.size, label)
            report(pi, env, hashes, keep)?.let { out += it }
        }
        Store.saveHashes(keep)
        progress(list.size, list.size, "")
        return out
    }

    fun scanOne(pkg: String, env: Env = env()): AppReport? {
        if (isSelf(pkg)) return null
        val pi = info(pkg) ?: return null
        val hashes = Store.hashes()
        val keep = HashMap(hashes)
        return report(pi, env, hashes, keep).also { Store.saveHashes(keep) }
    }

    private fun isSelf(p: String) = p == BuildConfig.APPLICATION_ID || p.startsWith(BuildConfig.APPLICATION_ID + ".")

    private fun report(pi: PackageInfo, env: Env, hashes: Map<String, String>, keep: MutableMap<String, String>): AppReport? {
        val ai = pi.applicationInfo ?: return null
        val pkg = pi.packageName
        val system = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val label = runCatching { ai.loadLabel(pm).toString() }.getOrNull() ?: pkg

        val sigs = signatures(pi)
        val certSha1 = sigs.map { Certs.sha1(it.toByteArray()) }
        val subjects = sigs.map { Certs.subject(it.toByteArray()) }

        val apk = ai.sourceDir?.let { File(it) }
        val size = apk?.length() ?: 0L
        var sha: String? = null
        if (!system && apk != null && size in 1..MAX_HASH) {
            val key = "$pkg@${pi.lastUpdateTime}"
            sha = hashes[key] ?: Certs.sha256(apk)
            if (sha != null) keep[key] = sha
        }
        // вбудовані програми звіряємо лише за назвою пакета: їх підписує виробник, і збіг сертифіката тут — хибна тривога
        val ioc = if (system) ioc.match(pkg, emptyList(), emptyList(), null) else ioc.match(pkg, certSha1, subjects, sha)

        val (installer, initiator) = installSource(pkg)
        val source = Risk.source(installer, initiator, system)
        val flags = if (system) emptySet() else flags(pi, ai, label, source, env)
        val trusted = pkg in Store.trusted
        val score = Risk.score(flags)
        return AppReport(
            pkg = pkg, label = label, system = system, version = pi.versionName,
            install = Install(source, installer, initiator),
            firstInstall = pi.firstInstallTime, lastUpdate = pi.lastUpdateTime,
            flags = flags, ioc = ioc, score = score,
            level = Risk.level(flags, ioc, trusted, system), trusted = trusted, apkSize = size,
        )
    }

    @Suppress("DEPRECATION")
    private fun signatures(pi: PackageInfo): List<Signature> {
        if (Build.VERSION.SDK_INT >= 28) {
            val si = pi.signingInfo ?: return emptyList()
            val arr: Array<Signature>? = if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
            return arr?.toList() ?: emptyList()
        }
        return pi.signatures?.toList() ?: emptyList()
    }

    @Suppress("DEPRECATION")
    private fun installSource(pkg: String): Pair<String?, String?> = runCatching {
        if (Build.VERSION.SDK_INT >= 30) {
            val s = pm.getInstallSourceInfo(pkg)
            s.installingPackageName to s.initiatingPackageName
        } else {
            pm.getInstallerPackageName(pkg) to null
        }
    }.getOrNull() ?: (null to null)

    private fun flags(pi: PackageInfo, ai: ApplicationInfo, label: String, source: Source, env: Env): Set<Flag> {
        val pkg = pi.packageName
        val req = pi.requestedPermissions ?: emptyArray()
        val fl = pi.requestedPermissionsFlags ?: IntArray(0)
        fun granted(p: String): Boolean {
            val i = req.indexOf(p)
            return i >= 0 && i < fl.size && (fl[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
        }
        fun requested(p: String) = p in req

        val f = HashSet<Flag>()
        if (pkg in env.accessibility) f += Flag.ACCESSIBILITY
        if (pkg in env.admins) f += Flag.DEVICE_ADMIN
        if (pkg in env.listeners) f += Flag.NOTIFICATIONS
        if (granted(Manifest.permission.READ_SMS) || granted(Manifest.permission.RECEIVE_SMS) || granted(Manifest.permission.SEND_SMS)) f += Flag.SMS
        if (granted(Manifest.permission.READ_CALL_LOG)) f += Flag.CALL_LOG
        if (granted(Manifest.permission.RECORD_AUDIO)) f += Flag.MIC
        if (granted(Manifest.permission.CAMERA)) f += Flag.CAMERA
        if (Build.VERSION.SDK_INT >= 29 && granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) f += Flag.LOCATION_BG
        else if (granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)) f += Flag.LOCATION
        if (granted(Manifest.permission.READ_CONTACTS)) f += Flag.CONTACTS
        if (requested(Manifest.permission.SYSTEM_ALERT_WINDOW) && opAllowed(Manifest.permission.SYSTEM_ALERT_WINDOW, ai.uid, pkg)) f += Flag.OVERLAY
        if (requested(Manifest.permission.REQUEST_INSTALL_PACKAGES) && opAllowed(Manifest.permission.REQUEST_INSTALL_PACKAGES, ai.uid, pkg)) f += Flag.INSTALL_APPS
        if (requested(Manifest.permission.PACKAGE_USAGE_STATS) && opAllowed(Manifest.permission.PACKAGE_USAGE_STATS, ai.uid, pkg)) f += Flag.USAGE_ACCESS
        val side = Risk.sideloaded(source)
        if (side) f += Flag.SIDELOADED
        if (Risk.disguised(label, source)) f += Flag.DISGUISED
        if (side && ai.targetSdkVersion < 23) f += Flag.OLD_TARGET
        // без значка — підозріло лише в поєднанні з правами (клавіатури, шпалери й плагіни теж без значка)
        if (pkg !in env.launchable && Risk.score(f - Flag.SIDELOADED - Flag.DISGUISED) >= 2) f += Flag.HIDDEN_ICON
        return f
    }

    @Suppress("DEPRECATION")
    private fun opAllowed(perm: String, uid: Int, pkg: String): Boolean {
        val op = AppOpsManager.permissionToOp(perm) ?: return false
        return runCatching {
            val m = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(op, uid, pkg) else ops.checkOpNoThrow(op, uid, pkg)
            m == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun labelOf(pkg: String?): String? = pkg?.let {
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() }.getOrNull()
    }

    companion object {
        /** Великі ігри не хешуємо — довго, а стежилки маленькі. */
        const val MAX_HASH = 300L * 1024 * 1024
    }
}
