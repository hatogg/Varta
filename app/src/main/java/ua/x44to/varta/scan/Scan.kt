package ua.x44to.varta.scan

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.x44to.varta.clean.Cleaner
import ua.x44to.varta.data.Store
import java.io.File

/** Повна перевірка: програми, налаштування телефону, файли APK. Стан — для інтерфейсу. */
object Scan {
    /** Не прив'язано до екрана: перевірка не обірветься, якщо екран перебудується (зміна мови, поворот). */
    private val bg = MainScope()

    var result by mutableStateOf<ScanResult?>(null)
        private set
    var running by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var current by mutableStateOf("")
        private set

    fun start(ctx: Context, scope: CoroutineScope) {
        if (running) return
        running = true
        progress = 0f
        current = ""
        val app = ctx.applicationContext
        bg.launch {
            val r = withContext(Dispatchers.Default) { run(app) { p, label -> progress = p; current = label } }
            result = r
            Store.lastScan = r.at
            running = false
        }
    }

    /** Після «Довіряю» чи видалення: прибрати видалені програми й перерахувати рівні без повторної перевірки. */
    fun refresh(ctx: Context) {
        val old = result ?: return
        val installed = Cleaner.installed(ctx)
        val apps = old.apps.filter { it.pkg in installed }.map { a ->
            val tr = a.pkg in Store.trusted
            a.copy(trusted = tr, level = Risk.level(a.flags, a.ioc, tr, a.system))
        }
        val files = old.files.filter { File(it.path).exists() }
        result = old.copy(apps = apps, files = files)
    }

    fun run(ctx: Context, onProgress: (Float, String) -> Unit = { _, _ -> }): ScanResult {
        val apps = AppScanner(ctx).scanAll { done, total, label ->
            onProgress(if (total == 0) 0f else 0.85f * done / total, label)
        }
        onProgress(0.88f, "")
        val device = DeviceScanner(ctx).scan()
        val files = if (Cleaner.hasFiles(ctx)) apkFiles(ctx) { p, n -> onProgress(0.9f + 0.1f * p, n) } else null
        onProgress(1f, "")
        return ScanResult(System.currentTimeMillis(), apps, device, files ?: emptyList(), filesChecked = files != null)
    }

    /** Файли APK у пам'яті телефону («Завантаження», папки месенджерів), які збігаються з базою стежилок. */
    fun apkFiles(ctx: Context, onProgress: (Float, String) -> Unit = { _, _ -> }): List<ApkFinding> {
        val ioc = Ioc.get(ctx)
        val found = findApks(Cleaner.root)
        val out = ArrayList<ApkFinding>()
        found.forEachIndexed { i, f ->
            onProgress(i.toFloat() / found.size.coerceAtLeast(1), f.name)
            val meta = Cleaner.apkMeta(ctx, f)
            val sha = if (f.length() <= AppScanner.MAX_HASH) Certs.sha256(f) else null
            val sigs = meta?.sigs ?: emptyList()
            val hit = ioc.match(meta?.pkg, sigs.map { Certs.sha1(it) }, sigs.map { Certs.subject(it) }, sha)
            if (hit != null) out += ApkFinding(f.path, f.length(), meta?.pkg, meta?.label, hit)
        }
        return out
    }

    fun findApks(root: File, maxDepth: Int = 8): List<File> {
        val out = ArrayList<File>()
        fun walk(d: File, depth: Int) {
            if (depth > maxDepth) return
            d.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    if (!(depth == 0 && f.name == "Android")) walk(f, depth + 1)
                } else if (f.name.lowercase().endsWith(".apk")) {
                    out += f
                }
            }
        }
        walk(root, 0)
        return out
    }
}
