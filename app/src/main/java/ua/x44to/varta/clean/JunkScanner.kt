package ua.x44to.varta.clean

import java.io.File
import java.nio.file.Files

/** Що вважаємо сміттям. Великі файли й APK лише показуємо — що з них видаляти, вирішує людина. */
enum class Junk { APP_CACHE, EMPTY_DIRS, TEMP, THUMBS, LEFTOVERS, APKS, LARGE }

data class JunkItem(val file: File, val size: Long, val dir: Boolean = false, val pkg: String? = null)

/**
 * Шукає сміття в спільній пам'яті телефону (/sdcard) — звичайний java.io, без Android, щоб перевіряти тестами.
 *
 * @param installed чи встановлена програма з такою назвою пакета (для «залишків видалених програм»)
 * @param legacyCache на Android 10 і старших можна чистити кеш програм у Android/data/<пакет>/cache напряму
 */
class JunkScanner(
    private val root: File,
    private val installed: (String) -> Boolean,
    private val legacyCache: Boolean,
    private val now: Long = System.currentTimeMillis(),
    private val largeMin: Long = 100L * 1024 * 1024,
) {
    private val out = Junk.entries.associateWith { ArrayList<JunkItem>() }

    fun scan(cancelled: () -> Boolean = { false }): Map<Junk, List<JunkItem>> {
        val android = File(root, "Android")
        File(android, "media").listFiles()?.forEach { d ->
            if (d.isDirectory && PKG.matches(d.name) && !installed(d.name)) out.getValue(Junk.LEFTOVERS) += JunkItem(d, size(d), dir = true, pkg = d.name)
        }
        if (legacyCache) File(android, "data").listFiles()?.forEach { d ->
            val c = File(d, "cache")
            if (c.isDirectory) {
                val s = size(c)
                if (s > 0) out.getValue(Junk.APP_CACHE) += JunkItem(c, s, dir = true, pkg = d.name)
            }
        }
        root.listFiles()?.forEach { f ->
            if (cancelled()) return out
            if (f.name == "Android") return@forEach
            walk(f, inThumbs = false, cancelled)
        }
        // порожні папки: лише найвищі (вміст видаляється разом із ними) і не стандартні («Download», «DCIM» тощо)
        root.listFiles()?.forEach { f ->
            if (f.isDirectory && f.name != "Android" && !isLink(f)) collectEmpty(f, top = true)
        }
        return out.mapValues { (_, v) -> v.sortedByDescending { it.size } }
    }

    private fun walk(f: File, inThumbs: Boolean, cancelled: () -> Boolean) {
        if (cancelled() || isLink(f)) return
        if (f.isDirectory) {
            val thumbs = inThumbs || f.name.equals(".thumbnails", ignoreCase = true)
            f.listFiles()?.forEach { walk(it, thumbs, cancelled) }
            return
        }
        val len = f.length()
        val name = f.name.lowercase()
        val age = now - f.lastModified()
        val kind = when {
            inThumbs -> Junk.THUMBS
            name.endsWith(".apk") || name.endsWith(".apks") || name.endsWith(".xapk") -> Junk.APKS
            name.endsWith(".tmp") || name.endsWith(".temp") || name.endsWith(".log") -> Junk.TEMP
            name == "thumbs.db" || name == ".ds_store" -> Junk.TEMP
            (name.endsWith(".crdownload") || name.endsWith(".part") || name.endsWith(".partial")) && age > DAY -> Junk.TEMP
            name.startsWith(".pending-") && age > 3 * DAY -> Junk.TEMP
            len >= largeMin -> Junk.LARGE
            else -> null
        }
        if (kind != null) out.getValue(kind) += JunkItem(f, len)
    }

    /** @return чи порожня папка (немає жодного файлу всередині, навіть глибоко) */
    private fun collectEmpty(d: File, top: Boolean): Boolean {
        val kids = d.listFiles() ?: return false
        var empty = true
        val emptyKids = ArrayList<File>()
        for (k in kids) {
            if (isLink(k)) { empty = false; continue }
            if (k.isDirectory) {
                if (collectEmpty(k, top = false)) emptyKids += k else empty = false
            } else {
                empty = false
            }
        }
        val protectedDir = top && d.name.lowercase() in STANDARD
        if (empty && !protectedDir) {
            if (top) out.getValue(Junk.EMPTY_DIRS) += JunkItem(d, 0, dir = true)
            return true
        }
        // ця папка не порожня (або стандартна) — порожні підпапки видаляємо окремо
        for (k in emptyKids) out.getValue(Junk.EMPTY_DIRS) += JunkItem(k, 0, dir = true)
        return false
    }

    private fun isLink(f: File) = runCatching { Files.isSymbolicLink(f.toPath()) }.getOrDefault(false)

    companion object {
        const val DAY = 24L * 3600 * 1000
        private val PKG = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
        val STANDARD = setOf(
            "dcim", "download", "downloads", "pictures", "music", "movies", "documents", "alarms", "notifications",
            "podcasts", "ringtones", "audiobooks", "recordings", "android", "screenshots",
        )

        fun size(f: File): Long {
            if (!f.isDirectory) return f.length()
            var s = 0L
            f.listFiles()?.forEach { s += size(it) }
            return s
        }

        /** Видалити вибране. Кеш програм — лише вміст папки cache, саму папку лишаємо. @return звільнено байтів */
        fun delete(items: Collection<JunkItem>, keepDir: Boolean = false): Long {
            var freed = 0L
            for (it in items) {
                val before = size(it.file)
                when {
                    it.dir && keepDir -> it.file.listFiles()?.forEach { c -> c.deleteRecursively() }
                    it.dir -> it.file.deleteRecursively()
                    else -> it.file.delete()
                }
                val after = if (it.file.exists()) size(it.file) else 0L
                freed += (before - after).coerceAtLeast(0)
            }
            return freed
        }
    }
}
