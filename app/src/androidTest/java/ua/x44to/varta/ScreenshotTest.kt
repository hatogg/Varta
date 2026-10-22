package ua.x44to.varta

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.x44to.varta.clean.Clean
import ua.x44to.varta.clean.Junk
import ua.x44to.varta.data.Store
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.Flag
import ua.x44to.varta.scan.Level
import ua.x44to.varta.scan.Scan
import java.io.File
import java.io.FileOutputStream

/**
 * Перевірка на справжньому Android і фотосесія для README. Перед запуском .github/screenshots.sh ставить
 * нешкідливу «приманку» com.systemservice (назва пакета відомої стежилки TheTruthSpy), вмикає їй спецможливості,
 * доступ до сповіщень і права адміністратора, кладе її APK у «Завантаження» і розкидає трохи сміття.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val inst = InstrumentationRegistry.getInstrumentation()
    private val ctx get() = inst.targetContext

    @Test
    fun checkAndShoot() {
        Store.init(ctx)
        for (lang in listOf("uk", "en")) {
            Store.chooseLang(lang)
            Store.chooseTheme(1)
            ActivityScenario.launch(MainActivity::class.java).use {
                waitFor("перевірка", 180_000) { Scan.result != null && !Scan.running }
                if (lang == "uk") verifyFindings()
                settle()
                shot("$lang-light-protect")

                click("System Service")
                settle()
                shot("$lang-light-detail")
                back()

                tab(t("Програми", "Apps"))
                settle()
                shot("$lang-light-apps")

                tab(t("Очищення", "Cleaning"))
                settle()
                if (Clean.junk == null) click(t("Знайти сміття", "Find junk"))
                waitFor("пошук сміття", 60_000) { Clean.junk != null && !Clean.running }
                if (lang == "uk") verifyJunk()
                settle()
                click(t("Тимчасові файли й журнали", "Temporary files and logs"))
                settle()
                shot("$lang-light-clean")

                if (lang == "uk") {
                    Store.chooseTheme(2)
                    tab(t("Захист", "Protection"))
                    settle()
                    shot("$lang-dark-protect")
                    tab(t("Очищення", "Cleaning"))
                    settle()
                    shot("$lang-dark-clean")
                    Store.chooseTheme(1)
                }
                tab(t("Захист", "Protection"))
                settle()
            }
        }
        Store.chooseLang(null)
        Store.chooseTheme(0)
    }

    private fun verifyFindings() {
        val r = Scan.result!!
        Log.d("Varta", "apps=${r.apps.size} danger=${r.danger} warn=${r.warn} device=${r.device.map { it.issue }} files=${r.files.map { it.path }}")
        val spy = r.apps.firstOrNull { it.pkg == "com.systemservice" } ?: error("приманку не знайдено серед програм")
        assertEquals("рівень приманки", Level.DANGER, spy.level)
        assertEquals("TheTruthSpy", spy.ioc?.name)
        for (f in listOf(Flag.ACCESSIBILITY, Flag.NOTIFICATIONS, Flag.DEVICE_ADMIN, Flag.SMS, Flag.MIC, Flag.HIDDEN_ICON, Flag.SIDELOADED, Flag.DISGUISED))
            assertTrue("приманка: немає позначки $f (є ${spy.flags})", f in spy.flags)
        assertTrue("APK приманки в «Завантаженнях» не знайдено: ${r.files}", r.files.any { it.path.endsWith("System_Service.apk") })
        // звичайні вбудовані програми не мають бути «небезпечними»
        val bad = r.apps.filter { it.level == Level.DANGER && it.pkg != "com.systemservice" }
        assertTrue("зайві тривоги: ${bad.map { it.pkg }}", bad.isEmpty())
    }

    private fun verifyJunk() {
        val j = Clean.junk!!
        Log.d("Varta", "junk=" + j.mapValues { (_, v) -> v.map { it.file.name } })
        val names = j.values.flatten().map { it.file.name }.toSet()
        for (n in listOf("upload.tmp", "debug.log", "EmptyFolder", "com.removed.game", "System_Service.apk", "trip.mp4"))
            assertTrue("сміття: не знайдено $n", n in names)
        assertTrue("великий файл не має бути вибраний", j[Junk.LARGE]!!.none { it.file.path in Clean.selected })
        assertTrue("тимчасові файли мають бути вибрані", j[Junk.TEMP]!!.all { it.file.path in Clean.selected })
    }

    private fun click(text: String) {
        compose.onAllNodesWithText(text, useUnmergedTree = true).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun tab(text: String) {
        compose.onAllNodes(hasText(text) and isSelectable()).onLast().performClick()
        compose.waitForIdle()
    }

    private fun back() {
        shell("input keyevent 4")
        settle()
    }

    private fun waitFor(what: String, ms: Long, ok: () -> Boolean) {
        val t0 = System.currentTimeMillis()
        while (!ok()) {
            if (System.currentTimeMillis() - t0 > ms) throw AssertionError("не дочекались: $what")
            Thread.sleep(300)
        }
    }

    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(1500)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        val bmp: Bitmap = inst.uiAutomation.takeScreenshot() ?: error("no screenshot for $name")
        val dir = File(ctx.filesDir, "screens").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun shell(cmd: String) {
        inst.uiAutomation.executeShellCommand(cmd).use { pfd ->
            java.io.FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
        }
    }
}
