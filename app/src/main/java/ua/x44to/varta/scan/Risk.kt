package ua.x44to.varta.scan

/**
 * Оцінка ризику — окремо від Android, щоб перевіряти правила звичайними тестами.
 *
 * Небезпечно: збіг із базою шпигунських програм.
 * Варто перевірити: програма стеження з бази (батьківський контроль тощо) або поєднання, типове для шпигунок
 * і банківських троянів: спецможливості чи адміністратор у програми не з магазину, приховану програму з
 * доступом до SMS/мікрофона, або просто дуже багато прав.
 * До уваги: широкі права, але нічого підозрілого в тому, звідки програма.
 */
object Risk {
    val STORES = setOf(
        "com.android.vending", "com.google.android.feedback",
        "com.sec.android.app.samsungapps", "com.huawei.appmarket", "com.hihonor.appmarket",
        "com.xiaomi.market", "com.xiaomi.mipicks", "com.xiaomi.discover",
        "com.heytap.market", "com.oppo.market", "com.bbk.appstore", "com.vivo.appstore",
        "com.amazon.venezia", "com.aurora.store", "ru.vk.store", "com.transsion.phoenix",
    )
    val OPEN_STORES = setOf(
        "org.fdroid.fdroid", "org.fdroid.basic", "com.looker.droidify", "com.machiav3lli.fdroid",
        "dev.imranr.obtainium", "org.obtainium.obtainium", "com.github.androidacy.obtainium",
    )
    /** Системні вікна «Встановити програму?» — отже, програму поставили з файлу APK. */
    val FILE_INSTALLERS = setOf(
        "com.google.android.packageinstaller", "com.android.packageinstaller",
        "com.samsung.android.packageinstaller", "com.miui.packageinstaller",
    )

    fun source(installer: String?, initiator: String?, system: Boolean): Source = when {
        installer in STORES -> Source.STORE
        installer in OPEN_STORES -> Source.OPEN_STORE
        installer in FILE_INSTALLERS -> Source.FILE
        installer != null -> Source.OTHER_APP
        system -> Source.PREINSTALLED
        else -> Source.ADB
    }

    fun sideloaded(s: Source) = s == Source.FILE || s == Source.OTHER_APP || s == Source.ADB

    private val SYSTEM_WORDS = Regex(
        "(?i)(system|service|servic|update|updater|security|settings|google|android|wi-?fi|sync|framework|battery|" +
            "системн|сервіс|служб|оновлен|налаштуван|безпек|обслугов|систем|сервис|обновлен|настройк)",
    )

    /** Назва схожа на системну («System Service», «Оновлення системи»), хоча програму поставили не з магазину. */
    fun disguised(label: String, source: Source) = sideloaded(source) && SYSTEM_WORDS.containsMatchIn(label)

    fun score(flags: Set<Flag>) = flags.sumOf { it.weight }

    fun level(flags: Set<Flag>, ioc: IocHit?, trusted: Boolean, system: Boolean): Level {
        if (trusted) return Level.OK
        if (ioc != null) return if (ioc.watchware) Level.WARN else Level.DANGER
        if (system) return Level.OK
        val side = Flag.SIDELOADED in flags
        val s = score(flags)
        val spyish = flags.any { it == Flag.SMS || it == Flag.MIC || it == Flag.CALL_LOG || it == Flag.NOTIFICATIONS || it == Flag.LOCATION_BG }
        return when {
            side && (Flag.ACCESSIBILITY in flags || Flag.DEVICE_ADMIN in flags) -> Level.WARN
            side && Flag.HIDDEN_ICON in flags && spyish -> Level.WARN
            Flag.DISGUISED in flags && s >= 4 -> Level.WARN
            s >= 9 -> Level.WARN
            s >= 5 -> Level.INFO
            else -> Level.OK
        }
    }
}
