package ua.x44to.varta.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Налаштування й пам'ять «Варти»: тема, мова, довірені програми, кеш хешів APK. Усе — лише на телефоні. */
object Store {
    private lateinit var p: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }
    private val setSer = SetSerializer(String.serializer())
    private val mapSer = MapSerializer(String.serializer(), String.serializer())

    /** 0 — як у системі, 1 — світла, 2 — темна */
    var theme by mutableIntStateOf(0)
        private set

    /** null — як у системі, "uk" або "en" */
    var lang by mutableStateOf<String?>(null)
        private set

    var daily by mutableStateOf(false)
        private set

    var trusted by mutableStateOf<Set<String>>(emptySet())
        private set

    fun init(ctx: Context) {
        if (::p.isInitialized) return
        p = ctx.applicationContext.getSharedPreferences("varta", Context.MODE_PRIVATE)
        theme = p.getInt("theme", 0)
        lang = p.getString("lang", null)
        L.override = lang
        daily = p.getBoolean("daily", false)
        trusted = readSet("trusted")
    }

    fun chooseTheme(v: Int) {
        theme = v
        p.edit().putInt("theme", v).apply()
    }

    fun chooseLang(v: String?) {
        lang = v
        L.override = v
        p.edit().putString("lang", v).apply()
    }

    fun chooseDaily(v: Boolean) {
        daily = v
        p.edit().putBoolean("daily", v).apply()
    }

    fun trust(pkg: String, yes: Boolean) {
        trusted = if (yes) trusted + pkg else trusted - pkg
        p.edit().putString("trusted", json.encodeToString(setSer, trusted)).apply()
    }

    var lastScan: Long
        get() = p.getLong("lastScan", 0)
        set(v) = p.edit().putLong("lastScan", v).apply()

    /** Номер змін у списку програм (PackageManager.getChangedPackages) — для щоденної перевірки. */
    var pkgSequence: Int
        get() = p.getInt("seq", 0)
        set(v) = p.edit().putInt("seq", v).apply()

    /** Про які програми вже сповіщали, щоб не повторюватися. */
    var notified: Set<String>
        get() = readSet("notified")
        set(v) = p.edit().putString("notified", json.encodeToString(setSer, v)).apply()

    /** «пакет@час оновлення» → SHA-256 файлу APK: не рахуємо хеш тієї самої версії вдруге. */
    fun hashes(): MutableMap<String, String> =
        p.getString("hashes", null)?.let { runCatching { json.decodeFromString(mapSer, it) }.getOrNull() }?.toMutableMap() ?: mutableMapOf()

    fun saveHashes(m: Map<String, String>) = p.edit().putString("hashes", json.encodeToString(mapSer, m)).apply()

    private fun readSet(k: String): Set<String> =
        p.getString(k, null)?.let { runCatching { json.decodeFromString(setSer, it) }.getOrNull() } ?: emptySet()
}
