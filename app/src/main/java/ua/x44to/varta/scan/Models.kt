package ua.x44to.varta.scan

/** Наскільки все погано: від «небезпечно» до «усе гаразд». */
enum class Level { DANGER, WARN, INFO, OK }

/** Що вміє або як потрапила на телефон програма. Вага — внесок у загальну оцінку ризику. */
enum class Flag(val weight: Int) {
    ACCESSIBILITY(3),   // увімкнені спецможливості: бачить і натискає все на екрані
    DEVICE_ADMIN(2),    // адміністратор пристрою: може блокувати, стирати, не дає себе видалити
    NOTIFICATIONS(2),   // читає всі сповіщення, зокрема коди з SMS і повідомлення месенджерів
    SMS(2),
    CALL_LOG(1),
    MIC(1),
    CAMERA(1),
    LOCATION_BG(2),     // місцезнаходження навіть у фоні
    LOCATION(0),
    CONTACTS(1),
    OVERLAY(1),         // показ поверх інших програм (фальшиві вікна входу)
    INSTALL_APPS(1),    // може ставити інші програми
    USAGE_ACCESS(1),    // бачить, якими програмами ви користуєтеся
    HIDDEN_ICON(2),     // немає значка в меню програм
    SIDELOADED(1),      // не з магазину програм
    DISGUISED(2),       // назва під системну програму, але встановлена з файлу
    OLD_TARGET(1),      // зроблена для дуже старого Android: отримує права без запитів
}

/** Звідки встановили програму. */
enum class Source { STORE, OPEN_STORE, FILE, OTHER_APP, ADB, PREINSTALLED }

data class Install(val source: Source, val installer: String?, val initiator: String?)

/** Збіг із базою відомих стежилок. */
data class IocHit(val name: String, val watchware: Boolean, val by: By) {
    enum class By { PACKAGE, CERTIFICATE, APK_HASH, CERT_OWNER }
}

data class AppReport(
    val pkg: String,
    val label: String,
    val system: Boolean,
    val version: String?,
    val install: Install,
    val firstInstall: Long,
    val lastUpdate: Long,
    val flags: Set<Flag>,
    val ioc: IocHit?,
    val score: Int,
    val level: Level,
    val trusted: Boolean,
    val apkSize: Long,
)

/** Що не так з налаштуваннями телефону. */
enum class DeviceIssue { NO_SCREEN_LOCK, OLD_PATCH, VERY_OLD_PATCH, USB_DEBUG, WIFI_DEBUG, ROOT, USER_CA, VPN, PROXY, BOOTLOADER }

data class DeviceFinding(val issue: DeviceIssue, val level: Level, val detail: String = "")

/** APK-файл у пам'яті телефону, який збігся з базою. */
data class ApkFinding(val path: String, val size: Long, val pkg: String?, val label: String?, val ioc: IocHit)

data class ScanResult(
    val at: Long,
    val apps: List<AppReport>,
    val device: List<DeviceFinding>,
    val files: List<ApkFinding>,
    val filesChecked: Boolean,
) {
    val danger get() = apps.count { it.level == Level.DANGER } + files.size + device.count { it.level == Level.DANGER }
    val warn get() = apps.count { it.level == Level.WARN } + device.count { it.level == Level.WARN }
    val worst: Level
        get() = when {
            danger > 0 -> Level.DANGER
            warn > 0 -> Level.WARN
            device.any { it.level == Level.INFO } -> Level.INFO
            else -> Level.OK
        }
}
