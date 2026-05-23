# LiteMenus API и YAML-гайд

Документ описывает текущий публичный формат LiteMenus: как создавать меню, какие поля поддерживаются, как работают клики, команды, звуки, требования и анимации.

## Быстрый старт

1. Положи `LiteMenus-1.0.jar` в папку `plugins`.
2. Запусти сервер.
3. Открой папку:

```text
plugins/LiteMenus/menus/
```

LiteMenus загружает все `.yml` файлы внутри `menus`, включая подпапки. Если имя файла уникальное, id остается простым: `menus/kits/crusher.yml` получит id `crusher`. Если есть два файла с одинаковым именем, для дубликатов используется id по пути, например `kits/crusher`.

4. Создай файл меню, например:

```text
plugins/LiteMenus/menus/main.yml
```

5. Перезагрузи LiteMenus:

```text
/lm reload
```

6. Открой меню:

```text
/lm open main
```

Если в меню есть ошибка, LiteMenus не загрузит только это меню и покажет точный путь ошибки в консоли.

## Команды

```text
/lm reload
/lm open <menu> [player]
```


## Permissions

```text
litemenus.admin
litemenus.open
litemenus.reload
```

## Минимальное меню

Файл: `plugins/LiteMenus/menus/main.yml`

```yaml
title: "&8Главное меню"
size: 27
open-command: main
open-aliases:
  - menu
  - gui

items:
  profile:
    slot: 13
    material: PLAYER_HEAD
    name: "&#3CFDE6Профиль"
    lore:
      - "&7Нажми, чтобы открыть профиль"
    commands:
      - "[message] &aОткрываем профиль..."
      - "[close]"
```

Открыть можно так:

```text
/main
/menu
/gui
/lm open main
```

## Верхний уровень меню

```yaml
type: CHEST
title: "&8Меню"
size: 54
open-command: menu
open-aliases:
  - m
  - menus
update: false
update-interval: 20

open-commands: []
close-commands: []

items: {}
```

Поля:

| Поле | Тип | Описание |
|---|---:|---|
| `type` | enum | `CHEST` или `BOOK`. По умолчанию `CHEST`. |
| `title` | text | Заголовок инвентаря. |
| `size` | number | Размер: `9`, `18`, `27`, `36`, `45`, `54`. |
| `open-command` | string/bool | Команда открытия. `main` даст `/main`, `kit крушителя` даст `/kit крушителя`. `false` отключает команду. |
| `open-aliases` | list | Дополнительные команды открытия: `/m`, `/menus`, `kit vip`. |
| `update` | bool | Обновлять предметы в открытом меню. |
| `update-interval` | ticks | Интервал обновления. `20` = 1 секунда. |
| `open-commands` | list | Actions при открытии меню. |
| `close-commands` | list | Actions при закрытии меню. |
| `items` | section | Предметы меню. Обязательная секция. |

Для `type: BOOK` вместо `items` используется `pages`.

### Команды с аргументами

LiteMenus умеет открывать меню не только одной командой, но и командой с аргументами:

```yaml
open-command: kit крушителя
open-aliases:
  - kit crusher
  - набор крушителя
```

После `/lm reload` игрок сможет открыть меню через `/kit крушителя`. Если несколько меню используют одну базовую команду, например `/kit`, LiteMenus сам разрулит маршруты и добавит tab-complete для следующих аргументов:

```yaml
# crusher.yml
open-command: kit крушителя

# warrior.yml
open-command: kit воителя
```

В игре после `/kit ` будут подсказки `крушителя` и `воителя`.

## Предметы

```yaml
items:
  reward:
    slot: 22
    material: DIAMOND
    name: "&#3CFDE6Награда"
    lore:
      - "&7Нажми, чтобы получить"
    glow: true
    flags:
      - HIDE_ATTRIBUTES
    click-sound: UI_BUTTON_CLICK 1 1.2
    permission: "example.reward"
    permission-message: "&cНет доступа."
    commands:
      - "[message] &aГотово!"
```

Поля предмета:

| Поле | Тип | Описание |
|---|---:|---|
| `slot` | number | Слот предмета. Можно не указывать для template-предметов scripted animation. |
| `material` | material | Bukkit material, например `DIAMOND`, `diamond`, `PLAYER_HEAD`. |
| `name` | text | Название предмета. |
| `lore` | list | Описание предмета. |
| `glow` | bool | Фейковое зачарование для свечения. |
| `flags` / `item_flags` | list | Bukkit ItemFlag. |
| `click-sound` / `sound` | sound | Звук клика по предмету. |
| `potion` | section | Цвет, base type и кастомные эффекты для potion items. |
| `permission` | string | Permission для клика. |
| `permission-message` | text | Сообщение при отсутствии доступа. |
| `commands` | list | Fallback actions для любого клика. |
| `clicks` | section | Отдельные actions для разных кликов. |

## Цвета текста

Поддерживаются:

```yaml
name: "&aОбычный цвет"
name: "&#3CFDE6HEX цвет"
name: "<#3CFDE6>MiniMessage-like HEX"
name: "<b><#3CFDE6>Жирный текст"
name: "<gradient:#3CFDE6:#00BFFF>LiteMenus</gradient>"
```

PlaceholderAPI поддерживается мягко: если PlaceholderAPI установлен, `%...%` будут обработаны.

Встроенные placeholders:

```text
%player%
%menu%
%item%
```

## Clicks

## Potion Items

Для `POTION`, `SPLASH_POTION`, `LINGERING_POTION` и `TIPPED_ARROW` можно задавать potion meta:

```yaml
items:
  speed_potion:
    slot: 13
    material: POTION
    name: "&#3CFDE6Зелье скорости"
    lore:
      - "&7Красивое зелье для меню"
    potion:
      color: "#3CFDE6"
      base: SWIFTNESS
      effects:
        - type: SPEED
          duration: 600
          amplifier: 1
          ambient: false
          particles: false
          icon: false
    flags:
      - HIDE_ADDITIONAL_TOOLTIP
```

Поля:

| Поле | Тип | Описание |
|---|---:|---|
| `potion.color` | hex | Цвет зелья, например `#3CFDE6`. |
| `potion.base` / `potion.base-type` | PotionType | Базовый тип, например `SWIFTNESS`, `HEALING`. |
| `potion.effects[].type` | PotionEffectType | Тип эффекта, например `SPEED`. |
| `potion.effects[].duration` | ticks | Длительность. `600` = 30 секунд. |
| `potion.effects[].amplifier` | number | Усиление эффекта. `0` = уровень I, `1` = уровень II. |
| `potion.effects[].ambient` | bool | Ambient-эффект. |
| `potion.effects[].particles` | bool | Показывать частицы. |
| `potion.effects[].icon` | bool | Показывать иконку эффекта. |

Чтобы скрыть стандартные подсказки зелья, используй:

```yaml
flags:
  - HIDE_ADDITIONAL_TOOLTIP
```

На некоторых версиях Bukkit также может быть доступен старый флаг:

```yaml
flags:
  - HIDE_POTION_EFFECTS
```

Если эффект, цвет или base type написаны неправильно, меню не загрузится, а LiteMenus покажет точный путь ошибки.

## Clicks

Можно назначить разные действия на разные клики:

```yaml
items:
  navigator:
    slot: 13
    material: COMPASS
    name: "&aНавигатор"
    clicks:
      left:
        sound: UI_BUTTON_CLICK 1 1.2
        commands:
          - "[player] spawn"
      right:
        sound: BLOCK_NOTE_BLOCK_PLING 1 1
        commands:
          - "[message] &eПКМ по компасу"
```

Типы кликов:

```text
left
right
shift_left
shift_right
middle
drop
number_key
any
```

Если точный click handler не найден, LiteMenus использует `any`, потом fallback `commands`.

## Actions

Actions пишутся строками:

```yaml
commands:
  - "[message] &aПривет, %player%!"
  - "[player] spawn"
  - "[console] say %player% открыл меню"
  - "[sound] UI_BUTTON_CLICK 1 1.2"
  - "[close]"
```

Типы:

| Action | Что делает |
|---|---|
| `[message] text` | Отправляет сообщение игроку. |
| `[player] command` | Выполняет команду от игрока. Без `/`. |
| `[console] command` | Выполняет команду от консоли. |
| `[sound] SOUND volume pitch` | Проигрывает звук игроку. |
| `[open] menuId` | Открывает другое меню по id. |
| `[book] menuId` | Открывает book-меню по id. |
| `[close]` | Закрывает меню. |

Если action написан без `[type]`, он считается `[player]`.

## Звуки

Формат:

```text
SOUND volume pitch
```

Примеры:

```yaml
click-sound: UI_BUTTON_CLICK 1 1.2
click-sound: ENTITY_EXPERIENCE_ORB_PICKUP 1 1.4
```

В редакторе можно выбрать предмет, нажать `Звук клика`, затем использовать:

```text
```

Tab покажет доступные звуки.

## Requirements

Базовые requirements:

```yaml
items:
  secret:
    slot: 10
    material: CHEST
    name: "&eСекрет"
    view-requirements:
      admin:
        type: permission
        input: "example.admin"
    click-requirements:
      has-name:
        type: has-placeholder
        input: "%player%"
```

Типы:

| Type | Поля | Описание |
|---|---|---|
| `permission` | `input` или `permission` | Проверяет permission игрока. |
| `has-placeholder` | `input` | Placeholder после обработки не должен быть пустым. |
| `equals-placeholder` | `input`, `expected` | Сравнивает два значения после placeholders. |

## Простая анимация открытия

```yaml
animation:
  open:
    type: FILL
    interval: 1
    material: BLACK_STAINED_GLASS_PANE
    sound: UI_BUTTON_CLICK 1 1.2
```

Типы:

```text
NONE
FILL
BORDER
ROWS
CENTER
```

Во время анимации клики по меню отменяются.

## Scripted animation

Scripted animation позволяет ставить template-предметы в слоты по кадрам.

```yaml
title: "&8Анимированное меню"
size: 45
open-command: animated
animation-sound: UI_BUTTON_CLICK 1 1.2

items:
  border:
    material: BLACK_STAINED_GLASS_PANE
    name: "&8"
  info:
    material: BOOK
    name: "&#3CFDE6Информация"
    lore:
      - "&7Предмет появился через scripted animation"

animation:
  - tick: 0
    opcodes:
      - set: border 0,1,2,3,4,5,6,7,8
  - tick: 3
    sound: ENTITY_EXPERIENCE_ORB_PICKUP 1 1.3
    opcodes:
      - set: info 22
```

В scripted animation предметы могут не иметь `slot`. Их ставит opcode:

```yaml
- set: item_id slot,slot,slot
```

Если `sound` указан у кадра, проиграется он. Если нет, используется `animation-sound`. Если нет и его, LiteMenus использует дефолтный звук scripted animation.

## Book Menus

Book menu открывает игроку окно книги вместо chest GUI.

```yaml
type: BOOK
title: "&#3CFDE6Боевой пропуск"
author: "LiteMenus"
open-command: bpinfo
open-aliases:
  - passinfo

open-commands:
  - "[sound] UI_BUTTON_CLICK 1 1.2"

pages:
  - title: "&#3CFDE6Боевой пропуск"
    lines:
      - "&7Выполняй задания,"
      - "&7получай опыт и забирай награды."
      - ""
      - "&eОткрыть меню: &f/battlepass"

  - title: "&eСтатусы"
    lines:
      - "&fОбычный"
      - "&#3CFDE6Премиум"
      - "&#00BFFFЭлитный"
```

Открыть:

```text
/bpinfo
/lm open bpinfo
```

Book menu удобно использовать для:

- правил сервера;
- описания боевого пропуска;
- справки по донату;
- квестов;
- текстовой навигации.

Из обычного GUI можно открыть книгу action-ом:

```yaml
commands:
  - "[book] bpinfo"
```

## Валидация и ошибки

Если в меню есть ошибка, LiteMenus не загрузит это меню. Остальные меню продолжат работать.

Пример ошибки:

```yaml
items:
  bad:
    slot: 999
    material: DIAMOND
```

Лог:

```text
[LiteMenus] [ERROR] menus/example.yml -> items.bad.slot: invalid slot 999 for menu size 45
[LiteMenus] [ERROR] menus/example.yml: Menu was not loaded because it has 1 error(s).
```

Частые ошибки:

| Ошибка | Что проверить |
|---|---|
| `missing required section` | Есть ли `items:`. |
| `invalid size` | Размер должен быть `9`, `18`, `27`, `36`, `45`, `54`. |
| `invalid slot` | Слот должен быть меньше размера меню. |
| `invalid material` | Проверь имя Material. |
| `unknown item flag` | Проверь ItemFlag. |
| `invalid sound` | Проверь имя Sound. |
| `unknown animation opcode` | Сейчас поддерживается только `set`. |

## Полный пример

```yaml
title: "&#3CFDE6LiteMenus &8• &fГлавное меню"
size: 45
open-command: menu
update: true
update-interval: 20

animation:
  open:
    type: BORDER
    interval: 1
    material: BLACK_STAINED_GLASS_PANE
    sound: UI_BUTTON_CLICK 1 1.2

open-commands:
  - "[sound] UI_BUTTON_CLICK 1 1.2"

close-commands:
  - "[message] &7Меню закрыто."

items:
  spawn:
    slot: 20
    material: ENDER_PEARL
    name: "&#3CFDE6Спавн"
    lore:
      - "&7ЛКМ, чтобы телепортироваться"
    click-sound: ENTITY_ENDERMAN_TELEPORT 1 1
    clicks:
      left:
        commands:
          - "[player] spawn"
          - "[close]"

  info:
    slot: 24
    material: BOOK
    name: "&#00BFFFИнформация"
    lore:
      - "&7Игрок: &f%player%"
      - "&7Меню: &f%menu%"
    commands:
      - "[message] &bЭто пример LiteMenus."
```