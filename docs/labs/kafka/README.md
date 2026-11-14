# Репликация Kafka под отказом

[Конспект](../../theory/messaging/kafka.md) · [План Tech Lead и вопросы](../../interview/tech-lead.md#гарантии-kafka) · [Docker для начинающих](../../interview/tech-lead.md#как-запускать-стенды-в-docker) · [Разбор конфигурации](#как-устроен-composeyaml)

Три брокера держат реплики данных. Отдельный KRaft-контроллер обслуживает метаданные и остаётся доступным при остановке двух брокеров. Один контроллер выбран для эксперимента с ISR. Для устойчивого к отказу контроллера кворума нужны дополнительные голосующие узлы.

## Партиция и группа

Открой PowerShell в корне репозитория. Перейди в каталог Kafka и запусти чистый стенд. Дождись завершения `up` перед командами Kafka.

```powershell
Set-Location .\docs\labs\kafka
docker compose up -d --wait --wait-timeout 180
docker compose ps -a
```

`up -d` поднимает кластер в фоне. `--wait` ожидает, пока healthcheck пройдёт на трёх брокерах. Если через `180` секунд команда завершится ошибкой, `ps -a` покажет, какой контейнер остановился или не готов. `docker compose logs controller broker1 --tail 50` помогает увидеть проблемы при запуске. `controller` в `ps` должен быть запущен, отдельного `healthcheck` для него в файле нет.

Создай топик, посмотри назначение реплик и состояние кворума.

```powershell
docker compose exec -T broker1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server broker1:9092 --create --topic durability --replica-assignment 1:2:3 --config min.insync.replicas=2
docker compose exec -T broker1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server broker1:9092 --describe --topic durability
docker compose exec -T broker1 /opt/kafka/bin/kafka-metadata-quorum.sh --bootstrap-server broker1:9092 describe --status
```

Начало `docker compose exec -T broker1` означает, что Compose выполнит команду **внутри** контейнера `broker1`. Там уже есть скрипты Kafka в `/opt/kafka/bin`. `--bootstrap-server broker1:9092` передаётся скрипту Kafka. Это адрес первого контакта с кластером внутри сети Docker, после которого клиент получает метаданные. Все команды запускаются из PowerShell, установка Kafka на Windows для этого не требуется.

| Команда Kafka | Что делает и что смотреть |
| --- | --- |
| `kafka-topics.sh --create --topic durability` | Создаёт топик `durability`. `--replica-assignment 1:2:3` задаёт одну партицию с репликами на брокерах `1`, `2`, `3`. Первый брокер в списке — предпочтительный лидер. `--config min.insync.replicas=2` задаёт порог именно для этого топика. |
| `kafka-topics.sh --describe --topic durability` | Показывает конфигурацию и строку для `Partition: 0`. `Replicas` — назначенные реплики, `Isr` — текущие синхронизированные реплики, `Leader` — кто сейчас принимает запись. |
| `kafka-metadata-quorum.sh … describe --status` | Показывает кворум метаданных. Ищи `LeaderId: 100` и контроллер `100` среди голосующих. Это лидер кворума контроллеров. В выводе `kafka-topics.sh` поле `Leader` относится к пользовательской партиции. |

Убедись, что `Leader: 1`, `Replicas` содержит `1,2,3`, а `Isr` содержит все три ID. Следующая команда нужна, только если лидером стал другой брокер. Дождись полного ISR и попроси контроллер вернуть партицию `0` предпочтительному лидеру. После команды повтори `--describe` и продолжай с лидером `1`.

```powershell
docker compose exec -T broker1 /opt/kafka/bin/kafka-leader-election.sh --bootstrap-server broker1:9092 --election-type preferred --topic durability --partition 0
```

### Отправь первую запись

Для краткости введи две функции в одном окне PowerShell. Producer делает синхронную отправку с `acks=all`. Повторы отключены, чтобы при отказе увидеть ответ брокера сразу. После изменения топологии проверяй ISR перед новой записью.

```powershell
function Show-Isr {
    docker compose exec -T broker1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server broker1:9092 --describe --topic durability
}

function Send-KafkaRecord([string]$value) {
    $value | docker compose exec -T broker1 /opt/kafka/bin/kafka-console-producer.sh `
        --bootstrap-server broker1:9092 --topic durability --sync `
        --producer-property acks=all --producer-property enable.idempotence=false `
        --producer-property retries=0 --producer-property max.block.ms=10000
}

Send-KafkaRecord 'isr-3'
```

`Show-Isr` повторяет `kafka-topics.sh --describe`. `Send-KafkaRecord` принимает строку `$value`, а `|` передаёт её в стандартный ввод `kafka-console-producer.sh`. `--topic durability` выбирает журнал, `--sync` ждёт результата каждой отправки. У успешной записи может не быть отдельной строки `OK`, поэтому в конце прочитаем топик и сверим содержимое.

- `--producer-property` передаёт настройку Java-клиенту Kafka. `acks=all` ждёт подтверждения по всему текущему ISR при допустимом размере ISR.
- `retries=0` убирает автоматические повторы, чтобы наблюдать ошибку на одной попытке. `enable.idempotence=false` позволяет использовать `retries=0` в этом опыте. Для разбора идемпотентного producer смотри [теорию](../../interview/tech-lead.md#гарантии-kafka).
- `max.block.ms=10000` ограничивает ожидание `send` на этапе получения метаданных и выделения буфера десятью секундами. Ответ брокера имеет собственные таймауты.

### Подключи двух участников одной группы

Открой ещё два окна PowerShell и в каждом перейди в каталог Kafka. В каждом окне запусти следующую команду и оставь её работать. После стабилизации группы запись напечатает один consumer.

```powershell
docker compose exec -T broker1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server broker1:9092 --topic durability --group drill --from-beginning --property print.offset=true
```

`kafka-console-consumer.sh` читает топик. `--group drill` присоединяет оба процесса к одной группе. `--from-beginning` читает с начала при отсутствии сохранённого offset, а `--property print.offset=true` печатает offset рядом с записью. Оба процесса можно запустить через один контейнер, потому что каждый вызов `exec` создаёт отдельный процесс consumer.

Вернись в первое окно, где определены функции. Выполни две диагностические команды.

```powershell
docker compose exec -T broker1 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server broker1:9092 --describe --group drill --members --verbose
docker compose exec -T broker1 /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server broker1:9092 --describe --group drill
```

`kafka-consumer-groups.sh --describe --group drill --members --verbose` показывает участников и назначенные партиции. Ищи одного участника с `durability(0)` и второго с нулём партиций. Вторая команда показывает `CURRENT-OFFSET`, `LOG-END-OFFSET` и `LAG`. После auto-commit первой записи `CURRENT-OFFSET` станет `1`, поскольку это позиция следующего чтения. У первой записи offset `0`.

Останови consumer, владеющего партицией, через Ctrl+C. Дождись назначения партиции оставшемуся участнику, выполни `Send-KafkaRecord 'after-rebalance'` и проверь, кто прочитал запись. Заверши оба consumer перед экспериментом с ISR.

## ISR 3 → 2 → 1 → 3

Выполняй команды этого опыта в первом окне. Сначала останови только `broker3`. `stop` сохраняет контейнер и его локальные данные. Повторяй `Show-Isr`, пока `Isr` не покажет `1,2`, и затем отправь запись.

```powershell
docker compose stop broker3
Show-Isr
```

```powershell
Send-KafkaRecord 'isr-2'
```

`Replicas` по-прежнему содержит три ID, а ISR уменьшился. Запись `isr-2` проходит, потому что двух реплик достаточно для `min.insync.replicas=2`. Теперь останови `broker2`. Дождись `Isr: 1`, прежде чем запускать producer.

```powershell
docker compose stop broker2
Show-Isr
```

```powershell
Send-KafkaRecord 'isr-1-expected-rejection'
```

При ISR из одной реплики в стабильном состоянии ожидай `NotEnoughReplicasException`. Если отправить прямо во время изменения ISR, возможна ошибка после append. Запусти оба остановленных контейнера командой `start`. Повторяй `Show-Isr`, пока реплики не догонят лидера, затем отправь `recovered`.

```powershell
docker compose start broker2 broker3
Show-Isr
```

```powershell
Send-KafkaRecord 'recovered'
```

В конце прочитай топик новой группой `verify`. У неё ещё нет сохранённой позиции, поэтому `--from-beginning` начнёт с offset `0`. В чистом прогоне увидишь четыре записи в порядке `isr-3`, `after-rebalance`, `isr-2`, `recovered`.

```powershell
docker compose exec -T broker1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server broker1:9092 --topic durability --group verify --from-beginning --timeout-ms 8000 --property print.offset=true
```

`--timeout-ms 8000` завершает consumer после восьми секунд ожидания новых сообщений. В этом шаге таймаут после чтения ожидаем. Проверь значения и offsets в его выводе.

Заверши `docker compose down -v`. Команда удаляет учебные контейнеры и их данные. Для эксперимента с повторами при обработке и Kafka-транзакциями вернись к [интеграционным тестам репозитория](../../../src/main/java/ru/learn/kafka/README.md).

## Как устроен compose.yaml

В [compose.yaml](compose.yaml) проект называется `learn-kafka-replication`. Все четыре контейнера используют один образ `apache/kafka:3.9.0`. Блок `x-environment` хранит общие параметры, `x-broker-environment` дополняет их настройками брокера, а `x-broker` описывает образ, зависимость и healthcheck. Записи `<<: *broker` и `<<: *broker-environment` подставляют поля в конкретный сервис. Посмотреть итог для всех сервисов можно через `docker compose config`.

| Поле | Как оно влияет на стенд |
| --- | --- |
| `CLUSTER_ID` | Общий идентификатор кластера при инициализации хранилища Kafka. Он одинаков у брокеров и контроллера. |
| `KAFKA_PROCESS_ROLES`, `KAFKA_NODE_ID` | Контроллер `100` отвечает за метаданные, брокеры `1`, `2`, `3` хранят партиции и обслуживают клиентов. ID должны быть уникальны в кластере. |
| `KAFKA_CONTROLLER_QUORUM_VOTERS` | `100@controller:9093` означает, что единственный голосующий узел с ID `100` доступен по имени `controller` и порту `9093`. В лабораторной контроллер переживает остановку двух **брокеров**, потому что он отдельный процесс. |
| `KAFKA_LISTENERS` | Определяет, на каком порту процесс слушает внутри контейнера. `CONTROLLER://:9093` нужен контроллеру, `INTERNAL://:9092` — брокерам. Пустой хост после `://` означает прослушивание интерфейсов контейнера. |
| `KAFKA_ADVERTISED_LISTENERS` | Адрес, который Kafka возвращает клиентам. Например, брокер 2 рекламирует `broker2:9092`, чтобы клиенты в сети Compose могли найти его после получения метаданных. Порты брокеров на Windows не опубликованы, поэтому CLI запускается через `exec`. |
| `KAFKA_CONTROLLER_LISTENER_NAMES`, `KAFKA_INTER_BROKER_LISTENER_NAME`, `KAFKA_LISTENER_SECURITY_PROTOCOL_MAP` | Выбирают имена listener для связи с контроллером и между брокерами и сопоставляют их протоколам. Оба используют `PLAINTEXT` внутри учебной сети. |
| `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR` | Задаёт RF=3 для внутреннего `__consumer_offsets`, где хранятся позиции групп. RF топика `durability` задан отдельно в `--replica-assignment`. |
| `KAFKA_AUTO_CREATE_TOPICS_ENABLE`, `KAFKA_UNCLEAN_LEADER_ELECTION_ENABLE` | Выключают автоматическое создание топиков и выбор лидера из отставших реплик. Благодаря первой настройке ошибочное имя топика не создаст ещё один журнал. |
| `KAFKA_HEAP_OPTS` | Задаёт начальную кучу JVM `192m` и максимум `384m`. |
| `depends_on`, `healthcheck` | Брокеры стартуют после запуска контейнера контроллера. Проверка внутри каждого брокера вызывает `kafka-broker-api-versions.sh` на его локальном `9092`. Проверки выполняются примерно каждые пять секунд, с десятисекундным таймаутом на попытку и `25` допустимыми последовательными неуспехами до состояния `unhealthy`. Полный ISR всё равно проверяем отдельной командой. |

[KRaft](https://kafka.apache.org/39/operations/kraft/) · [Настройки брокера Kafka](https://kafka.apache.org/39/configuration/broker-configs/)
