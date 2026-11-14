# От stdout до Kibana и от метрик до Grafana

[Конспект](../../theory/operations/observability.md) · [План Tech Lead и вопросы](../../interview/tech-lead.md#observability-и-nginx) · [Docker для начинающих](../../interview/tech-lead.md#как-запускать-стенды-в-docker) · [Разбор конфигурации](#как-устроены-файлы-стенда)

В этом стенде приложение пишет JSON в stdout/stderr. Docker-драйвер `fluentd` передаёт его во вход `forward` Fluent Bit. В Kubernetes ту же роль на стороне runtime и collector выполняют CRI-файлы и агент на узле. На втором пути Prometheus забирает `/metrics` напрямую у приложения. Nginx принимает пользовательские запросы.

## Запуск

Открой PowerShell в корне репозитория и перейди в каталог стенда. Compose возьмёт `compose.yaml` из этого каталога. Kibana может готовиться несколько минут.

```powershell
Set-Location .\docs\labs\observability
docker compose up -d
docker compose ps -a
```

Проверь, что Elasticsearch принимает запросы. Если он ещё стартует, повторяй команду до ответа с JSON.

```powershell
curl.exe -fsS http://localhost:9208/_cluster/health
```

Теперь отправь два запроса к приложению через Nginx.

```powershell
curl.exe -i http://localhost:8088/ok
curl.exe -i http://localhost:8088/fail
```

| Команда | Зачем выполняем и что увидим |
| --- | --- |
| `docker compose up -d` | Запускает семь сервисов в фоне. Если образов ещё нет на компьютере, Compose сначала скачает их. |
| `docker compose ps -a` | Сверь, что есть `app`, `nginx`, `fluent-bit`, `elasticsearch`, `kibana`, `prometheus`, `grafana`. Если какой-то сервис `Exited`, смотри его вывод через `docker compose logs ИМЯ --tail 50`, заменив `ИМЯ` именем сервиса. |
| `curl.exe -fsS …/_cluster/health` | Отправляет GET напрямую в Elasticsearch на опубликованный порт `9208`. `-f` возвращает ошибку для HTTP 4xx/5xx, `-s` убирает индикатор передачи, `-S` оставляет текст ошибок. Ожидай JSON с `status`. На одном узле `yellow` возможен, если для индекса требуется дополнительная реплика. Если соединение ещё отклоняется, повтори команду через несколько секунд. |
| `curl.exe -i …/ok` и `…/fail` | Отправляют GET через Nginx на порт `8088`. `-i` печатает строку статуса и заголовки вместе с телом. Ожидай `200` для `/ok` и `500` для `/fail`, в обоих ответах найди `X-Request-ID`. Ошибка `500` создана специально, чтобы найти запись из `stderr`. |

## Проследи одну запись

Сохрани `X-Request-ID` ответа `500`. Найди ту же строку в локальном логе, затем документ в Elasticsearch. При первом запросе дождись появления индекса.

```powershell
docker compose logs app --tail 20
docker compose logs nginx --tail 20
curl.exe -fsS http://localhost:9208/_cat/indices/learn-observability-*
$id = Read-Host 'X-Request-ID ответа 500'
(Invoke-RestMethod "http://localhost:9208/learn-observability-*/_search?q=request_id%3A$id").hits.hits |
    ForEach-Object { $_._source }
```

`logs app` читает последние строки контейнера, где видно поле `request_id` из JSON. `logs nginx` показывает access log прокси, где тот же ID стоит первым. Запрос `_cat/indices` печатает список созданных индексов с префиксом `learn-observability-`. Первой записи может понадобиться несколько секунд на доставку и индексацию.

`Read-Host` ждёт, пока ты вставишь только значение заголовка, и кладёт его в `$id`. `Invoke-RestMethod` вызывает Elasticsearch `_search` и преобразует JSON в объект PowerShell. `%3A` — закодированное двоеточие в условии `request_id:<ID>`. В ответе первое `hits` — раздел с результатами поиска, второе — список документов. Цикл `ForEach-Object` печатает `_source` каждого совпадения, то есть поля самого события.

Открой `http://localhost:5608`, в Discover создай data view `learn-observability-*` по `@timestamp` и найди событие через `request_id: "<ID>"`. Сверь `status=500` и `source=stderr`. Если документа нет, начни с `docker compose logs fluent-bit elasticsearch --tail 50`. В Elasticsearch проверь индекс, в Kibana — интервал времени.

## Метрики и граница прокси

Открой `http://localhost:9098/targets`, убедись, что `lab-app` в состоянии `UP`. Это означает, что Prometheus успешно прочитал метрики у приложения. Открой `http://localhost:3008` с локальным логином `admin` / `admin`, затем Explore → Prometheus. Запросы ниже вводи в редактор Grafana.

- `sum by (status) (lab_http_requests_total)` суммирует накопленные счётчики, оставляя по одному результату на HTTP-статус.
- `sum by (status) (rate(lab_http_requests_total[1m]))` вычисляет скорость прироста в секунду на окне в минуту и группирует результат по статусу. После нескольких `/ok` и `/fail` подожди как минимум два сбора метрик. Сравни, что происходит со счётчиком и скоростью, когда перестаёшь посылать запросы.

### Ограничь частоту запросов

Создай серию быстрых запросов к `/limited`. В access log сравни `upstream` и `limit` для `200` и `429`.

```powershell
curl.exe -s -w "`n%{http_code}`n" 'http://localhost:8088/limited?[1-10]'
docker compose logs nginx --tail 20
```

`[1-10]` в URL разворачивает сам curl. Он сделает десять GET с разными числами в query string, при этом путь каждого запроса останется `/limited`. `-w` допечатает `%{http_code}` после каждого ответа, а `` `n `` в двойных кавычках PowerShell означает перенос строки. В журнале Nginx смотри `limit=REJECTED` и `upstream=-` у `429`. Прокси отклонил запрос до передачи приложению, поэтому у приложения нет счётчика `429`.

### Останови хранилище логов

Останови Elasticsearch, отправь `/fail` и запиши его ID. Метрики должны продолжить обновляться. Изучи ошибки отправки Fluent Bit. Верни Elasticsearch, дождись повторной доставки и поищи запись. Объясни, что будет, если простой превысит размер буфера.

```powershell
docker compose stop elasticsearch
curl.exe -i http://localhost:8088/fail
docker compose logs fluent-bit --tail 30
docker compose start elasticsearch
```

`stop elasticsearch` останавливает один контейнер и сохраняет индекс в томе `elastic-data`. `curl` по-прежнему доходит через Nginx до приложения. В `logs fluent-bit` ищи ошибку подключения или повтор отправки. `start elasticsearch` поднимает тот же экземпляр. После восстановления повтори поиск по сохранённому ID.

### Останови приложение

Останови `app`, вызови `/ok` и найди `502` в Nginx. Через несколько секунд запрос `up{job="lab-app"}` в Prometheus покажет `0`. Верни приложение и повтори проверку. Сравни с `500`, который приложение вернуло самостоятельно.

```powershell
docker compose stop app
curl.exe -i http://localhost:8088/ok
docker compose logs nginx --tail 20
docker compose start app
```

`stop app` убирает upstream, поэтому `curl` получает ответ прокси. Prometheus делает следующий опрос и выставляет `up=0`. После `start app` ожидай `200` и `up=1`. Счётчики приложения хранятся в памяти процесса и начнутся с нуля после перезапуска.

Закончи, когда сможешь указать место появления каждого из `500`, `429` и `502` и назвать, какие сигналы дойдут до Elasticsearch и Prometheus. Затем выполни `docker compose down`. Чтобы удалить и учебные индексы, используй `docker compose down -v` в этом каталоге. История Prometheus и настройки Grafana без отдельного именованного тома могут быть потеряны при пересоздании контейнеров.

## Как устроены файлы стенда

### compose.yaml

[Файл Compose](compose.yaml) запускает сервисы в одной сети. К адресам в столбце «С компьютера» обращаются браузер и PowerShell. Внутренние адреса используют процессы в контейнерах.

| Сервис | С компьютера | Настройки и связь с соседями |
| --- | --- | --- |
| `app` | Через Nginx на `localhost:8088` | `node:22-alpine` содержит Node.js. Compose монтирует [app.mjs](app.mjs) как `/lab/app.mjs` и запускает `node /lab/app.mjs`. Приложение слушает `app:8080`. `logging.driver: fluentd` отправляет stdout/stderr через Docker-движок на `127.0.0.1:24224`. `fluentd-async` разрешает приложению стартовать, пока агент ещё недоступен. `tag: lab.app` используется в правилах Fluent Bit. |
| `nginx` | `localhost:8088` | Порт хоста `8088` направлен на порт `80`. `nginx.conf` монтируется на место стандартного конфига виртуального сервера. Upstream — `app:8080`. |
| `fluent-bit` | `127.0.0.1:24224` | Порт опубликован для Docker logging driver. Файл `fluent-bit.conf` заменяет конфиг в образе. Агент отправляет события в `elasticsearch:9200`. |
| `elasticsearch` | `localhost:9208` | `discovery.type: single-node` запускает один узел. `xpack.security.enabled: false` отключает аутентификацию для локального опыта. `node.store.allow_mmap: false` отключает отображение индексных файлов в память. `-Xms512m -Xmx512m` задаёт начальную и максимальную кучу JVM. Индексы хранятся в `elastic-data`. |
| `kibana` | `localhost:5608` | Контейнер слушает `5601`. `ELASTICSEARCH_HOSTS` задаёт список адресов Elasticsearch. Версия образа совпадает с версией Elasticsearch. |
| `prometheus` | `localhost:9098` | Контейнер слушает `9090` и загружает примонтированный `prometheus.yml`. Оттуда узнаёт адрес `app:8080`. |
| `grafana` | `localhost:3008` | Контейнер слушает `3000`. При запуске читает примонтированный `datasource.yml`, где указан `prometheus:9090`. |

### nginx.conf и app.mjs

В [nginx.conf](nginx.conf) `server` слушает `80`. Точное правило `location = /limited` перенаправляет этот путь в `/ok` приложения. `location /` обрабатывает остальные пути и передаёт их приложению. `limit_req_zone` заводит зону учёта `lab` объёмом `1m` и ключ по IP клиента. Скорость `2r/s` и `burst=2 nodelay` разрешают небольшой всплеск сразу. При превышении лимита `limit_req_status` задаёт `429`.

`proxy_set_header` передаёт сгенерированный Nginx `$request_id` приложению. `add_header … always` возвращает его клиенту, в том числе при ошибке. `log_format lab_access` задаёт поля access log, а `access_log /dev/stdout lab_access` отправляет строки в stdout Nginx. `$status` — ответ клиенту, `$upstream_status` — ответ или результат обращения к upstream, `$request_time` и `$upstream_response_time` помогают понять, где ушло время.

В [app.mjs](app.mjs) `/ok`, `/fail` и неизвестный путь дают `200`, `500` и `404`. Счётчик увеличивается по статусу. JSON события включает `request_id`, `path` и `status`. При `500` запись идёт в stderr, в остальных случаях — в stdout. `/metrics` возвращает счётчики в текстовом формате Prometheus и сам их не увеличивает.

### fluent-bit.conf

| Секция [fluent-bit.conf](fluent-bit.conf) | Что происходит |
| --- | --- |
| `[SERVICE]` | `Flush 1` задаёт интервал отправки в одну секунду, `Log_Level info` — подробность служебных логов. `Parsers_File` подключает встроенные парсеры, `storage.path` задаёт каталог буфера. |
| `[INPUT]` | `forward` слушает `24224` на интерфейсах контейнера и принимает записи от Docker-драйвера. `storage.type filesystem` разрешает хранить ожидающие отправки части потока на файловой системе контейнера. |
| `[FILTER]` | `Match lab.app` выбирает записи по тегу Docker. `Key_Name log` берёт строку с текстом приложения, `Parser json` извлекает поля из неё, `Reserve_Data On` сохраняет метаданные, включая `source`. |
| `[OUTPUT]` | `es` отправляет записи на `elasticsearch:9200`. `Logstash_Format` с префиксом формирует индекс по дате и добавляет `@timestamp`. `Suppress_Type_Name` подходит для Elasticsearch 8. `Generate_ID` помогает избежать повторного индексирования той же записи при retry. `Retry_Limit False` снимает ограничение на число повторов, а `storage.total_limit_size 32M` ограничивает размер очереди. |

Путь буфера находится в контейнере Fluent Bit. При его удалении буфер теряется. В опыте мы останавливаем только Elasticsearch, поэтому агент и его очередь остаются доступны.

### prometheus.yml и datasource.yml

[prometheus.yml](prometheus.yml) описывает сбор. `scrape_interval: 5s` задаёт частоту, `job_name: lab-app` добавляет метку `job`, а `static_configs.targets` указывает адрес приложения. По умолчанию путь сбора — `/metrics`.

[datasource.yml](datasource.yml) описывает чтение. `apiVersion` задаёт версию формата provisioning, `type: prometheus` — вид источника, `access: proxy` — запрос через сервер Grafana. `url` указывает на внутренний адрес Prometheus, `isDefault: true` выбирает его по умолчанию.

[Fluent Bit Elasticsearch output](https://docs.fluentbit.io/manual/data-pipeline/outputs/elasticsearch) · [Docker Fluentd logging driver](https://docs.docker.com/engine/logging/drivers/fluentd/)
