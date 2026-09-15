# Local LLM Chat (Android)

Локальный чат с GGUF-моделями на устройстве. Архитектура: UI -> ViewModel -> InferenceEngine / RagEngine.

## Структура
- `app/` — UI (Jetpack Compose) и логика
- `llamacpp/` — JNI-модуль llama.cpp (пока заглушка, подключение по образцу SmolChat-Android)

## Сборка

### Вариант А: GitHub Actions (рекомендуется, не нужен ПК)
1. Создай репозиторий на GitHub, залей эти файлы.
2. Actions -> "Build Debug APK" -> Run workflow.
3. Скачай artifact `app-debug.apk` на планшет и установи.

### Вариант Б: Android Studio
Открой папку проекта, дождись Gradle Sync, Build > Build APK.

## Дорожная карта
- [x] Скелет проекта, UI чата
- [x] Интерфейс InferenceEngine (+ заглушка DummyEngine)
- [x] Интерфейс RagEngine (+ заглушка на SQLite)
- [x] Голос: SpeechEngine + TtsEngine (системные, офлайн)
- [ ] Whisper-модель вместо системного распознавания речи
- [ ] Подключить llama.cpp JNI (образец: github.com/shubham0204/SmolChat-Android)
- [ ] RAG: эмбеддинги через llama.cpp embedding endpoint
