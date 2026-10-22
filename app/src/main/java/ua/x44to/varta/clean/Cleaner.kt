package ua.x44to.varta.clean

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import ua.x44to.varta.BuildConfig
import java.io.File

/** Пам'ять телефону: скільки зайнято, доступ до файлів, кеш програм. */
object Cleaner {
    val root: File get() = Environment.getExternalStorageDirectory()

    data class Space(val total: Long, val free: Long) {
        val used get() = total - free
    }

    fun space(): Space = runCatching {
        val s = StatFs(Environment.getDataDirectory().path)
        Space(s.totalBytes, s.availableBytes)
    }.getOrDefault(Space(0, 0))

    /** Чи можна читати й видаляти файли в спільній пам'яті. */
    fun hasFiles(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    /** Android 11+: окреме вікно «Доступ до всіх файлів». */
    fun filesAccessIntent(): Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID))

    /** Доступ до статистики використання — потрібен, щоб побачити, скільки кешу в кожної програми. */
    @Suppress("DEPRECATION")
    fun hasUsage(ctx: Context): Boolean = runCatching {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val m = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        m == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun usageIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    /** Системне вікно «Очистити кеш усіх програм» (Android 11+, потрібен доступ до всіх файлів). */
    fun clearCacheIntent(): Intent? = if (Build.VERSION.SDK_INT >= 30) Intent("android.os.storage.action.CLEAR_APP_CACHE") else null

    data class AppCache(val pkg: String, val label: String, val cache: Long)

    /** Кеш кожної програми (внутрішній і зовнішній). Без доступу до статистики — порожньо. */
    @Suppress("DEPRECATION")
    fun caches(ctx: Context): List<AppCache> {
        if (!hasUsage(ctx)) return emptyList()
        val ssm = ctx.getSystemService(StorageStatsManager::class.java) ?: return emptyList()
        val pm = ctx.packageManager
        val user = Process.myUserHandle()
        val apps: List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= 33) pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        else pm.getInstalledApplications(0)
        return apps.mapNotNull { ai ->
            runCatching {
                val st = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, ai.packageName, user)
                AppCache(ai.packageName, ai.loadLabel(pm).toString(), st.cacheBytes)
            }.getOrNull()
        }.filter { it.cache > 0 }.sortedByDescending { it.cache }
    }

    fun installed(ctx: Context): Set<String> {
        val pm = ctx.packageManager
        @Suppress("DEPRECATION")
        val list = if (Build.VERSION.SDK_INT >= 33) pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0)) else pm.getInstalledApplications(0)
        return list.map { it.packageName }.toSet()
    }

    /** Що всередині файлу APK: назва пакета, версія, назва програми. */
    data class ApkMeta(val pkg: String, val versionCode: Long, val label: String, val sigs: List<ByteArray>)

    @Suppress("DEPRECATION")
    fun apkMeta(ctx: Context, f: File): ApkMeta? = runCatching {
        val pm = ctx.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val pi: PackageInfo = (if (Build.VERSION.SDK_INT >= 33) pm.getPackageArchiveInfo(f.path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else pm.getPackageArchiveInfo(f.path, flags)) ?: return null
        val ai = pi.applicationInfo
        ai?.sourceDir = f.path
        ai?.publicSourceDir = f.path
        val label = ai?.loadLabel(pm)?.toString() ?: pi.packageName
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val si = pi.signingInfo
            val arr = if (si == null) null else if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
            arr?.map { it.toByteArray() } ?: emptyList()
        } else {
            pi.signatures?.map { it.toByteArray() } ?: emptyList()
        }
        val vc = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
        ApkMeta(pi.packageName, vc, label, sigs)
    }.getOrNull()

    @Suppress("DEPRECATION")
    fun installedVersion(ctx: Context, pkg: String): Long? = runCatching {
        val pi = ctx.packageManager.getPackageInfo(pkg, 0)
        if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
    }.getOrNull()
}
