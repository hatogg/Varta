package ua.x44to.varta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.x44to.varta.clean.Junk
import ua.x44to.varta.clean.JunkScanner
import java.io.File
import java.nio.file.Files

class JunkScannerTest {
    private fun tree(): File {
        val r = Files.createTempDirectory("sdcard").toFile()
        fun f(path: String, bytes: Int = 10, ageDays: Int = 0): File = File(r, path).apply {
            parentFile!!.mkdirs()
            writeBytes(ByteArray(bytes))
            if (ageDays > 0) setLastModified(System.currentTimeMillis() - ageDays * JunkScanner.DAY)
        }
        f("Download/report.pdf")
        f("Download/old.apk", 300)
        f("Download/movie.mkv.crdownload", 50, ageDays = 3)
        f("Download/fresh.part", 50)
        f("DCIM/Camera/IMG_1.jpg", 100)
        f("DCIM/.thumbnails/1.jpg", 20)
        f("Telegram/log.txt.log", 30)
        f("app/cache/x.tmp", 40)
        f("Movies/big.mp4", 2000)
        File(r, "EmptyA/EmptyB").mkdirs()
        File(r, "Music").mkdirs()                 // стандартна порожня — не чіпаємо
        File(r, "Pictures/Screens/Empty").mkdirs() // порожня підпапка в стандартній — можна
        f("Android/media/com.gone.app/a.bin", 70)
        f("Android/media/com.still.here/a.bin", 70)
        f("Android/data/com.some.app/cache/c.bin", 90)
        f("Android/data/com.some.app/files/keep.bin", 90)
        return r
    }

    private fun scan(r: File, legacy: Boolean) =
        JunkScanner(r, { it == "com.still.here" || it == "com.some.app" }, legacyCache = legacy, largeMin = 1000).scan()

    @Test fun findsEveryKind() {
        val r = tree()
        val j = scan(r, legacy = true)
        fun names(k: Junk) = j[k]!!.map { it.file.name }.toSet()
        assertEquals(setOf("old.apk"), names(Junk.APKS))
        assertEquals(setOf("movie.mkv.crdownload", "log.txt.log", "x.tmp"), names(Junk.TEMP))
        assertEquals(setOf("1.jpg"), names(Junk.THUMBS))
        assertEquals(setOf("big.mp4"), names(Junk.LARGE))
        assertEquals(setOf("com.gone.app"), names(Junk.LEFTOVERS))
        assertEquals(setOf("EmptyA", "Screens"), names(Junk.EMPTY_DIRS))
        assertEquals(listOf("com.some.app"), j[Junk.APP_CACHE]!!.map { it.pkg })
        assertEquals(90L, j[Junk.APP_CACHE]!!.single().size)
    }

    @Test fun newerAndroidDoesNotTouchAppData() {
        val j = scan(tree(), legacy = false)
        assertTrue(j[Junk.APP_CACHE]!!.isEmpty())
    }

    @Test fun deletesOnlyWhatWasPicked() {
        val r = tree()
        val j = scan(r, legacy = true)
        val freed = JunkScanner.delete(j[Junk.TEMP]!! + j[Junk.EMPTY_DIRS]!! + j[Junk.LEFTOVERS]!!)
        assertEquals((50 + 30 + 40 + 70).toLong(), freed)
        assertFalse(File(r, "EmptyA").exists())
        assertFalse(File(r, "Android/media/com.gone.app").exists())
        assertTrue(File(r, "Music").exists())
        assertTrue(File(r, "Download/report.pdf").exists())
        assertTrue(File(r, "Download/fresh.part").exists())
        // кеш: вміст видаляється, папка лишається
        assertEquals(90L, JunkScanner.delete(j[Junk.APP_CACHE]!!, keepDir = true))
        assertTrue(File(r, "Android/data/com.some.app/cache").isDirectory)
        assertTrue(File(r, "Android/data/com.some.app/files/keep.bin").exists())
    }
}
