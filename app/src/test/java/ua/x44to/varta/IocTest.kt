package ua.x44to.varta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.x44to.varta.scan.Ioc
import ua.x44to.varta.scan.IocHit
import java.io.File

class IocTest {
    private val ioc = Ioc(File("src/main/assets/ioc.json").readText())

    @Test fun databaseIsThere() {
        assertTrue("шпигунок у базі: ${ioc.spyCount}", ioc.spyCount >= 100)
        assertTrue(ioc.watchCount >= 10)
        assertTrue(ioc.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test fun byPackage() {
        val h = ioc.match("com.systemservice", emptyList(), emptyList(), null)
        assertNotNull(h)
        assertEquals("TheTruthSpy", h!!.name)
        assertFalse(h.watchware)
        assertEquals(IocHit.By.PACKAGE, h.by)
    }

    @Test fun byCertificateEvenWithAnotherPackage() {
        val h = ioc.match("org.example.renamed", listOf("31A6ECECD97CF39BC4126B8745CD94A7C30BF81C"), emptyList(), null)
        assertEquals("TheTruthSpy", h?.name)
        assertEquals(IocHit.By.CERTIFICATE, h?.by)
    }

    @Test fun byFileHash() {
        val h = ioc.match(null, emptyList(), emptyList(), "2f3bc9ebe8c249f0416f5b0367f34b4b8f6aaa7d351dc5b4912fe2558491b6b7")
        assertNotNull(h)
        assertEquals(IocHit.By.APK_HASH, h!!.by)
    }

    @Test fun publicTestKeysAreNotIndicators() {
        // AOSP testkey: ним підписані службові пакети емуляторів і програми кастомних прошивок
        assertNull(ioc.match("com.android.cts.ctsshim", listOf("61ED377E85D386A8DFEE6B864BD85B0BFAA5AF81"), emptyList(), null))
    }

    @Test fun watchware() {
        val h = ioc.match("com.life360.android.safetymapd", emptyList(), emptyList(), null)
        assertTrue(h!!.watchware)
    }

    @Test fun ordinaryAppsDontMatch() {
        for (p in listOf("org.telegram.messenger", "com.whatsapp", "com.google.android.youtube", "ua.privatbank.ap24", "com.android.chrome"))
            assertNull(p, ioc.match(p, listOf("0000000000000000000000000000000000000000"), listOf("Google LLC" to "Android"), null))
    }
}
