# Варта

<p>
  <a href="https://github.com/hatogg/Varta/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/hatogg/Varta?style=flat-square&label=release&color=2747C4"></a>
  <a href="https://github.com/hatogg/Varta/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/hatogg/Varta/build.yml?branch=main&style=flat-square&label=build"></a>
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white">
  <img alt="Без інтернету" src="https://img.shields.io/badge/інтернет-не%20потрібен-FFC857?style=flat-square">
  <a href="LICENSE"><img alt="MIT" src="https://img.shields.io/badge/license-MIT-2747C4?style=flat-square"></a>
</p>

<p><b>Українська</b> &nbsp;|&nbsp; <a href="README.en.md">English</a></p>

**Варта** перевіряє Android-телефон на шпигунські програми й бекдори, показує програми з підозрілими правами й небезпечні налаштування, а ще чистить пам'ять від сміття. Працює без інтернету: нічого не надсилає й не показує реклами.

<p>
  <a href="https://github.com/hatogg/Varta/releases/latest"><img alt="Завантажити APK" src="https://img.shields.io/badge/Завантажити-APK-2747C4?style=for-the-badge&logo=android&logoColor=white"></a>
</p>

<p align="center">
  <img src="docs/screens/uk-light-protect.png" width="24%" alt="Знайдено загрози: шпигунська програма і її інсталятор">
  <img src="docs/screens/uk-light-detail.png" width="24%" alt="Що вміє знайдена програма і що робити">
  <img src="docs/screens/uk-light-clean.png" width="24%" alt="Очищення пам'яті">
  <img src="docs/screens/uk-dark-protect.png" width="24%" alt="Темна тема">
</p>

<sub>Справжні скриншоти: GitHub Actions запускає «Варту» в емуляторі Android 14 разом із нешкідливою тестовою програмою, що має назву пакета відомої стежилки, і перевіряє, що її знайдено.</sub>

## Що перевіряє

| | |
| --- | --- |
| 🕵️ **Шпигунки й бекдори** | Кожну програму звіряє з відкритою базою [Echap](https://github.com/AssoEchap/stalkerware-indicators): 170 відомих стежилок і RAT-ів (TheTruthSpy, Cerberus, AhMyth, AndroRat та інші) і 25 програм стеження — за назвою пакета, сертифікатом підпису й хешем файлу. |
| 📦 **Інсталятори APK** | Файли APK у «Завантаженнях» і папках месенджерів — ще до того, як ви їх відкриєте. |
| ⚠️ **Підозрілу поведінку** | Спецможливості, права адміністратора, доступ до сповіщень і SMS, мікрофон і місцезнаходження у фоні, програми без значка, «системні» назви в програм не з магазину. |
| 🔐 **Налаштування** | Блокування екрана, давність оновлень безпеки, налагодження USB і Wi‑Fi, root, чужі сертифікати (перехоплення трафіку), VPN і проксі. |
| 🔔 **Щоденно** | За бажанням раз на день перевіряє нові й оновлені програми і сповіщає, якщо знайде загрозу. |

Кожна знахідка пояснена простими словами, поруч — кнопки: «Видалити», «Вимкнути спецможливості», «Вимкнути адміністратора», «Налаштування програми» або «Довіряю», якщо програму ви ставили свідомо (батьківський контроль, сімейний локатор).

## Що чистить

Кеш усіх програм (системним вікном Android 11+), порожні папки, тимчасові файли й журнали, мініатюри галереї, залишки видалених програм і вже встановлені APK. Великі файли (від 100 МБ) лише показує — що видаляти, вирішуєте ви. Перед видаленням завжди питає.

## Чесно про межі

Без root жодна програма на Android не може зазирнути у файли інших програм, тож «Варта» не замінює антивірус із хмарною базою: зовсім нового невідомого вірусу в базі може не бути. Її сила — у відомих стежилках і в тому, що вона показує, хто з програм отримав небезпечні права. Для певності перевірте телефон ще й Play Захистом (кнопка є на головному екрані).

Якщо вам здається, що за вами стежить близька людина, видалення стежилки може її насторожити. Спершу порадьтеся: **102** — поліція, **15 47** або **116 123** — безкоштовна анонімна допомога.

## Встановлення

1. Завантажте `Varta-1.0.apk` з [релізу](https://github.com/hatogg/Varta/releases/latest) на телефоні.
2. Відкрийте файл і дозвольте встановлення з цього джерела.
3. Для очищення й перевірки APK дайте «Доступ до всіх файлів», а щоб бачити кеш кожної програми — «Доступ до статистики використання». Обидва дозволи необов'язкові.

## Дозволи

`QUERY_ALL_PACKAGES` — бачити всі програми, щоб не пропустити шпигунку; `MANAGE_EXTERNAL_STORAGE` — файли APK і сміття; `PACKAGE_USAGE_STATS` — розмір кешу; `REQUEST_DELETE_PACKAGES` — системне вікно видалення; `ACCESS_NETWORK_STATE` — чи ввімкнено VPN; `POST_NOTIFICATIONS` — щоденна перевірка. Дозволу на інтернет немає — збірка в CI падає, якщо він з'явиться.

## Збірка

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Kotlin, Jetpack Compose, Material 3, WorkManager. Базу оновлює `tools/update_ioc.py` (дані Echap, [CC BY 4.0](NOTICE.md)).

## Ліцензія

[MIT](LICENSE). База загроз — © Echap, CC BY 4.0.
