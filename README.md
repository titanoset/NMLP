# NMLP (No More Lonely Person)

Плагин для Paper / Purpur **1.21.x**, **Java 21**: отношения, помолвка, брак, семья, GUI. **Интерфейс по умолчанию — русский** (`messages.yml`, `gui.yml`, `items.yml`, `plugin.yml`).

English: production-oriented social/RP plugin with SQLite, migrations, PlaceholderAPI.

## Сборка

```bash
./gradlew.bat jar
```

Артефакт: `build/libs/NMLP-1.0.0-SNAPSHOT.jar` (включает SQLite JDBC + HikariCP).

## Тесты и покрытие

```bash
.\gradlew.bat test jacocoTestReport
```

HTML-отчёт JaCoCo: `build/reports/jacoco/test/html/index.html`, XML для SonarQube: `build/reports/jacoco/test/jacocoTestReport.xml`.

## Локальный SonarScanner

1. Поднимите SonarQube (например Docker: образ `sonarqube:community`, порт **9000**).
2. Соберите классы и отчёт покрытия: `.\gradlew.bat test jacocoTestReport`.
3. Установите [SonarScanner](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner/) и из корня проекта выполните:
   ```bash
   sonar-scanner -Dsonar.host.url=http://localhost:9000 -Dsonar.token=ВАШ_ТОКЕН
   ```
   Базовые свойства лежат в **`sonar-project.properties`** (при необходимости переопределяйте `-Dsonar.projectKey=...`).

## Команды (кратко)

| Команда | Описание |
|---------|----------|
| `/gender [male\|female]` | Пол; местоимения выставляются автоматически |
| `/engage accept <ник>` | Принять предложение |
| `/engage ring` | Получить помолвочное кольцо за алмазы (`ring.purchase-diamonds` в `config.yml`, по умолчанию 20) и при желании деньги Vault |
| `/marry` | Брак (после помолвки) |
| `/divorce` | Развод / снятие помолвки |
| `/partner`, `/family`, `/tree` | GUI |
| `/adopt <ник>` | Усыновление (цель в сети) |
| `/hug [ник]`, `/kiss [ник]` | Эмоции к партнёру или с правом `nmlp.affection.friend` |
| `/risk <ключ>` | Риск-действия: бафы, при злоупотреблении штраф и потеря max HP (см. `buffs.yml`) |
| `/nmlp reload` / `/nmlp give ring` | Админ |

ПКМ по игроку с **кольцом** (получить: **`/engage ring`**, админ: `/nmlp give ring`) — предложение.

## PlaceholderAPI (идентификатор `nmlp`)

- `%nmlp_partner%` — имя партнёра  
- `%nmlp_gender%` — **иконка пола** текущего игрока (строки `icons.gender.*` в `messages.yml`, по умолчанию ♂ / ♀ / ○)  
- `%nmlp_gender_<Ник>%` — иконка пола **указанного** игрока, например `%nmlp_gender_Notch%`  
- `%nmlp_gender_code%` — код `male` / `female` у текущего игрока  
- `%nmlp_gender_code_<Ник>%` — код пола по нику  
- `%nmlp_pronouns%` — текст (напр. он/его)  
- `%nmlp_status%` — статус отношений по-русски  
- `%nmlp_family%`, `%nmlp_relation_since%`, `%nmlp_children%`, `%nmlp_exes%`

Плейсхолдеры читают кэш; для актуальности данных — зайти в игру или открыть `/partner`.

### Иконки пола

В `messages.yml` → `icons.gender` задайте любые символы (в т.ч. из ресурс-пака или эмодзи), например вариант «кружок и стрелка»: `male: "⊙↓"` (на свой вкус).

## Риск-действия (`/risk`, `buffs.yml`)

В примере `buffs.yml` заданы ключи `sex` и `masturbate` (приватный сервер — правьте под себя). Дальше:

1. Меняйте в `buffs.yml` ключи, эффекты и лимиты.  
2. Выдать игрокам права `nmlp.risk.<ключ>` или родитель `nmlp.risk.*` (у OP по умолчанию).  
3. В корне сервера в **`commands.yml`** задать алиасы, например:  
   `sex: ["risk", "sex"]` — тогда `/sex` вызовет то же, что `/risk sex` (синтаксис см. [wiki Spigot](https://www.spigotmc.org/wiki/commands-yml/)).

Логика: в окне времени разрешено `max-uses-in-window` «безопасных» нажатий с кулдауном; сверх лимита — штрафные зелья, накопительная потеря **max health** (модификатор атрибута), при достижении `min-max-health-lethal` — смерть (`kill-on-lethal`). После смерти штраф max HP снимается, если `reset-risk-health-on-death: true`.

## Requirements

- Paper or Purpur 1.21.x server
- Java 21 runtime
- Optional: [Vault](https://github.com/MilkBowl/Vault), [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI), [TAB](https://github.com/NEZNAMY/TAB)

## Commands

| Command | Description |
|---------|-------------|
| `/gender [male\|female]` | Gender (pronouns follow: male → он/его, female → она/ее) |
| `/engage accept <player>` | Accept proposal |
| `/engage ring` | Get an engagement ring: pays diamonds (`ring.purchase-diamonds` in `config.yml`, default 20) and optional Vault money |
| `/marry` | Marry current partner (must be engaged) |
| `/divorce` | End marriage or engagement |
| `/partner` | Relationship profile GUI |
| `/family` | Family links GUI |
| `/tree` | Family tree GUI |
| `/adopt <player>` | Record adoptive child (online target) |
| `/hug [player]` | Hug partner (no arg) or a nearby player with `nmlp.affection.friend` |
| `/kiss [player]` | Same for kiss emote |
| `/nmlp reload` | Reload configs (`nmlp.reload`) |
| `/nmlp give ring` | Admin: add proposal ring to inventory (`nmlp.admin`) |

Right-click another player with the **engagement ring** (get one with **`/engage ring`**; admins may still use `/nmlp give ring`) to propose.

### Emotes (`/hug`, `/kiss`)

Content in `messages.yml` under `affection.*` controls all visible text; `config.yml` → `affection` sets distance, cooldown, particles. The plugin does not implement explicit sexual commands; for a private 18+ server you may adjust YAML yourself under your own rules and liability.

## PlaceholderAPI

Identifiers (identifier `nmlp`):

- `%nmlp_partner%`
- `%nmlp_gender%` (icon; see `icons.gender` in `messages.yml`)
- `%nmlp_gender_<PlayerName>%` (icon for that player)
- `%nmlp_gender_code%`, `%nmlp_gender_code_<PlayerName>%` (raw `male` / `female`)
- `%nmlp_pronouns%`
- `%nmlp_status%`
- `%nmlp_family%` (child count)
- `%nmlp_relation_since%`
- `%nmlp_children%`
- `%nmlp_exes%`

Placeholders read from an in-memory cache; warm data by playing or use `/partner` once after changes.

## TAB

TAB has no stable Maven dependency in this repo. Configure TAB prefixes/suffixes to use PlaceholderAPI placeholders, for example `%nmlp_status%` and a heart from your resource pack.

## Database

SQLite schema migrations ship inside the JAR under `migrations/` and are applied on startup.

## Permissions

- `nmlp.user` — default player features
- `nmlp.reload`, `nmlp.debug`, `nmlp.admin` — staff

## API hooks

Events:

- `com.nmlp.api.event.MarriageEvent`
- `com.nmlp.api.event.DivorceEvent`

## License

All rights reserved unless you add a license file.
