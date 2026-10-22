package ua.x44to.varta.scan

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * База відомих стежилок і програм стеження з відкритого проєкту Echap
 * (github.com/AssoEchap/stalkerware-indicators, CC BY 4.0). Оновлюється скриптом tools/update_ioc.py.
 */
class Ioc(json: String) {
    private class App(val name: String, val watch: Boolean)

    private val apps: List<App>
    private val pkg: Map<String, Int>
    private val cert: Map<String, Int>
    private val sha: Map<String, Int>
    private val org: Map<String, Int>
    private val cn: List<Pair<Regex, Int>>
    val date: String
    val commit: String

    init {
        val o = Json.parseToJsonElement(json).jsonObject
        apps = o["apps"]!!.jsonArray.map {
            val a = it.jsonObject
            App(a["n"]!!.jsonPrimitive.content, a["t"]!!.jsonPrimitive.content == "w")
        }
        fun map(k: String): Map<String, Int> = (o[k] as? JsonObject)?.mapValues { it.value.jsonPrimitive.int } ?: emptyMap()
        pkg = map("pkg")
        cert = map("cert")
        sha = map("sha256")
        org = map("certOrg")
        cn = o["certCn"]?.jsonArray?.mapNotNull {
            val p = it.jsonArray
            runCatching { Regex(p[0].jsonPrimitive.content) to p[1].jsonPrimitive.int }.getOrNull()
        } ?: emptyList()
        date = o["date"]?.jsonPrimitive?.content ?: ""
        commit = o["commit"]?.jsonPrimitive?.content ?: ""
    }

    val spyCount get() = apps.count { !it.watch }
    val watchCount get() = apps.count { it.watch }

    private fun hit(i: Int?, by: IocHit.By) = i?.let { apps.getOrNull(it) }?.let { IocHit(it.name, it.watch, by) }

    /**
     * @param certSha1 відбитки сертифікатів підпису (SHA-1, великими літерами без двокрапок)
     * @param certSubjects власники сертифікатів: пари (O, CN)
     * @param apkSha256 хеш файлу APK, якщо порахований
     */
    fun match(pkgName: String?, certSha1: List<String>, certSubjects: List<Pair<String?, String?>>, apkSha256: String?): IocHit? {
        apkSha256?.let { h -> hit(sha[h.lowercase()], IocHit.By.APK_HASH)?.let { return it } }
        for (c in certSha1) hit(cert[c.uppercase()], IocHit.By.CERTIFICATE)?.let { return it }
        pkgName?.let { p -> hit(pkg[p], IocHit.By.PACKAGE)?.let { return it } }
        for ((o, n) in certSubjects) {
            o?.let { hit(org[it], IocHit.By.CERT_OWNER)?.let { h -> return h } }
            if (n != null) for ((re, i) in cn) if (re.containsMatchIn(n)) hit(i, IocHit.By.CERT_OWNER)?.let { return it }
        }
        return null
    }

    companion object {
        @Volatile private var cached: Ioc? = null

        fun get(ctx: Context): Ioc = cached ?: synchronized(this) {
            cached ?: Ioc(ctx.assets.open("ioc.json").bufferedReader().use { it.readText() }).also { cached = it }
        }
    }
}
