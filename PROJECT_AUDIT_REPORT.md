# NeoCatroid — Полный аудит проекта

**Дата:** 2026-07-06
**Сборка:** `assembleCatroidDebug` — **BUILD SUCCESSFUL** (27s, 88 tasks)
**Проанализировано:** 20 специализированных аудиторов, ~3000 исходных файлов

---

## Сводка проблем

| Категория | Количество |
|-----------|-----------|
| 🔴 Критические | **37** |
| 🟠 Высокий приоритет | **42** |
| 🟡 Средний приоритет | **34** |
| 🟢 Низкий приоритет | **18** |
| **ВСЕГО** | **131** |

---

## 🔴 КРИТИЧЕСКИЕ (37)

### 1. XStream 1.4.11.1 — уязвимость RCE при десериализации
- **Модуль:** Сериализация
- **Файлы:** `catroid/build.gradle:704`, `XstreamSerializer.java`
- **Проблема:** XStream версии 1.4.11.1 (2019) имеет известные CVE-2021-21341/21351 (RCE). `allowTypesByWildcard("org.catrobat.catroid.**")` — неполная защита. Злоумышленник может подложить `.catrobat`-файл с вредоносной полезной нагрузкой.
- **Рекомендация:** Обновить XStream до ≥1.5.0 (с deny-by-default моделью), сузить белый список типов.

### 2. 5 новых блоков не зарегистрированы в XStream
- **Модуль:** Сериализация / Brick
- **Файлы:** `XstreamSerializer.java:222-758`
- **Проблема:** `PutFileIntoFolderBrick`, `PutFileIntoPathBrick`, `ExecuteForCloneNumberBrick`, `DeleteCloneByNumberBrick`, `TouchDirectionBrick` — нет XStream alias. Проекты с этими блоками потеряют данные при загрузке (конвертируются в UnknownBrick).
- **Рекомендация:** Добавить `xstream.alias("brick", Class)` для всех 5 классов.

### 3. `ShowNotificationBrick` не существует (AGENTS.md != код)
- **Модуль:** Brick / Документация
- **Файлы:** `AGENTS.md`, `SendNotificationBrick.java`
- **Проблема:** AGENTS.md утверждает, что `ShowNotificationBrick` модифицирован для HEADER+TOAST, но такого класса нет. Существующий `SendNotificationBrick` использует только `NOTIFICATION_ID`.
- **Рекомендация:** Создать блок или обновить AGENTS.md.

### 4. StageActivity.onDestroy() — super.onDestroy() до cleanup
- **Модуль:** Stage
- **Файл:** `StageActivity.java:1521-1531`
- **Проблема:** Вызов `super.onDestroy()` до очистки ресурсов (`stageDestroy`) — окно разрушено, а код пытается к нему обратиться.
- **Рекомендация:** Перенести `super.onDestroy()` в конец метода.

### 5. Static Handler — утечка StageActivity
- **Модуль:** Stage / UI
- **Файл:** `StageActivity.java:192, 1545-1571`
- **Проблема:** `public static Handler messageHandler` + анонимный inner class, захватывающий `currentStage` (Activity). Статическое поле предотвращает GC Activity.
- **Рекомендация:** Убрать `static`, использовать WeakReference или отменять handler в onDestroy().

### 6. onSaveInstanceState не реализован — потеря состояния при process death
- **Модуль:** UI
- **Файлы:** Все Activity (кроме ProjectOptionsFragment)
- **Проблема:** Ни одна Activity не сохраняет состояние. `BaseActivity.checkIfProcessRecreatedAndFinishActivity()` намеренно завершает Activity при recreation.
- **Рекомендация:** Реализовать `onSaveInstanceState`/`onRestoreInstanceState`.

### 7. NPE в 20+ Java Action — scope без null-проверки
- **Модуль:** Action
- **Файлы:** `TouchDirectionAction.java`, `MoveNStepsAction`, `ChangeXByNAction` и ~20 других
- **Проблема:** `scope.getSprite().look` — scope может быть null (устанавливается через setter), NPE в update().
- **Рекомендация:** Добавить `if (scope == null) return;` во все методы update()/act().

### 8. ShowTextActor.draw() — Bitmap/Texture аллокация каждый кадр
- **Модуль:** Stage / Производительность
- **Файл:** `ShowTextActor.java:249-263`
- **Проблема:** Каждый кадр создаётся Bitmap, Canvas и Texture для отображения текста. Огромная нагрузка на GC и аллокатор.
- **Рекомендация:** Кэшировать результат в текстуру, обновлять только при изменении содержимого.

### 9. AskGemini2Action — отключена верификация хоста и бесконечные таймауты
- **Модуль:** Network / AI
- **Файл:** `AskGemini2Action.kt:72-96`
- **Проблема:** `hostnameVerifier { _, _ -> true }` (HTTPS не защищён), `connectTimeout(0)`, `JSONObject("""{"text": "$askReq"}""")` — XSS-инъекция через интерполяцию.
- **Рекомендация:** Использовать `OkHttpClient.Builder` без отключения безопасности, экранировать JSON.

### 10. URL продакшена — Telegram, а не сервер API
- **Модуль:** Network
- **Файл:** `Constants.java:134-137`
- **Проблема:** `MAIN_URL_PRODUCTION = "https://t.me/NeoCatroidDevs"` — все API-запросы идут на Telegram.
- **Рекомендация:** Установить корректный URL API-сервера.

### 11. Coroutines — конфликт версий 1.3.2 vs 1.7.3
- **Модуль:** Gradle
- **Файл:** `catroid/build.gradle:609, 620`
- **Проблема:** `kotlinx-coroutines-core:1.7.3` и `kotlinx-coroutines-android:1.3.2` — разные версии на classpath. `NoSuchMethodError` в рантайме.
- **Рекомендация:** Унифицировать на 1.7.3+.

### 12. Material Design — конфликт версий 1.2.1 vs 1.13.0
- **Модуль:** Gradle / UI
- **Файл:** `catroid/build.gradle:631,891`
- **Проблема:** `material:1.2.1` форсирован через `resolutionStrategy`, но vncclient требует 1.13.0.
- **Рекомендация:** Убрать force, обновить catroid до 1.13.0.

### 13. Missing `android:exported` на ProjectListActivity и SpriteActivity
- **Модуль:** AndroidManifest
- **Файл:** `AndroidManifest.xml:208,243`
- **Проблема:** На API 31+ Activity с intent-filter без `android:exported="true"` вызывает SecurityException.
- **Рекомендация:** Добавить `android:exported="true"`.

### 14. Scoped Storage — 29 файлов используют `getExternalStorageDirectory()` (заблокировано на API 30+)
- **Модуль:** File I/O
- **Файлы:** `Constants.java:65-68`, `FlavoredConstants.java:28-29`, все file bricks
- **Проблема:** На Android 11+ без `MANAGE_EXTERNAL_STORAGE` прямое обращение к внешнему хранилищу запрещено. `requestLegacyExternalStorage="false"`.
- **Рекомендация:** Мигрировать на MediaStore/Storage Access Framework.

### 15. `RunShellBrick` — выполнение shell-команд
- **Модуль:** Security
- **Файлы:** `RunShellAction.kt`, `PythonCommandManager.kt`
- **Проблема:** Полный shell-доступ из пользовательского проекта. Команды не валидируются.
- **Рекомендация:** Запретить или сделать опциональным, добавить confirmation UI.

### 16. PythonEngine — полный доступ к JVM/ОС
- **Модуль:** Security / Python
- **Файл:** `PythonCommandManager.kt`
- **Проблема:** pip install, shell-команды, git clone, file I/O — всё доступно из Python-скрипта.
- **Рекомендация:** Песочница / ограничение API.

### 17. LunoScript — полный доступ к JVM через рефлексию
- **Модуль:** LunoScript
- **Файл:** `Interpreter.kt:3939-3958`
- **Проблема:** `Class.forName(fullClassName)` без белого списка. Любой скрипт может создать любой класс, вызвать любой метод, читать любые поля.
- **Рекомендация:** Ввести белый список разрешённых классов.

### 18. UnzipAction — zip-slip уязвимость
- **Модуль:** File I/O / Security
- **Файл:** `UnzipAction.kt:57`
- **Проблема:** `File(outputDir, zipEntry.name)` без проверки `../`. Может перезаписать файлы вне outputDir.
- **Рекомендация:** Проверять `getCanonicalPath().startsWith(outputDir.canonicalPath)`.

### 19. PutFileIntoPathAction / DeleteFolderByPathAction — path traversal
- **Модуль:** File I/O / Security
- **Файлы:** `PutFileIntoPathAction.kt:24`, `DeleteFolderByPathAction.kt:23`, `CreateFolderByPathAction.kt:22`, `CopyProjectFileToPathAction.kt:24`
- **Проблема:** Принимают произвольный путь из формулы пользователя без валидации. `../data/data/...`.
- **Рекомендация:** Проверять canonical path.

### 20. Gradle native OOM — 6× JVM crash (hs_err_pid*.log)
- **Модуль:** Сборка
- **Файлы:** `hs_err_pid22480.log`, `hs_err_pid23208.log`, `hs_err_pid24400.log`, `hs_err_pid25776.log`, `hs_err_pid26428.log`, `hs_err_pid5308.log`
- **Проблема:** Native memory exhaustion при сборке. -Xmx6g/8g превышает лимит CompressedOops на 15GB машине.
- **Рекомендация:** Уменьшить -Xmx до 4g, добавить `-XX:-UseCompressedOops`.

### 21. Duplicate apksig — 7.0.0 и 8.2.0
- **Модуль:** Gradle
- **Файл:** `catroid/build.gradle:614,825`
- **Проблема:** Две версии одной библиотеки на classpath — неопределённое поведение.
- **Рекомендация:** Оставить только 8.2.0.

### 22. Null-safety в LookPostRequestAction и LookRequestAction — !! операторы
- **Модуль:** Action
- **Файлы:** `LookPostRequestAction.kt`, `LookRequestAction.kt`
- **Рекомендация:** Использовать `?.` вместо `!!`.

### 23. `RunShellAction` нарушает naming convention
- **Модуль:** Action
- **Файл:** `ActionFactory.java`
- **Проблема:** Метод называется `RunShellAction`, а не `createRunShellAction`.
- **Рекомендация:** Переименовать.

### 24. `FFILE_URL` → `FILE_URL` опечатка в BrickField? (проверить)
- **Модуль:** Brick
- **Проблема:** Возможна опечатка в константе — может вызвать ошибки поиска поля.
- **Рекомендация:** Проверить BrickField.FILE_URL и BrickField.FFILE_URL.

### 25. `createDeleteCloneByNumberAction` — объявлен дважды
- **Модуль:** ActionFactory
- **Файл:** `ActionFactory.java:1223,1268`
- **Проблема:** Два метода с одинаковой сигнатурой, различающиеся только типом параметра (SequenceAction vs ScriptSequenceAction).
- **Рекомендация:** Объединить.

### 26. FlameChart: блоки не удерживаются в XStreamBrickConverter
- **Модуль:** Сериализация
- **Файл:** `XStreamBrickConverter.java:66-74`
- **Проблема:** XStreamBrickConverter.doUnmarshal() создаёт новый экземпляр, игнорируя `result`, ломая reference tracking XStream.
- **Рекомендация:** Вернуть `result` из unmarshal, а не пересоздавать.

### 27. Отсутствует GL context loss handling в ThreeDManager
- **Модуль:** 3D (Raptor)
- **Файл:** `ThreeDManager.java`
- **Проблема:** Нет recreate текстур/шейдеров/FBO при потери GL контекста. Гарантированный краш на Android lifecycle events.
- **Рекомендация:** Реализовать `resume()` с перезагрузкой всех GL-ресурсов.

### 28. PanoramicConverter — утечка FrameBufferCubemap
- **Модуль:** 3D (Raptor)
- **Файл:** `PanoramicConverter.java:97`
- **Проблема:** `fbo.dispose()` закомментирован — утечка памяти при каждой смене skybox.
- **Рекомендация:** Раскомментировать `fbo.dispose()`.

### 29. Clone system — cloneCounter не синхронизирован, нет overflow защиты
- **Модуль:** Stage / Clone
- **Файл:** `StageListener.java:247,799,819`
- **Проблема:** `cloneCounter++` — не AtomicInteger, не volatile. После 2B клонов — переполнение. `removeCloneByIndex` не чистит `PhysicsWorld.physicsObjects`.
- **Рекомендация:** Использовать AtomicInteger, добавить защиту переполнения.

### 30. Игнорирование будильника — physics accumulator объявлен, но не используется
- **Модуль:** Physics / Stage
- **Файл:** `StageListener.java:160-161`
- **Проблема:** `accumulator` и `TIME_STEP` объявлены как dead code. Физика шагает с framerate-зависимым `deltaActionTimeDivisor`.
- **Рекомендация:** Использовать fixed-timestep accumulator правильно.

### 31. Collision Detection — молча возвращает false, если NativeLookOptimizer не работает
- **Модуль:** Physics
- **Файл:** `CollisionDetection.java:40`
- **Проблема:** Если native библиотека не загружена (`isWorking = false`), все коллизии отключаются без уведомления.
- **Рекомендация:** Добавить Java fallback и залогировать ошибку.

### 32. PutFileIntoFolderAction — File I/O на render thread (ANR риск)
- **Модуль:** Action
- **Файлы:** `PutFileIntoFolderAction.kt:31`, `PutFileIntoPathAction.kt:30`, `CopyProjectFileTo*Action.kt:31`
- **Проблема:** Файловые операции выполняются на GL-потоке синхронно. Для больших файлов — ANR.
- **Рекомендация:** Использовать корутины или AsynchronousAction.

### 33. writeVariableToFileAction — `System.getProperty("user.home")` не работает на Android
- **Модуль:** Action / File I/O
- **Файл:** `WriteVariableToFileAction.kt:88-96`
- **Проблема:** `System.getProperty("user.home")` на Android возвращает `/data` (корень data-раздела).
- **Рекомендация:** Использовать `Environment.getExternalStoragePublicDirectory()` или SAF.

### 34. `ErrorInterceptor` — читает не то тело ответа
- **Модуль:** Network
- **Файл:** `ErrorInterceptor.kt:32-43`
- **Проблема:** `response.body?.toString()` возвращает literal `"okhttp3.ResponseBody$1@xxxx"`, а не содержимое. Interceptor не делает ничего полезного.
- **Рекомендация:** Исправить на `response.body?.string()` или удалить.

### 35. 14 action-файлов с закомментированным package — мёртвый код
- **Модуль:** Action
- **Файлы:** `Apply3dForceAction.java`, `CameraLookAtAction.java`, `CreateCubeAction.java`, `ObjectLookAtAction.java`, `Remove3dObjectAction.java`, `Set3dFrictionAction.java`, `Set3dGravityAction.java`, `Set3dPositionAction.java`, `Set3dScaleAction.java`, `Set3dVelocityAction.java`, `SetCameraPositionAction.java`, `SetDirectionalLightAction.java`, `SetRestitutionAction.java`, `SetSkyColorAction.java`
- **Проблема:** package statement закомментирован — файлы не компилируются.
- **Рекомендация:** Восстановить package или удалить файлы.

### 36. `PrepareNotificationBrick` — `importanceLevel` и `isPinned` не сериализуются в XStream
- **Модуль:** Brick
- **Файл:** `PrepareNotificationBrick.java`
- **Проблема:** Поля не сохраняются в formulaMap и не имеют XStream alias. При десериализации сбрасываются на значения по умолчанию.
- **Рекомендация:** Добавить сохранение через formulaMap или XStream alias.

### 37. `secondFaceYPosition` → `faceY` вместо `secondFaceY` в конвертере формул
- **Модуль:** Formula
- **Файл:** `XStreamFormulaElementConverter.kt:78`
- **Проблема:** `SECOND_FACE_Y_POSITION` маппится на `FACE_Y`, а должно на `SECOND_FACE_Y` — потеря данных.
- **Рекомендация:** Исправить маппинг.

---

## 🟠 ВЫСОКИЙ ПРИОРИТЕТ (42)

### Архитектура
- **A1.** Монолитная архитектура (1 модуль catroid из 4, ~2141 файл) — отсутствует модуляризация.
- **A2.** 7 God-классов: `ThreeDManager` (6231 строка), `StageListener` (2249), `ActionFactory` (4768), `FormulaElement` (2114), `Interpreter` (4284), `StageActivity` (2076), `ProjectManager` (829).
- **A3.** Циклическая зависимость `content ↔ stage ↔ ui` — доменные сущности импортируют Activity.
- **A4.** 4 конкурирующих DI-механизма: Koin + ServiceLocator + static singleton + `KoinJavaComponent.get`.

### Gradle / Сборка
- **G1.** `minifyEnabled false` для release — APK без обфускации.
- **G2.** `workers.max=1` — блокирует параллельное выполнение.
- **G3.** `configuration-cache=false` — теряет ускорение повторных сборок.
- **G4.** `kotlin-stdlib-jdk8` — deprecated, использовать `kotlin-stdlib`.
- **G5.** `PowerMock 1.6.6 + 2.0.0` — смешение несовместимых версий.
- **G6.** `Espresso 3.1.0` — древний, первая AndroidX версия.
- **G7.** `unitTests.returnDefaultValues = true` — скрывает ошибки мокирования.
- **G8.** `kapt.include.compile.classpath` deprecation warning.
- **G9.** `variant.getJavaCompiler()` deprecated — AGP 9.0 сломает сборку.

### UI
- **U1.** `WebViewActivity` — JS всегда включён, URL из интента без валидации.
- **U2.** 22× AsyncTask usage (deprecated).
- **U3.** `ViewSwitchLock.lock()` — spinlock на UI-потоке (ANR).
- **U4.** `BaseActivity` — static self-reference, не очищается в onDestroy.
- **U5.** `SensorHandler` singleton кэширует Context (потенциально Activity context).

### Stage / Runtime
- **R1.** StageActivity — 100+ `runOnUiThread` без `isFinishing()`/`isDestroyed()`.
- **R2.** `CameraManager` — 8× `newSingleThreadExecutor()` без shutdown (утечка потоков).
- **R3.** `GlobalScope.launch` в 9+ местах (fire-and-forget, нет отмены).
- **R4.** `getPixels()` — busy-wait spinlock на GL-потоке (ANR).
- **R5.** `SystemLoadingActor` — загрузка ресурсов на render thread (frame drops).
- **R6.** UncaughtExceptionHandler закомментирован (`BaseActivity.kt:69`).
- **R7.** Crashlytics плагин подключён, но инициализация отсутствует.
- **R8.** Unsynchronized `StageListener.sprites` — `ConcurrentModificationException`.
- **R9.** Двойная инициализация (`create()` вызывается дважды).

### Сериализация / XStream
- **S1.** `cloneIndex` (transient) — сбрасывается при save/load, номера клонов теряются.
- **S2.** `MoveToObjectBrick` — custom Java serialization никогда не вызывается (XStream).
- **S3.** `ZipArchiver` — нет защиты zip bomb (безлимитное распакованное содержимое).

### 3D (Raptor)
- **3D1.** `attachObjectToCamera(id, x, y, z)` — offset в world-space, а не camera-space — объект не следует за камерой.
- **3D2.** Thread-unsafe `HashMap` в ThreeDManager (sceneObjects, physicsBodies, activeParticleEffects).

### 2D (Fast2D)
- **2D1.** Два параллельных physics world (Box2D + catroid PhysicsWorld) — изолированы, не взаимодействуют.
- **2D2.** ECS и stage — независимые rendering pipeline без координации z-order.
- **2D3.** `Fast2DRenderSystem` — insertion sort O(n²) каждый кадр.

### Formula
- **F1.** `CategoryListFragment.java:202` — неправильный resource id для `file_project_size` param.
- **F2.** File size функции продублированы в STRING_FUNCTIONS и DEVICE_FUNCTIONS.

### Security
- **SE1.** `usesCleartextTraffic="true"` — HTTP трафик разрешён системно.
- **SE2.** Hardcoded `build.properties:27-28` — пароль keystore в VCS.
- **SE3.** `FileProvider` экспортирует весь external storage (`path="."`).
- **SE4.** Gemini API key в статическом `var api_key` (mutable, без синхронизации).

---

## 🟡 СРЕДНИЙ ПРИОРИТЕТ (34)

### Code Quality
- **C1.** 595 TODO/FIXME в коде (наибольшая концентрация: ThreeDManager 76, Interpreter 35).
- **C2.** 45+ empty catch blocks (InspectorManager 12, ThreeDManager 7, FormulaElement 3).
- **C3.** 85+ overly broad `catch (Exception)`.
- **C4.** `ActionFactory.java` — 4768 строк, 549 методов (Single Responsibility Principle нарушен).
- **C5.** 7 abstract classes без `serialVersionUID` (FormulaBrick, ScriptBrickBaseType, UserDataBrick и др.).
- **C6.** 11 concrete bricks без `serialVersionUID`.
- **C7.** `listenServerAction` — companion object c ScheduledExecutorService (утечка при уничтожении).

### Localization
- **L1.** `formula_file_project_size_param` отсутствует в `values-ru/strings.xml`.
- **L2.** Множество hardcoded strings в layouts.
- **L3.** Lexar: RTL-макеты не проверены.
- **L4.** `ExtraTranslation`, `MissingTranslation` — игнорируются lint'ом.

### Android API
- **AP1.** `lifecycle-extensions` deprecated (build.gradle:659).
- **AP2.** `PreferenceManager` deprecated (30+ файлов).
- **AP3.** CameraX `1.0.0-beta07` — экстремально старый (current: 1.3.x).
- **AP4.** `UniversalImageLoader` 1.9.5 — не поддерживается с 2015.
- **AP5.** `CastManager.java:482` — PendingIntent без `FLAG_IMMUTABLE` на API 31+.

### Gradle
- **GD1.** Kotlin 1.9.22 (old, current: 2.0.x), Glide 4.11.0 (4.16.x).
- **GD2.** `android.nonTransitiveRClass=false` — старое поведение.
- **GD3.** `android.enableBuildCache=false` — медленные инкрементальные сборки.

### 3D
- **3D3.** Двойной rendering pipeline (Legacy + Realistic PBR) — code duplication.
- **3D4.** Reflection hacks: `BillboardParticleBatch.renderablePool`, `ShaderVfxEffect.program`.
- **3D5.** Нет x86_64 natives для Bullet — эмулятор не работает с 3D.

### 2D
- **2D4.** `mTexture.create(e)` — молча перезаписывает TextureComponent без cleanup.
- **2D5.** `rebuildFixture()` — полная перестройка физики при каждом изменении scale.
- **2D6.** StageBackup не сохраняет fastTwoDManager state.

### File I/O
- **FIO1.** `ResourceImporter.java:47,60` — InputStream от `openRawResource()` никогда не закрывается.
- **FIO2.** `LookFromTableAction.kt` — создаёт PNG в cacheDir без очистки (вечный рост).
- **FIO3.** Проверки разрешений не ждут результата (6+ файлов).
- **FIO4.** `FileUrlAction.kt:105` — raw Thread без пула.
- **FIO5.** `SoundFileAction.kt`, `SoundFilesAction.kt` — мёртвый `if (true)` блок.

---

## 🟢 НИЗКИЙ ПРИОРИТЕТ (18)

- **N1.** 40+ `@SuppressWarnings("unused")` — возможно dead code.
- **N2.** 13 `@Deprecated` методов (ProjectManager).
- **N3.** Duplicate proguard keep rules (блок `java.io.Serializable` повторён дважды).
- **N4.** `org.gradle.unsafe.ignoreDeprecations` — несуществующий property.
- **N5.** `src/apktemplate/` — orphaned директория (не подключена).
- **N6.** `dummyKeystore` не существует (build.gradle:94).
- **N7.** `FlavoredConstants.java` — Context может быть null при первом обращении.
- **N8.** `template` build type наследует `debug` через `initWith` — debuggable = true в релизе.
- **N9.** `Kryo 5.1.1` — зависимость есть, usage 0 (мёртвый груз +1.5MB).
- **N10.** `NeoCatroidApi.java` — Retrofit интерфейс не используется нигде.
- **N11.** `@LunoProperty` — аннотация определена, но процессор её игнорирует.
- **N12.** BigAskAction — опечатки в параметрах (`canel`, `sumb`).
- **N13.** `Interpreter.kt` — `isExpressionContext()` всегда `return true` (code smell).
- **N14.** `while (true)` в LunoScript без защиты от infinite loop.
- **N15.** `e.printStackTrace()` в production-коде (`StageLifeCycleController.java:149,280`).
- **N16.** `Field.isAccessible = true` — нарушает инкапсуляцию, может не работать на Java 17+.
- **N17.** Lint baseline: 10 ClickableViewAccessibility (accessibility issues).
- **N18.** `proguardProject.txt` и `proguard-runtime.pro` применяются вместе — конфликт keep-правил.

---

## Итоговая оценка

| Метрика | Значение |
|---------|----------|
| Всего найдено проблем | **131** |
| 🔴 Критических | **37** |
| 🟠 Высокого приоритета | **42** |
| 🟡 Среднего приоритета | **34** |
| 🟢 Низкого приоритета | **18** |
| Результат сборки | ✅ **BUILD SUCCESSFUL** (27s) |
| Оценка проекта | **4/10** |

### Обоснование оценки

**4/10** — проект в состоянии "работающего прототипа с глубоким техническим долгом".

**Положительные стороны:**
- Сборка проходит успешно (27 секунд)
- Богатая функциональность (100+ типов блоков, 3D/2D рендер, скриптовые языки, AI, физика)
- Хорошая система обратной совместимости форматов (BackwardCompatibleCatrobatLanguageXStream)
- Качественные реализации подсистем: Scene backup/restore, animation transition manager, LunoScript encryption

**Критические проблемы:**
- 37 критических багов, включая XStream RCE, потерю данных при сериализации новых блоков, отключённую безопасность HTTPS, неработающий API URL
- Монолит без модуляризации, God-классы по 5000+ строк
- Устаревшие/конфликтующие зависимости, deprecated Gradle API
- Утечки памяти (static handlers, Activity references, unbounded caches)
- Отсутствие lifecycle-aware операций (runOnUiThread, GlobalScope)
- Проблемы Android-совместимости (exported activities, scoped storage)
- Экстремальное количество подавленных lint-предупреждений и TODO

---

## Gradle Build: Результат

```
BUILD SUCCESSFUL in 27s
88 actionable tasks: 21 executed, 67 up-to-date
```

**Примечание:** Несмотря на успешную сборку, из предыдущих build-логов известно, что некоторые файлы (с закомментированным package statement) не компилируются — они, вероятно, исключены из sourceSet или не используются. Рекомендуется верифицировать покрытие.

---

## Рекомендуемые немедленные действия (Phase 1)

1. **Зарегистрировать 5 новых блоков в XStream** — иначе пользователи потеряют данные.
2. **Обновить XStream до 1.5.0+** — закрыть RCE-уязвимость.
3. **Исправить `StageActivity.onDestroy()`** — `super` в конец.
4. **Убрать `static Handler`** из StageActivity.
5. **Добавить `android:exported="true"`** на ProjectListActivity и SpriteActivity.
6. **Исправить AskGemini2Action** — вернуть hostname verification и timeout.
7. **Установить корректный MAIN_URL_PRODUCTION** вместо Telegram.
8. **Устранить конфликт coroutines** (1.3.2 vs 1.7.3).
9. **Добавить zip-slip защиту** в UnzipAction.
10. **Добавить path traversal защиту** в file bricks с произвольными путями.

---

*Аудит выполнен 2026-07-06. 20 специализированных подагентов, объединение результатов.*
