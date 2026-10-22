package ua.x44to.varta.scan

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Відбитки й власники сертифікатів, хеші файлів — без Android, щоб перевіряти тестами. */
object Certs {
    fun hex(b: ByteArray, upper: Boolean = true): String {
        val s = StringBuilder(b.size * 2)
        for (x in b) s.append(String.format(if (upper) "%02X" else "%02x", x))
        return s.toString()
    }

    fun sha1(der: ByteArray) = hex(MessageDigest.getInstance("SHA-1").digest(der))

    fun sha256(f: File): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(f).use { inp ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = inp.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        hex(md.digest(), upper = false)
    }.getOrNull()

    /** Пара (O, CN) власника сертифіката. */
    fun subject(der: ByteArray): Pair<String?, String?> {
        val dn = runCatching {
            (CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate)
                .subjectX500Principal.name
        }.getOrNull() ?: return null to null
        return attr(dn, "O") to attr(dn, "CN")
    }

    /** Значення атрибута з імені RFC 2253 («CN=Тест\, ТОВ,O=Acme,C=UA»), з урахуванням екранованих ком. */
    fun attr(dn: String, key: String): String? {
        var i = 0
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        while (i < dn.length) {
            val c = dn[i]
            when {
                c == '\\' && i + 1 < dn.length -> { cur.append(dn[i + 1]); i += 2; continue }
                c == ',' -> { parts += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
            i++
        }
        parts += cur.toString()
        for (p in parts) {
            val eq = p.indexOf('=')
            if (eq > 0 && p.substring(0, eq).trim().equals(key, ignoreCase = true)) return p.substring(eq + 1).trim()
        }
        return null
    }
}
