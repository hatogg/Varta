package ua.x44to.varta.ui

import ua.x44to.varta.data.L
import ua.x44to.varta.data.t
import ua.x44to.varta.scan.DeviceIssue
import ua.x44to.varta.scan.Flag
import ua.x44to.varta.scan.IocHit
import ua.x44to.varta.scan.Source

/** Зрозумілі пояснення: що знайшли і чим це загрожує. */
object Texts {
    fun flagTitle(f: Flag) = when (f) {
        Flag.ACCESSIBILITY -> t("Спецможливості", "Accessibility")
        Flag.DEVICE_ADMIN -> t("Адміністратор пристрою", "Device admin")
        Flag.NOTIFICATIONS -> t("Читає сповіщення", "Reads notifications")
        Flag.SMS -> t("SMS", "SMS")
        Flag.CALL_LOG -> t("Журнал дзвінків", "Call log")
        Flag.MIC -> t("Мікрофон", "Microphone")
        Flag.CAMERA -> t("Камера", "Camera")
        Flag.LOCATION_BG -> t("Місцезнаходження у фоні", "Background location")
        Flag.LOCATION -> t("Місцезнаходження", "Location")
        Flag.CONTACTS -> t("Контакти", "Contacts")
        Flag.OVERLAY -> t("Поверх інших вікон", "Draws over apps")
        Flag.INSTALL_APPS -> t("Ставить програми", "Installs apps")
        Flag.USAGE_ACCESS -> t("Статистика використання", "Usage access")
        Flag.HIDDEN_ICON -> t("Без значка", "No icon")
        Flag.SIDELOADED -> t("Не з магазину", "Not from a store")
        Flag.DISGUISED -> t("Назва як у системної", "Looks like a system app")
        Flag.OLD_TARGET -> t("Для старого Android", "Built for old Android")
    }

    fun flagText(f: Flag) = when (f) {
        Flag.ACCESSIBILITY -> t(
            "Бачить усе на екрані й може сама натискати кнопки. Так працюють шпигунки й банківські віруси. Чесно це потрібно хіба що програмам для людей із порушеннями зору й деяким менеджерам паролів.",
            "Sees everything on screen and can tap buttons by itself. Spyware and banking trojans work this way. Honest uses are screen readers and some password managers.",
        )
        Flag.DEVICE_ADMIN -> t(
            "Може блокувати й стирати телефон і не дає себе просто видалити. Щоб видалити, спершу вимкніть їй права адміністратора.",
            "Can lock and wipe the phone and blocks normal uninstalling. Turn off its admin rights first, then uninstall.",
        )
        Flag.NOTIFICATIONS -> t(
            "Бачить усі сповіщення: повідомлення з месенджерів і коди з SMS.",
            "Sees every notification, including messenger texts and SMS codes.",
        )
        Flag.SMS -> t("Читає або надсилає SMS, зокрема коди підтвердження від банку.", "Reads or sends SMS, including bank confirmation codes.")
        Flag.CALL_LOG -> t("Бачить, кому і коли ви дзвонили.", "Sees who you called and when.")
        Flag.MIC -> t("Може записувати звук.", "Can record audio.")
        Flag.CAMERA -> t("Може фотографувати й знімати відео.", "Can take photos and videos.")
        Flag.LOCATION_BG -> t("Знає, де ви, навіть коли програма закрита.", "Knows where you are even when it's closed.")
        Flag.LOCATION -> t("Знає, де ви, поки нею користуєтесь.", "Knows where you are while you use it.")
        Flag.CONTACTS -> t("Бачить вашу телефонну книгу.", "Sees your contacts.")
        Flag.OVERLAY -> t(
            "Може показувати свої вікна поверх інших програм, наприклад фальшиву сторінку входу в банк.",
            "Can show its own windows over other apps, such as a fake bank login.",
        )
        Flag.INSTALL_APPS -> t("Має дозвіл встановлювати інші програми з файлів.", "Is allowed to install other apps from files.")
        Flag.USAGE_ACCESS -> t("Бачить, якими програмами і скільки ви користуєтесь.", "Sees which apps you use and for how long.")
        Flag.HIDDEN_ICON -> t("Її немає в меню програм, тож її легко не помітити.", "It isn't in the app drawer, so it's easy to miss.")
        Flag.SIDELOADED -> t(
            "Встановлена з файлу APK або іншою програмою, а не з магазину. Її ніхто не перевіряв.",
            "Installed from an APK file or by another app, not from a store. Nobody has reviewed it.",
        )
        Flag.DISGUISED -> t(
            "Називається як системна програма, але встановлена з файлу. Так часто ховаються шпигунки.",
            "Named like a system app but installed from a file. Spyware often hides this way.",
        )
        Flag.OLD_TARGET -> t(
            "Зроблена для Android 5 і старших, тому отримує права без запитань.",
            "Built for Android 5 or older, so it gets permissions without asking.",
        )
    }

    fun iocTitle(h: IocHit) = if (h.watchware) t("Програма стеження ${h.name}", "Monitoring app ${h.name}")
    else t("Шпигунська програма ${h.name}", "Stalkerware ${h.name}")

    fun iocText(h: IocHit) = if (h.watchware) t(
        "Відкрито показує комусь, де ви і що робите на телефоні: батьківський контроль, сімейні локатори. Якщо ставили її самі або знаєте про неї — усе гаразд, натисніть «Довіряю».",
        "Openly shows someone where you are and what you do on the phone: parental control, family locators. If you installed it or know about it, that's fine — tap “Trust”.",
    ) else t(
        "Ця програма є в базі відомих стежилок. Такі програми таємно читають листування, записують дзвінки й передають, де ви. Видаліть її. " +
            "Якщо її міг поставити хтось близький і вам загрожує небезпека, спершу порадьтеся: 102 — поліція, 15 47 або 116 123 — безкоштовна анонімна допомога.",
        "This app is in the database of known stalkerware. Such apps secretly read messages, record calls and report your location. Uninstall it. " +
            "If someone close to you may have installed it and you could be in danger, get advice first (stopstalkerware.org) — removing it may alert them.",
    )

    fun iocBy(b: IocHit.By) = when (b) {
        IocHit.By.PACKAGE -> t("Впізнано за назвою пакета", "Matched by package name")
        IocHit.By.CERTIFICATE -> t("Впізнано за сертифікатом підпису", "Matched by signing certificate")
        IocHit.By.APK_HASH -> t("Впізнано за хешем файлу", "Matched by file hash")
        IocHit.By.CERT_OWNER -> t("Впізнано за власником сертифіката", "Matched by certificate owner")
    }

    fun source(s: Source, installer: String?, initiator: String?) = when (s) {
        Source.STORE -> t("З магазину", "From a store") + (installer?.let { " $it" } ?: "")
        Source.OPEN_STORE -> t("З відкритого каталогу", "From an open-source catalog") + (installer?.let { " $it" } ?: "")
        Source.FILE -> t("З файлу APK", "From an APK file") + (initiator?.let { t(" через $it", " via $it") } ?: "")
        Source.OTHER_APP -> t("Встановила програма", "Installed by") + (installer?.let { " $it" } ?: "")
        Source.ADB -> t("Через комп'ютер (adb) або невідомо як", "Via a computer (adb) or unknown")
        Source.PREINSTALLED -> t("Вбудована виробником", "Preinstalled")
    }

    fun issueTitle(i: DeviceIssue) = when (i) {
        DeviceIssue.NO_SCREEN_LOCK -> t("Немає блокування екрана", "No screen lock")
        DeviceIssue.OLD_PATCH, DeviceIssue.VERY_OLD_PATCH -> t("Давно не було оновлень безпеки", "Security updates are old")
        DeviceIssue.USB_DEBUG -> t("Увімкнено налагодження USB", "USB debugging is on")
        DeviceIssue.WIFI_DEBUG -> t("Увімкнено налагодження через Wi‑Fi", "Wireless debugging is on")
        DeviceIssue.ROOT -> t("На телефоні є root", "The phone is rooted")
        DeviceIssue.USER_CA -> t("Встановлено чужий сертифікат", "A user certificate is installed")
        DeviceIssue.VPN -> t("Увімкнено VPN", "VPN is on")
        DeviceIssue.PROXY -> t("Налаштовано проксі", "A proxy is set")
        DeviceIssue.BOOTLOADER -> t("Розблокований завантажувач", "Unlocked bootloader")
    }

    fun issueText(i: DeviceIssue, detail: String) = when (i) {
        DeviceIssue.NO_SCREEN_LOCK -> t(
            "Будь-хто, хто візьме телефон, за хвилину поставить шпигунку. Увімкніть PIN-код, пароль або відбиток пальця.",
            "Anyone who picks up the phone can install spyware in a minute. Set a PIN, password or fingerprint.",
        )
        DeviceIssue.OLD_PATCH, DeviceIssue.VERY_OLD_PATCH -> t(
            "Останнє оновлення безпеки: ${patch(detail)}. Відомі дірки в системі не закриті. Перевірте, чи є оновлення системи.",
            "Last security update: ${patch(detail)}. Known holes in the system are open. Check for system updates.",
        )
        DeviceIssue.USB_DEBUG -> t(
            "Через кабель у цьому режимі на телефон можна поставити програму без вашого відома. Якщо ви не розробник, вимкніть.",
            "With this mode on, an app can be installed over a cable without you noticing. Turn it off unless you're a developer.",
        )
        DeviceIssue.WIFI_DEBUG -> t(
            "Хтось у тій самій мережі може підключитися до телефона. Вимкніть, якщо зараз цим не користуєтесь.",
            "Someone on the same network can connect to the phone. Turn it off if you're not using it right now.",
        )
        DeviceIssue.ROOT -> t(
            "Програми з root бачать і змінюють усе, і захист Android на них не діє. Якщо root ставили не ви — це дуже погано. Ознаки: $detail.",
            "Apps with root see and change everything, and Android's protection doesn't apply to them. If you didn't root it, that's very bad. Signs: $detail.",
        )
        DeviceIssue.USER_CA -> t(
            "З таким сертифікатом можна читати ваш зашифрований інтернет-трафік: паролі, листування. Буває на робочих телефонах. Якщо не знаєте, звідки він, видаліть.\n\n$detail",
            "With such a certificate someone can read your encrypted traffic: passwords, messages. Common on work phones. If you don't know where it came from, remove it.\n\n$detail",
        )
        DeviceIssue.VPN -> t(
            "Увесь інтернет іде через VPN-сервер. Якщо це ваш VPN — усе гаразд." + vpnList(detail),
            "All traffic goes through a VPN server. If it's your VPN, that's fine." + vpnList(detail),
        )
        DeviceIssue.PROXY -> t(
            "Інтернет іде через сервер $detail. Так перехоплюють трафік. Якщо налаштовували не ви — вимкніть проксі в налаштуваннях Wi‑Fi.",
            "Traffic goes through $detail. This is how traffic gets intercepted. If you didn't set it, turn the proxy off in Wi‑Fi settings.",
        )
        DeviceIssue.BOOTLOADER -> t(
            "Систему на телефоні можна підмінити. Це нормально, якщо ви самі ставили іншу прошивку.",
            "The phone's system can be replaced. That's fine if you installed a custom ROM yourself.",
        )
    }

    private fun vpnList(d: String) = if (d.isBlank()) "" else t(" VPN-програми на телефоні: $d.", " VPN apps on the phone: $d.")

    private fun patch(p: String): String {
        val parts = p.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull()
        val m = parts.getOrNull(1)?.toIntOrNull()
        return if (y != null && m != null && m in 1..12) L.monthYear(y, m) else p
    }
}
