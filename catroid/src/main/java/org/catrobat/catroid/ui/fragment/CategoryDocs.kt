/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.ui.fragment

import android.content.Context
import org.catrobat.catroid.R

/**
 * Provides per-category documentation: a long description + 2-3 examples
 * of the first blocks in each category. Used by the "What"/"Чаво" help button.
 */
object CategoryDocs {

    data class Doc(val title: String, val description: String, val examples: String)

    fun getDoc(categoryName: String, context: Context): Doc {
        val res = context.resources
        val lang = res.configuration.locales[0].language
        val isRu = lang == "ru"

        return when (categoryName) {
            context.getString(R.string.category_motion) -> Doc(
                title = if (isRu) "Движение" else "Motion",
                description = if (isRu)
                    "Категория Движение управляет перемещением спрайтов по сцене. " +
                        "Здесь находятся блоки для установки координат, поворота, скольжения, " +
                        "отскока от краёв и управления слоями. Большинство блоков работает " +
                        "с координатами X (горизонталь) и Y (вертикаль)."
                else
                    "The Motion category controls how sprites move around the stage. " +
                        "It includes blocks for setting position, rotation, gliding, " +
                        "bouncing off edges, and managing layers. Most blocks work with " +
                        "X (horizontal) and Y (vertical) coordinates.",
                examples = if (isRu)
                    "• Place At — поставить спрайт в точку (X, Y)\n" +
                        "• Move N Steps — переместить на N шагов в текущем направлении\n" +
                        "• Turn Left / Right — повернуть на заданный угол"
                else
                    "• Place At — set sprite position to (X, Y)\n" +
                        "• Move N Steps — move forward N steps in current direction\n" +
                        "• Turn Left / Right — rotate by a given angle"
            )

            context.getString(R.string.category_looks) -> Doc(
                title = if (isRu) "Образы" else "Looks",
                description = if (isRu)
                    "Категория Образы управляет внешним видом спрайтов: смена костюмов, " +
                        "размер, прозрачность, яркость, цветовые эффекты, показ текстовых " +
                        "облачков и диалогов. Также здесь есть блоки для работы с фоном сцены."
                else
                    "The Looks category controls the visual appearance of sprites: " +
                        "changing costumes, size, transparency, brightness, color effects, " +
                        "speech bubbles, and dialogs. It also includes blocks for scene backgrounds.",
                examples = if (isRu)
                    "• Set Look — сменить образ спрайта по имени\n" +
                        "• Set Size To — установить размер в процентах\n" +
                        "• Show / Hide — показать или скрыть спрайт"
                else
                    "• Set Look — change sprite costume by name\n" +
                        "• Set Size To — set size as a percentage\n" +
                        "• Show / Hide — make sprite visible or invisible"
            )

            context.getString(R.string.category_sound) -> Doc(
                title = if (isRu) "Звук" else "Sound",
                description = if (isRu)
                    "Категория Звук позволяет воспроизводить звуковые файлы, музыку, " +
                        "синтезировать речь и управлять громкостью. Звуки можно запускать " +
                        "по имени файла или из медиатеки проекта."
                else
                    "The Sound category lets you play audio files, music, synthesize " +
                        "speech, and control volume. Sounds can be triggered by filename " +
                        "or from the project's media library.",
                examples = if (isRu)
                    "• Play Sound — воспроизвести звуковой файл\n" +
                        "• Stop All Sounds — остановить все звуки\n" +
                        "• Set Volume To — установить громкость в процентах"
                else
                    "• Play Sound — play an audio file\n" +
                        "• Stop All Sounds — stop all playing sounds\n" +
                        "• Set Volume To — set volume as a percentage"
            )

            context.getString(R.string.category_control) -> Doc(
                title = if (isRu) "Управление" else "Control",
                description = if (isRu)
                    "Категория Управление содержит блоки для управления потоком " +
                        "выполнения скриптов: циклы, условия, ожидание, клонирование, " +
                        "остановка скриптов и переключение сцен."
                else
                    "The Control category contains blocks that manage script flow: " +
                        "loops, conditions, waits, cloning, stopping scripts, and scene switching.",
                examples = if (isRu)
                    "• Forever — повторять блоки бесконечно\n" +
                        "• If Then — выполнить блоки если условие истинно\n" +
                        "• Wait — приостановить скрипт на заданное время"
                else
                    "• Forever — repeat blocks endlessly\n" +
                        "• If Then — run blocks when a condition is true\n" +
                        "• Wait — pause the script for a given time"
            )

            context.getString(R.string.category_event) -> Doc(
                title = if (isRu) "События" else "Events",
                description = if (isRu)
                    "Категория События определяет когда скрипты начинают выполняться: " +
                        "по нажатию на зелёный флажок, касанию экрана, получению сообщения, " +
                        "изменению фона или системным событиям."
                else
                    "The Events category determines when scripts start running: " +
                        "on green flag tap, touch, message received, background change, " +
                        "or system events.",
                examples = if (isRu)
                    "• When Started — при запуске проекта\n" +
                        "• When Tapped — при нажатии на спрайт\n" +
                        "• When I Receive — при получении сообщения"
                else
                    "• When Started — when the project starts\n" +
                        "• When Tapped — when the sprite is tapped\n" +
                        "• When I Receive — when a message is received"
            )

            context.getString(R.string.category_data) -> Doc(
                title = if (isRu) "Данные" else "Data",
                description = if (isRu)
                    "Категория Данные управляет переменными, списками и хранилищем " +
                        "на устройстве. Переменные хранят числа и строки, списки — " +
                        "наборы значений. Данные можно сохранять и читать из файлов."
                else
                    "The Data category manages variables, lists, and device storage. " +
                        "Variables hold numbers and strings; lists hold collections of " +
                        "values. Data can be saved to and read from files.",
                examples = if (isRu)
                    "• Set Variable — присвоить значение переменной\n" +
                        "• Change Variable — изменить переменную на число\n" +
                        "• Add Item To List — добавить элемент в список"
                else
                    "• Set Variable — assign a value to a variable\n" +
                        "• Change Variable — change variable by a number\n" +
                        "• Add Item To List — add an item to a list"
            )

            context.getString(R.string.category_device) -> Doc(
                title = if (isRu) "Устройство" else "Device",
                description = if (isRu)
                    "Категория Устройство даёт доступ к функциям телефона/планшета: " +
                        "вибрация, датчики, камера, микрофон, текст в речь, таймеры, " +
                        "дата/время, геолокация и системная информация."
                else
                    "The Device category provides access to phone/tablet features: " +
                        "vibration, sensors, camera, microphone, text-to-speech, " +
                        "timers, date/time, geolocation, and system info.",
                examples = if (isRu)
                    "• Timer — получить значение таймера\n" +
                        "• Current — текущие дата/время (год, месяц, час)\n" +
                        "• Text To Speech — произнести текст вслух"
                else
                    "• Timer — get the timer value\n" +
                        "• Current — current date/time (year, month, hour)\n" +
                        "• Text To Speech — speak text aloud"
            )

            context.getString(R.string.category_pen) -> Doc(
                title = if (isRu) "Перо" else "Pen",
                description = if (isRu)
                    "Категория Перо позволяет рисовать на сцене линии, фигуры и точки. " +
                        "Опускаете перо — спрайт оставляет след при движении. " +
                        "Можно менять цвет, толщину и прозрачность пера."
                else
                    "The Pen category lets sprites draw lines, shapes, and dots on the stage. " +
                        "Put the pen down — the sprite leaves a trail when moving. " +
                        "You can change pen color, size, and transparency.",
                examples = if (isRu)
                    "• Pen Down — опустить перо (начать рисование)\n" +
                        "• Pen Up — поднять перо (остановить рисование)\n" +
                        "• Set Pen Color — установить цвет пера"
                else
                    "• Pen Down — start drawing\n" +
                        "• Pen Up — stop drawing\n" +
                        "• Set Pen Color — set the pen color"
            )

            context.getString(R.string.category_file) -> Doc(
                title = if (isRu) "Файлы" else "Files",
                description = if (isRu)
                    "Категория Файлы управляет чтением и записью файлов на устройстве. " +
                        "Создание папок, копирование, перемещение, удаление файлов, " +
                        "работа с ZIP-архивами и запись переменных в файлы."
                else
                    "The Files category manages reading and writing files on the device. " +
                        "Creating folders, copying, moving, deleting files, " +
                        "working with ZIP archives, and saving variables to files.",
                examples = if (isRu)
                    "• Create Folder — создать папку\n" +
                        "• Delete File — удалить файл\n" +
                        "• Write Variable To File — сохранить переменную в файл"
                else
                    "• Create Folder — create a new folder\n" +
                        "• Delete File — delete a file\n" +
                        "• Write Variable To File — save a variable to a file"
            )

            context.getString(R.string.category_json) -> Doc(
                title = if (isRu) "JSON" else "JSON",
                description = if (isRu)
                    "Категория JSON позволяет создавать, разбирать и изменять данные " +
                        "в формате JSON. Полезно для обмена данными с веб-сервисами " +
                        "и хранения структурированной информации."
                else
                    "The JSON category lets you create, parse, and modify data " +
                        "in JSON format. Useful for exchanging data with web services " +
                        "and storing structured information.",
                examples = if (isRu)
                    "• Create JSON Object — создать объект JSON\n" +
                        "• Get JSON Value By Key — получить значение по ключу\n" +
                        "• Add JSON Entry — добавить пару ключ-значение"
                else
                    "• Create JSON Object — create a JSON object\n" +
                        "• Get JSON Value By Key — get value by key\n" +
                        "• Add JSON Entry — add a key-value pair"
            )

            context.getString(R.string.category_neoscript) -> Doc(
                title = if (isRu) "NeoScript" else "NeoScript",
                description = if (isRu)
                    "Категория NeoScript позволяет создавать переиспользуемые модули " +
                        "скриптов (.neoscript) и импортировать их в другие проекты. " +
                        "Можно создавать объекты и назначать им скрипты из файлов."
                else
                    "The NeoScript category lets you create reusable script modules " +
                        "(.neoscript files) and import them into other projects. " +
                        "You can create objects and assign scripts from files.",
                examples = if (isRu)
                    "• Import Script — импортировать .neoscript в объект\n" +
                        "• Create Object — создать пустой спрайт в сцене\n" +
                        "• Assign Scripts — назначить .neoscript объекту"
                else
                    "• Import Script — import a .neoscript into an object\n" +
                        "• Create Object — create a blank sprite in a scene\n" +
                        "• Assign Scripts — assign a .neoscript to an object"
            )

            context.getString(R.string.category_threed) -> Doc(
                title = if (isRu) "3D" else "3D",
                description = if (isRu)
                    "Категория 3D управляет трёхмерными объектами: позиция, вращение, " +
                        "масштаб, материалы, освещение и камера. Работает с 3D-сценой " +
                        "через ThreeDManager."
                else
                    "The 3D category controls 3D objects: position, rotation, " +
                        "scale, materials, lighting, and camera. Works with the 3D scene " +
                        "via ThreeDManager.",
                examples = if (isRu)
                    "• Set 3D Position — установить позицию X, Y, Z\n" +
                        "• Set 3D Rotation — повернуть объект по осям\n" +
                        "• Set 3D Scale — масштабировать объект"
                else
                    "• Set 3D Position — set position X, Y, Z\n" +
                        "• Set 3D Rotation — rotate object on axes\n" +
                        "• Set 3D Scale — scale the object"
            )

            context.getString(R.string.category_internet) -> Doc(
                title = if (isRu) "Интернет" else "Internet",
                description = if (isRu)
                    "Категория Интернет позволяет отправлять HTTP-запросы (GET, POST, PUT, DELETE), " +
                        "скачивать файлы и работать с WebSocket. Полезно для интеграции " +
                        "с API и веб-сервисами."
                else
                    "The Internet category lets you send HTTP requests (GET, POST, PUT, DELETE), " +
                        "download files, and work with WebSockets. Useful for integrating " +
                        "with APIs and web services.",
                examples = if (isRu)
                    "• Web Request — отправить GET-запрос\n" +
                        "• Post Web Request — отправить POST-запрос\n" +
                        "• Download File — скачать файл по URL"
                else
                    "• Web Request — send a GET request\n" +
                        "• Post Web Request — send a POST request\n" +
                        "• Download File — download a file from URL"
            )

            context.getString(R.string.category_transitions) -> Doc(
                title = if (isRu) "Переходы" else "Transitions",
                description = if (isRu)
                    "Категория Переходы создаёт анимированные эффекты между сценами " +
                        "или внутри одной сцены. Затемнение, масштабирование, перемещение " +
                        "и другие визуальные переходы."
                else
                    "The Transitions category creates animated effects between scenes " +
                        "or within a scene. Fade, scale, move, and other visual transitions.",
                examples = if (isRu)
                    "• Fade Transition — плавное затемнение\n" +
                        "• Scale Transition — масштабирование\n" +
                        "• Move Transition — перемещение"
                else
                    "• Fade Transition — smooth fade effect\n" +
                        "• Scale Transition — scale animation\n" +
                        "• Move Transition — move animation"
            )

            else -> Doc(
                title = categoryName,
                description = if (isRu)
                    "В этой категории находятся блоки для различных действий. " +
                        "Нажмите на блок чтобы добавить его в скрипт."
                else
                    "This category contains blocks for various actions. " +
                        "Tap a block to add it to your script.",
                examples = if (isRu)
                    "Выберите блок из списка ниже."
                else
                    "Select a block from the list below."
            )
        }
    }
}
