package ua.x44to.varta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.x44to.varta.scan.Certs
import ua.x44to.varta.scan.DeviceScanner
import java.io.File
import java.time.LocalDate

class CertsTest {
    @Test fun distinguishedNames() {
        val dn = "CN=Тест\\, ТОВ,OU=Mobile,O=Acme Inc,L=Kyiv,C=UA"
        assertEquals("Тест, ТОВ", Certs.attr(dn, "CN"))
        assertEquals("Acme Inc", Certs.attr(dn, "O"))
        assertEquals("UA", Certs.attr(dn, "c"))
        assertNull(Certs.attr(dn, "ST"))
    }

    @Test fun hashes() {
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", Certs.sha1("abc".toByteArray()))
        val f = File.createTempFile("varta", ".bin").apply { writeText("abc"); deleteOnExit() }
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Certs.sha256(f))
    }

    @Test fun patchAge() {
        assertEquals(31L, DeviceScanner.patchAge("2026-09-20", LocalDate.of(2026, 10, 21)))
        assertNull(DeviceScanner.patchAge("", LocalDate.of(2026, 10, 21)))
        assertNull(DeviceScanner.patchAge(null))
    }
}
