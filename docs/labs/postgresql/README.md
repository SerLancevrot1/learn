# Репликация и восстановление PostgreSQL

[Конспект](../../theory/data/README.md) · [План Tech Lead и вопросы](../../interview/tech-lead.md#надёжность-postgresql) · [Docker для начинающих](../../interview/tech-lead.md#как-запускать-стенды-в-docker) · [Разбор конфигурации](#как-устроены-файлы-стенда)

Первичный экземпляр, standby и экземпляр восстановления используют разные тома данных. Экземпляр восстановления читает копию и архив WAL из учебного тома только для чтения. Команды выполняются внутри контейнеров PostgreSQL 17. База и роль называются `learn`.

## Асинхронный standby

Открой PowerShell в корне репозитория. Перейди в каталог PostgreSQL и запусти primary и standby. При первом старте standby делает `pg_basebackup -R`, который создаёт `standby.signal` и настройки подключения к primary.

```powershell
Set-Location .\docs\labs\postgresql
docker compose --profile standby up -d --wait primary standby
docker compose --profile standby ps -a
```

`--profile standby` подключает сервис реплики. `up -d --wait primary standby` явно выбирает два сервиса, запускает их в фоне и ждёт проверки здоровья обоих. Профиль `restore` пока не нужен. В `ps -a` смотри `STATUS`. Если standby не готов, выполни `docker compose --profile standby logs primary standby --tail 40` и посмотри, прошли ли запуск primary и создание базовой копии для реплики.

### Выполни первые запросы SQL

Команда `docker compose exec -T primary psql -U learn -d learn -c "SELECT …"` состоит из двух частей. Compose запускает `psql` внутри существующего контейнера `primary`. `psql` подключается с ролью `-U learn` к базе `-d learn`, исполняет SQL из `-c` и завершает работу. Аргументы `-d` в `docker compose up` и в `psql` относятся к разным программам. Без `-h` клиент подключается через локальный Unix-сокет внутри своего контейнера.

```powershell
docker compose exec -T primary psql -U learn -d learn -c "SELECT application_name, state, sync_state, sent_lsn, flush_lsn, replay_lsn FROM pg_stat_replication;"
docker compose --profile standby exec -T standby psql -U learn -d learn -c "SELECT pg_is_in_recovery();"
docker compose exec -T primary psql -U learn -d learn -c "CREATE TABLE orders (id int PRIMARY KEY, amount int NOT NULL); INSERT INTO orders VALUES (1, 100);"
```

| SQL | Что показывает или меняет |
| --- | --- |
| `SELECT … FROM pg_stat_replication` | На primary показывает подключение `lab_standby`. Ожидай `state=streaming` и `sync_state=async`. `sent_lsn`, `flush_lsn` и `replay_lsn` показывают позиции WAL, которые соответственно отправлены, сохранены на реплике и уже применены ею. Если строк пока нет, подожди подключения standby и повтори запрос. |
| `SELECT pg_is_in_recovery()` | На standby возвращает `t`. Этот экземпляр воспроизводит WAL и принимает запросы на чтение. |
| `CREATE TABLE …; INSERT …` | Создаёт учебную таблицу `orders` с уникальным `id` и добавляет заказ `1` на сумму `100`. Ожидай `CREATE TABLE` и `INSERT 0 1`. |

### Проверь запись при остановленной реплике

Останови standby и добавь строку. Успешный ответ primary покажет, что этот `COMMIT` не ждёт standby. Запусти его снова и дождись строки `2`.

```powershell
docker compose --profile standby stop standby
docker compose exec -T primary psql -U learn -d learn -c "INSERT INTO orders VALUES (2, 200);"
docker compose --profile standby start standby
docker compose --profile standby exec -T standby psql -U learn -d learn -c "SELECT * FROM orders ORDER BY id;"
```

`stop standby` оставляет файлы реплики в томе. `INSERT … VALUES (2, 200)` выполняется только на primary. `start standby` запускает прежний контейнер, который догоняет primary. Последний `SELECT` читается уже с реплики. Если после старта соединение ещё не готово или виден только заказ `1`, повтори `SELECT` через несколько секунд.

Объясни, что случилось бы с записью `2`, если бы primary потерял диск до передачи этого WAL. В реальной системе отдельно наблюдай receive/flush/replay LSN. На спокойной системе временные показатели lag могут быть `NULL` или отражать последний commit.

## Синхронный commit

Включи ожидание `lab_standby` и проверь `sync_state=sync`. Сначала выполни следующий блок в первом окне.

```powershell
# Первое окно.
docker compose exec -T primary psql -U learn -d learn -c "ALTER SYSTEM SET synchronous_standby_names = 'FIRST 1 (lab_standby)';"
docker compose exec -T primary psql -U learn -d learn -c "SELECT pg_reload_conf();"
docker compose exec -T primary psql -U learn -d learn -c "SELECT application_name, state, sync_state FROM pg_stat_replication;"
```

`ALTER SYSTEM SET` записывает настройку в `postgresql.auto.conf` на primary. `FIRST 1 (lab_standby)` требует подтверждения от одного указанного синхронного standby. `pg_reload_conf()` просит сервер перечитать настройки без перезапуска. Повтори запрос к `pg_stat_replication`, если статус пока `async`. Когда увидишь `sync`, останови standby в первом окне.

```powershell
docker compose --profile standby stop standby
```

Открой второе окно PowerShell и перейди в тот же каталог стенда. Запусти `INSERT` оттуда. После отправки команда остаётся ждать подтверждения реплики, поэтому не закрывай это окно.

```powershell
# Второе окно. Команда ждёт подтверждения standby.
docker compose exec -T primary psql -U learn -d learn -c "INSERT INTO orders VALUES (3, 300);"
```

Вернись в первое окно. `pg_stat_activity` показывает текущие запросы. Фильтр `wait_event = 'SyncRep'` оставляет запрос, который ждёт синхронного подтверждения. Сначала убедись, что видишь ожидающий `INSERT`, затем запускай standby.

```powershell
docker compose exec -T primary psql -U learn -d learn -c "SELECT pid, wait_event_type, wait_event, query FROM pg_stat_activity WHERE wait_event = 'SyncRep';"
```

```powershell
docker compose --profile standby start standby
```

Дождись успешного завершения `INSERT` во втором окне. Объясни, почему таймаут клиента во время `SyncRep` оставляет неопределённый результат для клиента. Верни асинхронный режим перед восстановлением. Проверь, что `SHOW synchronous_standby_names` показывает пустое значение.

```powershell
docker compose exec -T primary psql -U learn -d learn -c "ALTER SYSTEM RESET synchronous_standby_names;"
docker compose exec -T primary psql -U learn -d learn -c "SELECT pg_reload_conf();"
docker compose exec -T primary psql -U learn -d learn -c "SHOW synchronous_standby_names;"
```

`ALTER SYSTEM RESET` убирает запись из `postgresql.auto.conf`. После `pg_reload_conf()` команда `SHOW` читает действующее значение. Пустое `synchronous_standby_names` возвращает стенд к асинхронной репликации.

## Учение PITR

Создай физическую копию и проверь её манифест. После копии добавь заказ `4`, зафиксируй именованную точку и только затем удали строки. Для появления нужного WAL в архиве переключи сегмент. Используй новое имя Compose-проекта или свежие тома, если повторяешь лабораторную с начала.

```powershell
docker compose exec -T -u postgres primary pg_basebackup -U learn -D /archive/base -X stream
docker compose exec -T -u postgres primary pg_verifybackup /archive/base
docker compose exec -T primary psql -U learn -d learn -c "INSERT INTO orders VALUES (4, 400);"
$walFile = (docker compose exec -T primary psql -U learn -d learn -Atc "SELECT pg_walfile_name(pg_create_restore_point('before_delete'));").Trim()
docker compose exec -T primary psql -U learn -d learn -c "DELETE FROM orders;"
docker compose exec -T primary psql -U learn -d learn -c "SELECT pg_switch_wal();"
docker compose exec -T primary psql -U learn -d learn -c "SELECT archived_count, failed_count, last_archived_wal FROM pg_stat_archiver;"
docker compose exec -T -u postgres primary ls "/archive/wal/$walFile"
```

| Команда | Что делает и что проверить |
| --- | --- |
| `pg_basebackup -U learn -D /archive/base -X stream` | Копирует физические файлы PostgreSQL в пустой каталог внутри тома `archive`. `-U learn` — роль PostgreSQL, `-D` — каталог назначения, `-X stream` параллельно забирает WAL, необходимый для консистентности копии. Флаг Compose `-u postgres` запускает утилиту от Linux-пользователя с правом записи в каталог. |
| `pg_verifybackup /archive/base` | Проверяет файлы копии по `backup_manifest` и доступность WAL, необходимого для самой копии. Успех этой проверки ещё не показывает, что архив доведёт нас до `before_delete`. |
| `INSERT … VALUES (4, 400)` | Добавляет запись после создания base backup. При восстановлении она должна прийти из WAL. |
| `$walFile = (… psql … -Atc "SELECT …").Trim()` | Создаёт точку `before_delete` в WAL и получает имя сегмента по её позиции. У `psql` `-A` отключает табличное выравнивание, `-t` оставляет данные без заголовка, `-c` выполняет запрос. `Trim()` убирает пробелы по краям, `$walFile` хранит имя для последней команды. |
| `DELETE FROM orders` | Удаляет все учебные строки. Команда идёт после создания точки. В standby удаление придёт по тому же потоку WAL. |
| `SELECT pg_switch_wal()` | Закрывает текущий сегмент WAL переключением на следующий, чтобы законченный сегмент можно было архивировать. |
| `SELECT … FROM pg_stat_archiver` | Показывает счётчики архивированных сегментов и ошибок, а также имя последнего успешно архивированного файла. |
| `ls "/archive/wal/$walFile"` | Проверяет существование конкретного файла внутри контейнера primary. PowerShell подставляет имя из `$walFile` в путь перед вызовом `exec`. Если файла ещё нет, повтори проверку через несколько секунд. |

Проверь, что на primary и после replay на standby `count(*) = 0`. Если standby ещё видит старые данные, подожди применения WAL и повтори его запрос.

```powershell
docker compose exec -T primary psql -U learn -d learn -c "SELECT count(*) FROM orders;"
docker compose --profile standby exec -T standby psql -U learn -d learn -c "SELECT count(*) FROM orders;"
```

### Запусти восстановление и проверь результат

Отметь начало восстановления, затем подними отдельный экземпляр. В логах найди остановку replay в `before_delete` и выход из recovery. `pg_isready` сам по себе может показать готовность к чтению до promote, поэтому проверь флаг recovery и данные.

```powershell
$start = Get-Date
docker compose --profile restore up -d --wait restore
docker compose --profile restore logs restore --tail 40
docker compose --profile restore exec -T restore psql -U learn -d learn -c "SELECT pg_is_in_recovery(); SELECT * FROM orders ORDER BY id; SELECT count(*), sum(amount) FROM orders;"
(Get-Date) - $start
```

`$start = Get-Date` сохраняет время до запуска. `--profile restore up -d --wait restore` включает и запускает сервис восстановления. Он копирует base backup в свой том и применяет архивный WAL до точки. `logs restore --tail 40` показывает строки запуска PostgreSQL и replay. В `psql` первый запрос проверяет режим экземпляра, второй печатает строки `orders`, третий проверяет количество и сумму. `(Get-Date) - $start` выводит прошедшее время. Если первый запрос ещё показывает `t`, дождись завершения recovery и повтори проверку SQL.

Ожидай `pg_is_in_recovery=f`, четыре заказа, сумму `1000`. Наличие заказа `4` подтверждает replay WAL после base backup. Его сохранность вместе с остальными строками показывает, что удаление осталось за пределами выбранной точки восстановления. Если restore не стартует, проверь логи, непрерывность WAL и имя точки. Сними время от запуска до проверки запроса. Для оценки полного RTO добавь обнаружение сбоя, подготовку среды и переключение приложения. Для оценки RPO сравни последнюю восстановимую прикладную запись с моментом сбоя.

Дополнительный опыт. До первого запуска `restore` попробуй предсказать его реакцию, если архивный сегмент с точкой восстановления недоступен. Для проверки используй отдельную копию учебного архива. Сравни наличие файла backup и возможность дойти до целевой точки.

## Полная и инкрементальная копии

На primary включён `summarize_wal`. После PITR-учения создай инкремент от `/archive/base` и собери из них новую полную копию. Здесь инкремент отражает состояние primary **после** `DELETE`. Для проверки восстановленного состояния до удаления использовалась отдельная цель PITR.

```powershell
docker compose exec -T -u root primary install -d -o postgres -g postgres -m 0700 /archive/inc /archive/combined
docker compose exec -T -u postgres primary pg_basebackup -U learn -D /archive/inc -X stream --incremental=/archive/base/backup_manifest
docker compose exec -T -u postgres primary pg_combinebackup -o /archive/combined /archive/base /archive/inc
docker compose exec -T -u postgres primary pg_verifybackup /archive/combined
docker compose exec -T -u postgres primary du -sh /archive/base /archive/inc /archive/combined
```

| Утилита | Что делает |
| --- | --- |
| `install -d -o postgres -g postgres -m 0700 …` | Создаёт два каталога. `-d` означает режим создания каталогов, `-o` и `-g` задают владельца и группу, `0700` даёт доступ только владельцу. `-u root` нужен здесь внутри контейнера, чтобы назначить владельца. |
| `pg_basebackup … --incremental=/archive/base/backup_manifest` | Использует манифест полной копии и сводки WAL, чтобы определить изменившиеся блоки. Пишет инкремент в `/archive/inc`. |
| `pg_combinebackup -o /archive/combined /archive/base /archive/inc` | Собирает синтетическую полную копию. `-o` задаёт каталог результата, а входы перечислены от старейшей копии к новейшей. |
| `pg_verifybackup /archive/combined` | Проверяет полученный результат по его манифесту. |
| `du -sh /archive/base /archive/inc /archive/combined` | `-s` показывает общий размер каждого каталога, `-h` выводит размеры в удобных единицах. |

Сравни объёмы. На маленькой базе WAL и служебные файлы могут скрывать выигрыш. Объясни, какие копии и манифесты хранить, чтобы собрать цепочку, и как этап сборки повлияет на время восстановления.

В конце выполни `docker compose --profile standby --profile restore down`. Чтобы заново пройти упражнение с чистыми данными, добавь `-v` к команде в этом каталоге. Это удалит данные трёх учебных экземпляров и архив.

## Как устроены файлы стенда

### compose.yaml и диски

В [compose.yaml](compose.yaml) общий блок `x-postgres: &postgres` описывает образ `postgres:17`, каталог данных `PGDATA` и проверку здоровья. Каждый сервис добавляет этот блок через `<<: *postgres`. `pg_isready -h 127.0.0.1 -U learn -d learn` проверяет, принимает ли PostgreSQL соединения внутри контейнера. Проверка выполняется каждые две секунды, с таймаутом в три секунды и порогом в 25 последовательных неуспехов. Для оценки состояния standby и восстановления есть отдельные SQL-запросы.

Сравни `docker compose config --services` и `docker compose --profile '*' config --services`. Первая команда перечислит только `primary`, вторая включит все профили и перечислит все три сервиса. Кавычки передают `*` как часть аргумента. Обе команды только читают конфигурацию.

| Том или файл | Где используется |
| --- | --- |
| `primary-data`, `standby-data`, `restore-data` | У каждого экземпляра свой том, примонтированный к `/var/lib/postgresql/data`. Подкаталог `pgdata` внутри тома — рабочий каталог PostgreSQL. Поэтому восстановление не перезаписывает файлы primary. |
| `archive` | У `primary` примонтирован к `/archive` с записью. В `/archive/base` кладём base backup, в `/archive/wal` попадают архивные сегменты. У `restore` тот же том доступен как `/archive:ro`, поэтому восстановление читает источник, а пишет в `restore-data`. |
| `./pg_hba.conf` | Примонтирован в `primary` как `/etc/postgresql/lab-pg_hba.conf:ro`. Параметр `hba_file` передаёт этот путь серверу. |

На Windows порты этого стенда не публикуются. `psql` запускается через `exec` в нужном контейнере, а standby подключается к primary по имени `primary` в сети Compose.

### Что выполняется при запуске контейнера

`entrypoint: ['/bin/sh', '-ec']` запускает Linux-оболочку. `-c` выполняет текст скрипта из `command`, `-e` завершает скрипт при необработанной ошибке команды. `|` после `command` в YAML сохраняет переносы строк. Символ `\` в конце строки склеивает строки в одну команду оболочки. `$$PGDATA` экранирует доллар для Compose, чтобы внутри контейнера оболочка получила `$PGDATA` и подставила путь. `exec docker-entrypoint.sh postgres …` передаёт управление стандартному скрипту образа, который готовит окружение и запускает PostgreSQL.

- **Primary.** `install -d` готовит каталоги архива для пользователя `postgres`. При пустом томе `POSTGRES_USER`, `POSTGRES_PASSWORD` и `POSTGRES_DB` задают начальную роль, пароль и базу. `archive_mode=on` включает архивирование, `summarize_wal=on` включает сводки для инкрементальных копий. В `archive_command` `%p` подставляет путь к сегменту, `%f` — имя файла. Команда копирует сегмент во временный файл и после успешного копирования переименовывает его. Если файл с таким именем уже есть, `cmp` проверяет, совпадает ли содержимое.
- **Standby.** `depends_on` с `service_healthy` ждёт готовности primary. `if [ ! -s "$PGDATA/PG_VERSION" ]` проверяет, нужно ли первоначальное копирование. `gosu postgres` запускает `pg_basebackup` от Linux-пользователя `postgres`. В строке подключения `host=primary` выбирает первичный контейнер, а `application_name=lab_standby` задаёт имя для `pg_stat_replication` и `synchronous_standby_names`. `-R` записывает параметры подключения и создаёт `standby.signal`. При повторном старте уже существующие файлы остаются. `hot_standby=on` разрешает чтение во время применения WAL.
- **Restore.** При пустом томе проверяет наличие base backup, через `cp -a` переносит его в свой `PGDATA` с сохранением атрибутов файлов и создаёт `recovery.signal` через `touch`. `restore_command` копирует запрошенный сегмент из `/archive/wal` туда, куда указывает `%p`. `recovery_target_name=before_delete` выбирает именованную точку, `recovery_target_action=promote` завершает recovery и переводит экземпляр в обычный режим. Повторный `start` с прежним томом продолжает работу с уже восстановленными файлами. Для нового учения с той же целью нужен пустой том восстановления.

### pg_hba.conf

[pg_hba.conf](pg_hba.conf) — правила, по которым primary аутентифицирует подключения. PostgreSQL проверяет правила сверху вниз и использует первое подходящее. Строки `local` относятся к Unix-сокету, `host` — к TCP. Для `host` четвёртое поле — диапазон адресов, здесь `all` внутри учебной конфигурации.

| Правило | Кто его использует |
| --- | --- |
| `local all all trust` | `psql` внутри контейнера primary для обычного подключения к `learn`. `trust` допускает заявленную роль через локальный сокет без пароля. |
| `local replication learn trust` | Локальный `pg_basebackup -U learn`. Поле `replication` отдельно разрешает физическую репликацию через сокет. |
| `host all all all scram-sha-256` | Обычные TCP-подключения с проверкой пароля SCRAM. |
| `host replication learn all scram-sha-256` | Подключение реплики к `primary` для получения base backup и потока WAL. Для физической репликации требуется отдельная строка `replication`. |

[Формат pg_hba.conf](https://www.postgresql.org/docs/17/auth-pg-hba-conf.html) · [pg_isready](https://www.postgresql.org/docs/17/app-pg-isready.html)

[Streaming replication](https://www.postgresql.org/docs/17/warm-standby.html#STREAMING-REPLICATION) · [pg_basebackup](https://www.postgresql.org/docs/17/app-pgbasebackup.html) · [Continuous Archiving and PITR](https://www.postgresql.org/docs/17/continuous-archiving.html)
