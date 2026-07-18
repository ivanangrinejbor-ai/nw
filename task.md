# Task List

## Phase 1 — Инфраструктура
- [x] RuntimeMutationTracker.kt — новый singleton
- [x] CreateObjectAction.kt — регистрировать мутации
- [x] AssignScriptsAction.kt — регистрировать мутации
- [x] ImportScriptAction.kt — регистрировать мутации
- [x] StageLifeCycleController.java — reset tracker в stageCreate()

## Phase 2 — Fix duplicate detection
- [x] NeoScriptImporter.java — исправить scriptSignature() и логику SKIP/REPLACE

## Phase 3 — Loading overlay
- [x] strings.xml (EN) — project_reload_status + project_reload_facts array
- [x] strings.xml (RU) — аналог
- [x] ProjectReloadOverlay.kt — новый класс

## Phase 4 — Reload при возврате из Stage
- [x] ProjectActivity.kt — onActivityResult(REQUEST_START_STAGE) + reloadProjectFromDisk()
- [x] SpriteActivity.java — onActivityResult(REQUEST_START_STAGE) + reload

## Phase 5 — ScriptFragment refresh
- [x] ScriptFragment.java — onResume() проверка и пересоздание adapter при smene sprite

## Phase 6 — Tests
- [x] NeoScriptStressTest.java — 60 StartScript stress test

## Phase 7 — Verification
- [/] ./gradlew testCatroidDebugUnitTest --tests "*NeoScript*" (Running...)
- [ ] ./gradlew assembleDebug
