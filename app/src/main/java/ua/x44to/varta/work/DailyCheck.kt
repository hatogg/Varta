package ua.x44to.varta.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ua.x44to.varta.MainActivity
import ua.x44to.varta.R
import ua.x44to.varta.data.Store
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.AppScanner
import ua.x44to.varta.scan.Level
import ua.x44to.varta.ui.Texts
import java.util.concurrent.TimeUnit

/** Раз на день: перевірити встановлені й оновлені програми і сповістити про стежилки та підозрілі програми. */
class DailyCheck(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Store.init(applicationContext)
        val pm = applicationContext.packageManager
        val seq = Store.pkgSequence
        val changed = runCatching { pm.getChangedPackages(seq) }.getOrNull()
        val scanner = AppScanner(applicationContext)
        // після перезавантаження номер змін скидається (getChangedPackages дає null) — тоді перевіряємо все
        val names: List<String> = changed?.packageNames ?: scanner.installed().map { it.packageName }
        (changed ?: runCatching { pm.getChangedPackages(0) }.getOrNull())?.let { Store.pkgSequence = it.sequenceNumber }
        val env = scanner.env()
        val notified = Store.notified.toMutableSet()
        for (p in names.distinct()) {
            val r = scanner.scanOne(p, env) ?: continue
            if (r.level != Level.DANGER && r.level != Level.WARN) continue
            val key = "${r.pkg}@${r.lastUpdate}"
            if (key in notified) continue
            notify(r.pkg, r.label, r.ioc?.let { Texts.iocTitle(it) } ?: t("Варто перевірити: багато прав, не з магазину", "Worth checking: broad access, not from a store"), r.level == Level.DANGER)
            notified += key
        }
        Store.notified = notified
        return Result.success()
    }

    private fun notify(pkg: String, label: String, text: String, danger: Boolean) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, t("Перевірка програм", "App checks"), NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            ctx, pkg.hashCode(),
            Intent(ctx, MainActivity::class.java).putExtra(MainActivity.EXTRA_PKG, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(if (danger) t("Небезпечна програма: $label", "Dangerous app: $label") else t("Підозріла програма: $label", "Suspicious app: $label"))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(pkg.hashCode(), n)
    }

    companion object {
        private const val CHANNEL = "checks"
        private const val WORK = "daily-check"

        fun schedule(ctx: Context, on: Boolean) {
            val wm = WorkManager.getInstance(ctx)
            if (!on) {
                wm.cancelUniqueWork(WORK)
                return
            }
            val req = PeriodicWorkRequestBuilder<DailyCheck>(1, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
