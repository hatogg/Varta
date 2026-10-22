package ua.x44to.varta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.x44to.varta.scan.Flag
import ua.x44to.varta.scan.IocHit
import ua.x44to.varta.scan.Level
import ua.x44to.varta.scan.Risk
import ua.x44to.varta.scan.Source

class RiskTest {
    private fun level(vararg f: Flag, ioc: IocHit? = null, trusted: Boolean = false, system: Boolean = false) =
        Risk.level(f.toSet(), ioc, trusted, system)

    @Test fun sources() {
        assertEquals(Source.STORE, Risk.source("com.android.vending", null, false))
        assertEquals(Source.OPEN_STORE, Risk.source("org.fdroid.fdroid", null, false))
        assertEquals(Source.FILE, Risk.source("com.google.android.packageinstaller", "org.telegram.messenger", false))
        assertEquals(Source.OTHER_APP, Risk.source("com.apkpure.aegon", null, false))
        assertEquals(Source.ADB, Risk.source(null, null, false))
        assertEquals(Source.PREINSTALLED, Risk.source(null, null, true))
        assertTrue(Risk.sideloaded(Source.FILE))
        assertFalse(Risk.sideloaded(Source.STORE))
    }

    @Test fun knownStalkerwareIsDangerEvenFromAStore() {
        val hit = IocHit("TheTruthSpy", false, IocHit.By.PACKAGE)
        assertEquals(Level.DANGER, level(ioc = hit))
        assertEquals(Level.DANGER, level(ioc = hit, system = true))
        // людина свідомо довіряє (наприклад, власний протиугінний Cerberus)
        assertEquals(Level.OK, level(ioc = hit, trusted = true))
    }

    @Test fun watchwareIsOnlyWorthChecking() {
        assertEquals(Level.WARN, level(ioc = IocHit("Life360", true, IocHit.By.PACKAGE)))
    }

    @Test fun typicalSpywareShape() {
        // спецможливості в програми з файлу — як у банківських троянів
        assertEquals(Level.WARN, level(Flag.ACCESSIBILITY, Flag.SIDELOADED))
        assertEquals(Level.WARN, level(Flag.DEVICE_ADMIN, Flag.SIDELOADED))
        // прихована програма з файлу, що читає SMS
        assertEquals(Level.WARN, level(Flag.HIDDEN_ICON, Flag.SMS, Flag.SIDELOADED))
        // «Оновлення системи» з файлу з мікрофоном і камерою
        assertEquals(Level.WARN, level(Flag.DISGUISED, Flag.SIDELOADED, Flag.MIC, Flag.CAMERA))
    }

    @Test fun ordinaryAppsStayCalm() {
        // месенджер із Play Маркету: мікрофон, камера, контакти, місце
        assertEquals(Level.OK, level(Flag.MIC, Flag.CAMERA, Flag.CONTACTS, Flag.LOCATION))
        // менеджер паролів зі спецможливостями з магазину
        assertEquals(Level.OK, level(Flag.ACCESSIBILITY, Flag.OVERLAY))
        // багато прав, але з магазину — лише «до уваги»
        assertEquals(Level.INFO, level(Flag.SMS, Flag.CALL_LOG, Flag.CONTACTS, Flag.MIC))
        // системні програми не оцінюємо за правами
        assertEquals(Level.OK, level(Flag.ACCESSIBILITY, Flag.SMS, Flag.MIC, system = true))
    }

    @Test fun disguisedNames() {
        assertTrue(Risk.disguised("System Service", Source.FILE))
        assertTrue(Risk.disguised("Оновлення системи", Source.ADB))
        assertTrue(Risk.disguised("Wi-Fi Helper", Source.OTHER_APP))
        assertFalse(Risk.disguised("System Service", Source.STORE))
        assertFalse(Risk.disguised("Копійка", Source.FILE))
    }
}
