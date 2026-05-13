[![Typing SVG](https://readme-typing-svg.demolab.com/?font=Fira+Code&size=24&duration=4500&pause=1000&color=1F2328&vCenter=true&random=true&height=25&lines=⤇Проходи+без+затруднений;⤇QR+на+рабочем+столе;⤇Автопродление+пропуска)](https://git.io/typing-svg)

# doorDuck

![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Glance](https://img.shields.io/badge/Jetpack-Glance-4285F4)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](./LICENSE)

## Скачать

[Latest Release](https://github.com/vgy789/doorDuck/releases/latest) | [Download APK](https://github.com/vgy789/doorDuck/releases/latest/download/doorDuck-latest.apk) | [Download SHA256](https://github.com/vgy789/doorDuck/releases/latest/download/doorDuck-latest.apk.sha256)

`doorDuck` — Android-приложение для School 21, которое получает QR для входа через Rocket.Chat API и показывает его:
- в приложении,
- в виджете на рабочем столе.

## Возможности

- Пошаговый init wizard (RU/EN) для первичной настройки.
- Проверка доступа и получение нового QR через Rocket.Chat API.
- Парсинг `expire on dd.MM.yyyy` из ответа бота и планирование автообновления к сроку истечения.
- Тап по виджету открывает приложение.
- Ручное обновление QR из приложения.

## Стек

- Kotlin + Jetpack Compose
- Jetpack Glance (App Widgets)
- WorkManager
- Retrofit + OkHttp + kotlinx.serialization
- AndroidX Security Crypto
- DataStore Preferences

## Лицензия

[MIT](./LICENSE)
