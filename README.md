[![Typing SVG](https://readme-typing-svg.demolab.com/?font=Fira+Code&size=24&duration=4500&pause=1000&color=1F2328&vCenter=true&random=true&height=25&lines=⤇Проходи+без+затруднений;⤇QR+на+рабочем+столе;⤇Автопродление+пропуска)](https://git.io/typing-svg)

# doorDuck


![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-14%2B-000000?logo=apple&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](./LICENSE)

`doorDuck` показывает и обновляет QR-пропуск для кампусов Школы 21 в приложении и виджетах Android/iOS.

> [!IMPORTANT]
> `doorDuck` не связан с АНО «Школа 21», Сбером или Rocket.Chat. Пользователь отвечает за соблюдение правил организации, договоров и законодательства.

## Скачать

[Latest Release](https://github.com/vgy789/doorDuck/releases/latest) | [Android APK](https://github.com/vgy789/doorDuck/releases/latest/download/doorDuck-latest.apk) | iOS собирается из исходников

<table>
  <tr>
    <td align="center">
      <img src="docs/images/screenshots/authorization.jpg" alt="Authorization screen" width="280" />
    </td>
    <td align="center">
      <img src="docs/images/screenshots/main_page.jpg" alt="Main page screen" width="280" />
    </td>
    <td align="center">
      <img src="docs/images/screenshots/home_screen.jpg" alt="Main page screen" width="280" />
    </td>
  </tr>
</table>

## Возможности

- Масштабируемые виджеты Android/iOS с обновлением QR по истечении срока.
- Повышение яркости при показе кода.
- Русский и английский языки.
- Доступ для студентов, участников интенсива и сотрудников.

## Приватность и безопасность

Запросы идут с устройства напрямую в Rocket.Chat, без сервера автора. Подробнее: [конфиденциальность](./.github/PRIVACY.md) и [безопасность](./.github/SECURITY.md).

## Секреты сборки

Адресов API нет в исходниках. Для сборки заполните `secrets.properties` по примеру `secrets.properties.example`.

## Архитектура

- `shared` — общая логика, модели, ресурсы, платформенные абстракции и Compose UI для iOS.
- `app` — Android-приложение и виджет, сеть, хранилище и WorkManager.
- `iosApp` — iOS-приложение и WidgetKit extension на основе `shared`.

## Технологии

Kotlin и Compose Multiplatform, Jetpack Compose, WidgetKit, Glance, WorkManager, Ktor, Darwin client, Retrofit, OkHttp, kotlinx.serialization, DataStore и AndroidX Security Crypto.

## Лицензия

[MIT](./LICENSE)
