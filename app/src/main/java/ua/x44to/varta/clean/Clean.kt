package ua.x44to.varta.clean

import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Стан екрана «Очищення»: що знайдено, що вибрано, скільки звільнено. */
object Clean {
    private val bg = MainScope()

    data class ApkLabel(val label: String, val pkg: String, val installed: Boolean)

    var junk by mutableStateOf<Map<Junk, List<JunkItem>>?>(null)
        private set
    var running by mutableStateOf(false)
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
    var apks by mutableStateOf<Map<String, ApkLabel>>(emptyMap())
        private set
    var freed by mutableStateOf<Long?>(null)
        private set
    var caches by mutableStateOf<List<Cleaner.AppCache>>(emptyList())
        private set
    var space by mutableStateOf(Cleaner.Space(0, 0))
        private set

    /** Що вибирати одразу: усе, крім великих файлів і APK, яких ще не встановлено. */
    private val AUTO = setOf(Junk.APP_CACHE, Junk.EMPTY_DIRS, Junk.TEMP, Junk.THUMBS, Junk.LEFTOVERS)

    fun measure(ctx: Context, scope: CoroutineScope) {
        val app = ctx.applicationContext
        bg.launch {
            space = withContext(Dispatchers.IO) { Cleaner.space() }
            caches = withContext(Dispatchers.IO) { Cleaner.caches(app) }
        }
    }

    fun scan(ctx: Context, scope: CoroutineScope, keepFreed: Boolean = false) {
        if (running) return
        running = true
        if (!keepFreed) freed = null
        val app = ctx.applicationContext
        bg.launch {
            val (j, a) = withContext(Dispatchers.IO) {
                val installed = Cleaner.installed(app)
                val j = JunkScanner(Cleaner.root, { it in installed }, legacyCache = Build.VERSION.SDK_INT < 30).scan()
                val a = HashMap<String, ApkLabel>()
                for (it in j[Junk.APKS].orEmpty()) {
                    val m = Cleaner.apkMeta(app, it.file) ?: continue
                    val have = Cleaner.installedVersion(app, m.pkg)
                    a[it.file.path] = ApkLabel(m.label, m.pkg, have != null && have >= m.versionCode)
                }
                j to a
            }
            junk = j
            apks = a
            selected = j.filterKeys { it in AUTO }.values.flatten().map { it.file.path }.toSet() +
                j[Junk.APKS].orEmpty().filter { a[it.file.path]?.installed == true }.map { it.file.path }
            space = withContext(Dispatchers.IO) { Cleaner.space() }
            caches = withContext(Dispatchers.IO) { Cleaner.caches(app) }
            running = false
        }
    }

    fun selectedSize(): Long = junk?.values?.flatten()?.filter { it.file.path in selected }?.sumOf { it.size } ?: 0L

    fun selectedCount(): Int = junk?.values?.flatten()?.count { it.file.path in selected } ?: 0

    fun clean(ctx: Context, scope: CoroutineScope) {
        val j = junk ?: return
        val app = ctx.applicationContext
        bg.launch {
            val n = withContext(Dispatchers.IO) {
                var total = 0L
                for ((kind, items) in j) {
                    val sel = items.filter { it.file.path in selected }
                    if (sel.isNotEmpty()) total += JunkScanner.delete(sel, keepDir = kind == Junk.APP_CACHE)
                }
                total
            }
            freed = n
            scan(app, this, keepFreed = true)
        }
    }

    fun cacheCleared(ctx: Context, scope: CoroutineScope) = measure(ctx, scope)
}
