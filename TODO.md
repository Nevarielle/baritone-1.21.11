# TODO: Обновление Baritone 1.19.4 → 1.21.11 (Fabric)

---

## СТАТУС (обновляется по ходу работы)

Ветка: `update/1.21.11-fabric`. Baseline 1.19.4 сохранён коммитом.

| Этап | Статус |
|---|---|
| Тулчейн (Gradle 8.12, unimined 1.4.1, JDK 21) | ✅ работает |
| `buildSrc` | ✅ компилируется |
| sourceset `api` | ✅ **компилируется чисто** |
| sourceset `schematica_api` | ✅ компилируется |
| sourceset `main` | 🔧 ~151 ошибка (см. ниже) |
| sourceset `launch` (миксины) | ⏳ не начат |
| Сборка jar / тест в игре | ⏳ не начат |

**JDK 21** лежит в `c:\baritone\jdk21\jdk-21.0.11+10` (портативный Temurin, вне репозитория).
Сборка: `JAVA_HOME=/c/baritone/jdk21/jdk-21.0.11+10 ./gradlew :compileJava`

### Оставшиеся кластеры в `main`
| Подсистема | Файлы | Характер |
|---|---|---|
| Рендеринг | IRenderer(40), PathRenderer(10), GuiClick(10), SelectionRenderer(4) | новый render pipeline, самый объёмный |
| Схематики/NBT | Litematica(32), Sponge(16), MCEdit(14), DefaultFormats(10) | NBT-геттеры теперь возвращают `Optional` |
| Ввод | PlayerMovementInput(32) | `Input` → `ClientInput` |
| Элитра | ElytraBehavior(24) | data components (фейерверки) |
| Инструменты | ToolSet(22) | `DiggerItem`/`TieredItem` удалены → `ToolMaterial` |
| Движение | MovementHelper(22) | `Material` удалён, `isPathfindable` |

---

## ⚠️ КАРТА ПЕРЕИМЕНОВАНИЙ 1.19.4 → 1.21.11 (проверено по jar)

Главное открытие: **Mojang переименовал `ResourceLocation` → `Identifier`** в официальных
маппингах. Ниже — всё, что подтверждено `javap` по реальному ремапнутому jar.

| Было (1.19.4) | Стало (1.21.11) |
|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` |
| `new ResourceLocation(s)` | `Identifier.parse(s)` / `withDefaultNamespace(s)` |
| `Registry.get(id)` | `Registry.getValue(id)` |
| `Entity.level` (поле) | `Entity.level()` |
| `Entity.isOnGround()` | `Entity.onGround()` |
| `getMinBuildHeight()` / `getMaxBuildHeight()` | `getMinY()` / `getMaxY()` |
| `Direction.getNormal()` | `Direction.getUnitVec3i()` |
| `new ClickEvent(Action.RUN_COMMAND, s)` | `new ClickEvent.RunCommand(s)` |
| `new HoverEvent(Action.SHOW_TEXT, c)` | `new HoverEvent.ShowText(c)` |
| `Inventory.selected` (поле) | `getSelectedSlot()` / `setSelectedSlot(i)` |
| `Inventory.items` (поле) | `getNonEquipmentItems()` |
| `ToastComponent` | `ToastManager` |
| `Minecraft.getToasts()` | `Minecraft.getToastManager()` |
| `Toast.render(PoseStack, ToastComponent, long)` | `update(...)` + `getWantedVisibility()` + `render(GuiGraphics, Font, long)` |
| `GuiGraphics.blit(...)` | `blitSprite(RenderPipelines.GUI_TEXTURED, id, ...)` |
| `com.mojang.blaze3d.platform.GlStateManager` | `com.mojang.blaze3d.opengl.GlStateManager` |
| `monster.Spider` / `monster.ZombifiedPiglin` | `monster.spider.Spider` / `monster.zombie.ZombifiedPiglin` |
| `chunk.ChunkStatus` | `chunk.status.ChunkStatus` |
| `LootTables` / `PredicateManager` | `ReloadableServerRegistries.Holder` |
| `Block.getLootTable()` → `Identifier` | → `Optional<ResourceKey<LootTable>>` |
| `LootContext.Builder` | `LootParams.Builder` |
| `Level` конструктор | потерял параметр `ChunkProgressListener` |
| `ServerLevel` конструктор | без `ChunkProgressListener`, добавлен `RandomSequences` |
| `world.level.material.Material` | **удалён** (нет замены; `canBeReplaced()`, `getFluidState()`) |
| `DiggerItem`/`PickaxeItem`/`SwordItem`/`TieredItem` | **удалены** → `ToolMaterial` + компоненты |
| `ItemStack.getTagElement(...)` | компоненты: `stack.get(DataComponents.X)` |
| `javax.annotation.*` | больше не транзитивно → нужен `jsr305` |

---

> Поэтапный план миграции. Отмечай пункты `[x]` по мере выполнения.
> Цель: рабочая Fabric-сборка под Minecraft **1.21.11**, Java **21**.
> Скоуп: **только Fabric**. Forge/NeoForge не трогаем (Forge под 1.21 нет; NeoForge — отдельная история, вне задачи).

---

## 0. Контекст и стратегия

Прыжок большой: 1.19.4 → 1.21.11 — это ~10 мажорных релизов Minecraft. Между ними
несколько крупных ломающих изменений (Java 21, data-components, переписанный
нетворкинг, приватный конструктор `ResourceLocation`, новый рендер-пайплайн).

**Рекомендуемая стратегия — прямой прыжок на 1.21.11** с починкой ошибок компиляции
по подсистемам (см. этап 6). Причина: проект на **mojmap** (человекочитаемые имена
Mojang), поэтому большинство ошибок видно прямо в IDE, а не через обфусцированные
имена. Пошаговый апгрейд (1.20.1 → 1.20.6 → 1.21.1 → …) — запасной вариант, если
какая-то подсистема (скорее всего рендер или кэш чанков) окажется слишком запутанной
для разбора «в лоб».

**Текущее состояние проекта (зафиксировано при анализе):**
- Сборка: **unimined 1.0.5** (НЕ Loom), Gradle **8.2**
- Minecraft 1.19.4, Java 17, Fabric loader 0.14.11
- Mappings: intermediary + mojmap + Parchment `2023.06.26`
- Mixin 0.8.5, ASM 9.3, нативная либа `dev.babbaj:nether-pathfinder:1.6`
- 18 миксинов в [src/launch/java/baritone/launch/mixins/](src/launch/java/baritone/launch/mixins/)
- ~194 .java-файла импортируют `net.minecraft`
- Fabric-модуль без Java-кода: точка входа — через миксин `MixinMinecraft`, entrypoints в
  [fabric.mod.json](fabric/src/main/resources/fabric.mod.json) пустые

---

## 1. Подготовка

- [ ] Сделать git-ветку/бэкап перед началом (проект сейчас **не** под git — стоит `git init` и закоммитить исходное состояние как точку отката).
- [ ] Убедиться, что установлен **JDK 21** и что Gradle его видит (`java -version`, toolchain).
- [ ] (Опционально) Скачать для справки декомпилированные исходники MC 1.21.11 через unimined `genSources`, чтобы сверять сигнатуры.

---

## 2. Инструментарий сборки

| Компонент | Сейчас | Цель (1.21.11) |
|---|---|---|
| Java | 17 | **21** |
| Gradle wrapper | 8.2 | **8.10+** (нужно для JDK 21 и свежего unimined) |
| unimined | 1.0.5 | **1.3.15 / 1.4.1** (последний, поддерживает 1.21.x) |
| Shadow plugin | 8.1.1 | проверить совместимость с Gradle 8.10+ (при проблемах → `io.github.goooler.shadow` или `com.gradleup.shadow`) |

- [ ] Обновить [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties): `gradle-8.10-bin.zip` (или новее).
- [ ] Обновить версию unimined в [buildSrc/build.gradle](buildSrc/build.gradle#L43) → `1.4.1`.
- [ ] Сверить с API unimined 1.4.x использование в [buildSrc/.../ProguardTask.java](buildSrc/src/main/java/baritone/gradle/task/ProguardTask.java) (`UniminedExtension`, `MinecraftConfig` — API мог измениться между 1.0.5 и 1.4.x).
- [ ] В [build.gradle](build.gradle#L42-L47): `java_version` = 21, `sourceCompatibility`/toolchain → 21.

---

## 3. Версии зависимостей ([gradle.properties](gradle.properties))

- [ ] `java_version=21`
- [ ] `minecraft_version=1.21.11`
- [ ] `fabric_version=0.18.1` (Fabric loader) — сверить актуальный на момент сборки
- [ ] Добавить/обновить **Fabric API** до `0.141.2+1.21.11` (если используется; сейчас в зависимостях его нет — проверить, нужен ли он вообще, т.к. Baritone работает в основном через миксины).
- [ ] `mixin_version` → поднять (0.8.5 → 0.8.6/0.8.7; в MC 1.21.x идёт свежий sponge-mixin через Fabric loader — согласовать, чтобы не было конфликта версий).
- [ ] `asm_version` → 9.7+ (для class file v65 / Java 21).
- [ ] `nether_pathfinder_version=1.6` — проверить, есть ли новее в maven `babbaj-repo`; либа нативная (JNI), от версии MC не зависит, но пересобрать под Java 21 не помешает.
- [ ] Убрать `forge_version` из активного использования (см. этап 4).
- [ ] Обновить Parchment (этап 5).

---

## 4. Отключить Forge-платформу (Fabric-only)

- [ ] В [settings.gradle](settings.gradle#L45-L50) сборка платформ управляется через `Baritone.enabled_platforms`. Зафиксировать сборку только Fabric: собирать с `-DBaritone.enabled_platforms=fabric` либо убрать `forge` из дефолта.
- [ ] Модуль [forge/](forge/) и [tweaker/](tweaker/) не мигрируем; при желании — оставить как есть, но исключить из сборки, чтобы они не ломали билд.
- [ ] Проверить, что общий (`common`) код не содержит Forge-специфичных зависимостей на пути компиляции Fabric.

---

## 5. Маппинги (Parchment)

- [ ] В [build.gradle](build.gradle#L91-L95) заменить `parchment("2023.06.26")` на актуальную версию под 1.21.x (напр. `parchment("1.21.11:<дата>")` — формат в unimined 1.4.x: `parchment(mcVersion, releaseVersion)`; свериться с доступными артефактами `org.parchmentmc.data:parchment-1.21.x`).
- [ ] Если под конкретно 1.21.11 Parchment ещё нет — взять ближайший 1.21.x (маппинги параметров обратно совместимы) или временно убрать Parchment, оставив чистый mojmap.
- [ ] Прогнать `genSources`, убедиться, что маппинги накатываются без ошибок.

---

## 6. Починка компиляции по подсистемам

> Порядок — от самого «широкого» (задевает много файлов) к локальному.
> После каждой подсистемы гонять компиляцию `common` (main + api) и смотреть остаток ошибок.

### 6.1. `ResourceLocation` — приватный конструктор (1.21)
`new ResourceLocation(...)` больше нельзя. Заменить на `ResourceLocation.parse(str)`
(валидирующий), `ResourceLocation.tryParse(str)` или `ResourceLocation.fromNamespaceAndPath(ns, path)`.
- [ ] [BlockById.java:33](src/api/java/baritone/api/command/datatypes/BlockById.java#L33)
- [ ] [EntityClassById.java:33](src/api/java/baritone/api/command/datatypes/EntityClassById.java#L33)
- [ ] [ForBlockOptionalMeta.java:80](src/api/java/baritone/api/command/datatypes/ForBlockOptionalMeta.java#L80)
- [ ] [ItemById.java:33](src/api/java/baritone/api/command/datatypes/ItemById.java#L33)
- [ ] [BaritoneToast.java:49](src/api/java/baritone/api/utils/gui/BaritoneToast.java#L49)
- [ ] [SettingsUtil.java:249](src/api/java/baritone/api/utils/SettingsUtil.java#L249)
- [ ] [PathRenderer.java:53](src/main/java/baritone/utils/PathRenderer.java#L53)
- [ ] [LitematicaSchematic.java:84](src/main/java/baritone/utils/schematic/format/defaults/LitematicaSchematic.java#L84)
- [ ] [SpongeSchematic.java:133](src/main/java/baritone/utils/schematic/format/defaults/SpongeSchematic.java#L133)

### 6.2. Реестры / RegistryAccess
- [ ] Сверить `BuiltInRegistries.BLOCK/ITEM.get(...)` — в 1.21 `get(ResourceLocation)` может возвращать `Optional`/`Holder`; проверить сигнатуры и `getOptional`.
- [ ] Места, где нужен доступ к динамическим реестрам (biomes/dimension/damage types), теперь через `RegistryAccess` (`level.registryAccess()`), а не статически. Проверить [SettingsUtil.java](src/api/java/baritone/api/utils/SettingsUtil.java), схематики, поиск блоков.
- [ ] `DimensionType` / определение измерения (nether/overworld) — API вокруг `Level.dimension()` / `dimensionType()` менялся; проверить логику определения нижнего мира для nether-pathfinder.

### 6.3. Data components вместо NBT у ItemStack (1.20.5) — **важно**
`ItemStack.getTag()/getTagElement()/getOrCreateTag()` удалены. Данные предмета теперь
типизированные компоненты (`DataComponents.*`).
- [ ] [ElytraBehavior.java:938,948](src/main/java/baritone/process/elytra/ElytraBehavior.java#L938) — `itemStack.getTagElement("Fireworks")` → читать компонент `DataComponents.FIREWORKS` (тип `net.minecraft.world.item.component.Fireworks`), брать `flightDuration()`/`explosions()` из record'а, а не из `CompoundTag`.
- [ ] Проверить весь инвентарь/выбор инструмента ([main/java/baritone/utils/ToolSet.java] и утилиты предметов) на предмет обращения к NBT/зачарованиям (`enchantments` тоже стали компонентом `DataComponents.ENCHANTMENTS`).
- [ ] `MixinItemStack` — сверить целевые методы (см. этап 7).
- [ ] ⚠️ НЕ путать с `Waypoint.getTag()` в [Waypoint.java](src/api/java/baritone/api/cache/Waypoint.java) — это собственный enum `Waypoint.Tag`, к NBT отношения не имеет, трогать не надо.

### 6.4. Нетворкинг (1.20.2 — переписан)
Разделение фаз login/configuration/play; `ClientPacketListener` изменён.
- [ ] [MixinClientPlayNetHandler.java](src/launch/java/baritone/launch/mixins/MixinClientPlayNetHandler.java) — сверить целевой класс/методы (`ClientPacketListener`), обработку входящих пакетов чанков/позиции.
- [ ] [MixinNetworkManager.java](src/launch/java/baritone/launch/mixins/MixinNetworkManager.java) — `Connection` API, отправка пакетов.
- [ ] Проверить работу с пакетами позиции/поворота игрока (`ServerboundMovePlayerPacket` и т.п.) в поведении вращения/движения.

### 6.5. Text / Component API
- [ ] `Component`, `MutableComponent`, `Style` — в основном стабильны, но **`ClickEvent`/`HoverEvent` переписаны на sealed-интерфейсы/records (1.21.5)**. Проверить все места сборки кликабельного текста: [WaypointsCommand.java](src/main/java/baritone/command/defaults/WaypointsCommand.java), билдеры сообщений/справки в [command/](src/main/java/baritone/command/), [api/command/helpers/](src/api/java/baritone/api/command/helpers/).
- [ ] `ClickEvent.Action.RUN_COMMAND` + строка → теперь конструкторы record'ов (`new ClickEvent.RunCommand(...)` и т.п.). Аналогично `HoverEvent.ShowText`.

### 6.6. Рендеринг — **самая объёмная часть**
Пайплайн менялся многократно: `GuiGraphics` вместо `PoseStack` для GUI (1.20),
изменения `RenderType`/`VertexConsumer`, а в 1.21.2–1.21.5 — крупная переработка
`BufferBuilder`/`Tesselator`/`MeshData` и вертекс-форматов.
- [ ] [IRenderer.java](src/main/java/baritone/utils/IRenderer.java) — базовые вызовы `RenderSystem`/`Tesselator`/`BufferBuilder`. Переписать под новый API (`Tesselator.getInstance().begin(mode, format)` → `BufferBuilder`, затем `BufferUploader.drawWithShader(meshData)` / `MeshData`).
- [ ] [PathRenderer.java](src/main/java/baritone/utils/PathRenderer.java) — отрисовка пути/целей, `PoseStack`, beacon-beam текстура.
- [ ] [SelectionRenderer.java](src/main/java/baritone/selection/SelectionRenderer.java) — рендер выделений.
- [ ] [GuiClick.java](src/main/java/baritone/utils/GuiClick.java) — проекция/`Matrix4f`, клик по миру.
- [ ] [BaritoneToast.java](src/api/java/baritone/api/utils/gui/BaritoneToast.java) — тосты теперь рисуются через `GuiGraphics`; `Toast.render(...)` сигнатура изменилась.
- [ ] [RenderEvent.java](src/api/java/baritone/api/event/events/RenderEvent.java) — событие несёт `PoseStack`; сверить, что прокидывается из миксина рендера.
- [ ] [MixinWorldRenderer.java](src/launch/java/baritone/launch/mixins/MixinWorldRenderer.java) — точка инъекции отрисовки мира (`LevelRenderer`), имена методов/дескрипторы почти наверняка изменились; возможно, целевой метод переехал (`renderLevel`).

### 6.7. GUI / Screen
- [ ] [MixinScreen.java](src/launch/java/baritone/launch/mixins/MixinScreen.java) и `passEvents` в [MixinMinecraft.java:165-176](src/launch/java/baritone/launch/mixins/MixinMinecraft.java#L165) — поле `Screen.passEvents` **удалено** в новых версиях. Логику «пропускать ввод во время патинга» переделать (проверять состояние иначе) или убрать redirect.
- [ ] [MixinCommandSuggestionHelper.java](src/launch/java/baritone/launch/mixins/MixinCommandSuggestionHelper.java) — автодополнение команд (`CommandSuggestions`), сверить целевые методы/поля.

### 6.8. Кэш чанков и palette (внутренности MC — хрупко)
Эти миксины лезут в приватные поля, имена/структура меняются часто.
- [ ] [MixinPalettedContainer.java](src/launch/java/baritone/launch/mixins/MixinPalettedContainer.java) и [MixinPalettedContainer$Data.java](src/launch/java/baritone/launch/mixins/MixinPalettedContainer$Data.java) — структура `PalettedContainer`/`Data`, `BitStorage`, палитра.
- [ ] [MixinChunkArray.java](src/launch/java/baritone/launch/mixins/MixinChunkArray.java) — `ClientChunkCache$Storage` (внутренний массив чанков), поля `chunks`/`viewRange`/`chunkRadius`.
- [ ] [MixinClientChunkProvider.java](src/launch/java/baritone/launch/mixins/MixinClientChunkProvider.java) — `ClientChunkCache`.
- [ ] Проверить кэш блоков Baritone ([cache/](src/main/java/baritone/cache/)): чтение `LevelChunkSection`/`PalettedContainer` напрямую.

### 6.9. Остальные миксины и точки входа
- [ ] [MixinMinecraft.java](src/launch/java/baritone/launch/mixins/MixinMinecraft.java) — дескрипторы в `@At target=` (`missTime:I`, `screen:Lnet/minecraft/client/gui/screens/Screen;`, `ClientLevel.tickEntities()V`) сверить с 1.21.11; метод `setLevel` мог быть переименован.
- [ ] [MixinClientPlayerEntity.java](src/launch/java/baritone/launch/mixins/MixinClientPlayerEntity.java), [MixinLivingEntity.java](src/launch/java/baritone/launch/mixins/MixinLivingEntity.java), [MixinEntity.java](src/launch/java/baritone/launch/mixins/MixinEntity.java) — движение/поворот; сигнатуры `travel`/`aiStep`/`tick` менялись.
- [ ] [MixinPlayerController.java](src/launch/java/baritone/launch/mixins/MixinPlayerController.java) — `MultiPlayerGameMode` (ломание/установка блоков), API взаимодействия менялся.
- [ ] [MixinFireworkRocketEntity.java](src/launch/java/baritone/launch/mixins/MixinFireworkRocketEntity.java) — буст элитр.
- [ ] [MixinLootContext.java](src/launch/java/baritone/launch/mixins/MixinLootContext.java) — loot API (1.21 переработка лута), для предсказания дропа блоков.
- [ ] [MixinEntityRenderManager.java](src/launch/java/baritone/launch/mixins/MixinEntityRenderManager.java), [MixinClientPlayNetHandler](src/launch/java/baritone/launch/mixins/MixinClientPlayNetHandler.java).

---

## 7. Ревизия миксинов (общий чек)

- [ ] [mixins.baritone.json](src/launch/resources/mixins.baritone.json): `compatibilityLevel` → `JAVA_21`.
- [ ] Для каждого миксина: убедиться, что `@Shadow`-поля/методы, `@Inject`/`@Redirect`/`@At` таргеты существуют в 1.21.11. При «сломанных» инъекциях искать новую точку/метод.
- [ ] Где строковые дескрипторы (`target = "net/minecraft/.../Class.method:desc"`) — самая частая причина падений в рантайме; проверять по декомпилированному 1.21.11.

---

## 8. Метаданные мода

- [ ] [fabric.mod.json](fabric/src/main/resources/fabric.mod.json): `"minecraft": ">=1.21.11"` (или `~1.21.11`), `"fabricloader": ">=0.16.0"`, при необходимости добавить `"java": ">=21"`.
- [ ] Проверить, нужен ли depends на `fabric-api` (обычно Baritone не требует).

---

## 9. Сборка и запуск

- [ ] `./gradlew :fabric:build -DBaritone.enabled_platforms=fabric` — добиться чистой компиляции.
- [ ] Настроить runClient (unimined) или запустить готовый jar в реальном клиенте Fabric 1.21.11 + свежий Fabric loader.
- [ ] Дымовые тесты в игре:
  - [ ] Мод грузится, все миксины применились (в логе нет `mixin apply failed`).
  - [ ] `#goto`, `#path`, базовый патинг работает.
  - [ ] Ломание/установка блоков (`#mine`, строительство).
  - [ ] Отрисовка пути/выделений (этап 6.6) — визуально корректна.
  - [ ] Кэш чанков / `#explore` (этап 6.8).
  - [ ] Элитра-полёт + бусты фейерверком (этап 6.3/6.9).
  - [ ] Команды и кликабельные сообщения/автодополнение (этап 6.5/6.7).

---

## 10. Финализация

- [ ] Прогнать `proguard`/`createDist` ([fabric/build.gradle](fabric/build.gradle#L82-L91)) — сверить, что ProGuard 7.2.1 понимает class file v65 (Java 21); при необходимости поднять версию ProGuard.
- [ ] Обновить README/бейджи версии, переименовать корневую папку `baritone-1.19.4` → `baritone-1.21.11` (по желанию).
- [ ] Финальный тест итогового (обфусцированного) jar в чистом профиле.

---

## Что может понадобиться скачать вручную (fabric.jar и пр.)

При обычной сборке unimined сам тянет всё из Maven (MC, Fabric loader, mappings) —
**вручную ничего класть не нужно**. Скинуть jar в папку может потребоваться, только если:
- нет доступа к maven-репозиториям (оффлайн) — тогда нужны: `fabric-loader-0.18.1.jar`,
  `fabric-api-0.141.2+1.21.11.jar`, при желании — деобфусцированный MC 1.21.11 для справки;
- захочется держать рядом эталонный `fabric-api` jar для сверки сигнатур.

**Скажу отдельно, если конкретный jar понадобится** — тогда закинь его в папку проекта.

---

## Основные риски / самые трудоёмкие места

1. **Рендеринг (6.6)** — переработка `Tesselator`/`BufferBuilder`/`MeshData` и вертекс-форматов между 1.20 и 1.21.5+. Самый большой объём переписывания.
2. **Кэш чанков / palette (6.8)** — доступ к приватным внутренностям MC; хрупко, ломается тихо в рантайме.
3. **Data components (6.3)** — фейерверки/зачарования/предметы.
4. **Строковые дескрипторы в миксинах (7)** — компилируется, но падает при запуске; сверять по декомпилю.
5. **unimined 1.0.5 → 1.4.x** — могла поменяться конфигурация DSL сборки и API, используемый в `buildSrc` (`ProguardTask`).
