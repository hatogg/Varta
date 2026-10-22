package ua.x44to.varta

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.x44to.varta.data.L
import ua.x44to.varta.data.bytes

class StringsTest {
    @After fun reset() { L.override = null }

    @Test fun ukrainianPlurals() {
        L.override = "uk"
        assertEquals("1 програма", L.apps(1))
        assertEquals("3 програми", L.apps(3))
        assertEquals("5 програм", L.apps(5))
        assertEquals("11 програм", L.apps(11))
        assertEquals("21 програма", L.apps(21))
        assertEquals("112 програм", L.apps(112))
        assertEquals("1,5 ГБ", bytes(1610612736))
        assertEquals("340 МБ", bytes(340L * 1024 * 1024))
    }

    @Test fun english() {
        L.override = "en"
        assertEquals("1 app", L.apps(1))
        assertEquals("2 apps", L.apps(2))
        assertEquals("12 KB", bytes(12 * 1024))
    }
}
