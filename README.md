[![Typing SVG](https://readme-typing-svg.demolab.com/?font=Fira+Code&size=24&duration=4500&pause=1000&color=1F2328&vCenter=true&random=true&height=25&lines=⤇Проходи+без+затруднений;⤇QR+на+рабочем+столе;⤇Автопродление+пропуска)](https://git.io/typing-svg)

# doorDuck

![Android](https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-14%2B-000000?logo=apple&logoColor=white)
![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](./LICENSE)

`doorDuck` — мультиплатформенное приложение для School 21, которое получает QR для входа через Rocket.Chat API, сохраняет его локально и показывает в приложении и виджете на Android / iOS.

## Скачать

[Latest Release](https://github.com/vgy789/doorDuck/releases/latest) | [Download APK](https://github.com/vgy789/doorDuck/releases/latest/download/doorDuck-latest.apk) | [Download SHA256](https://github.com/vgy789/doorDuck/releases/latest/download/doorDuck-latest.apk.sha256)

Сейчас в релизах публикуется Android APK. iOS-версия собирается из исходников.

## Возможности

- Пошаговый мастер первичной настройки на RU/EN.
- Проверка доступа к Rocket.Chat и валидация учётных данных.
- Автообновление QR перед истечением срока действия.
- Масштабируемый виджет на Android и iOS.

## Архитектура

Проект разделён на три основных модуля:

- `shared` — Kotlin Multiplatform модуль с общей бизнес-логикой, Compose Multiplatform UI для iOS, моделями, строками и platform abstraction.
- `app` — Android-приложение на Jetpack Compose, Android widget, WorkManager, Android storage/network integration.
- `iosApp` — Xcode-проект, который использует `shared` framework и содержит iOS app + WidgetKit extension.

## Технологии

- Kotlin Multiplatform
- Compose Multiplatform
- Jetpack Compose
- WidgetKit
- Jetpack Glance
- WorkManager
- Ktor / Darwin client
- Retrofit + OkHttp
- kotlinx.serialization
- DataStore Preferences
- AndroidX Security Crypto

## Лицензия

[MIT](./LICENSE)
