# Практикум по Redis, Kafka, RabbitMQ, MongoDB и Data Lake

[Маршрут собеседований](INTERVIEW.md) · [Лабораторные по Spring, PostgreSQL и Outbox/Inbox](docs/interview/labs.md)

Лабораторные запускаются как интеграционные тесты. Тест играет роль клиента приложения, управляет моментом отказа и проверяет результат на брокере, в Redis или в БД. Для каждой работы сначала предскажи, что будет видно после сбоя, затем поставь breakpoint и проверь состояние.

## Запуск

Из корня проекта, с JDK 17 или новее и запущенным Docker:

```powershell
docker compose --profile labs up -d --wait kafka redis rabbitmq mongo lake-s3
docker compose --profile labs ps
.\mvnw.cmd "-Dlabs.external=true" test
```

В Bash используй `./mvnw`. Kafka-тесты используют Embedded Kafka на случайных портах и запускаются без Compose. Флаг `labs.external=true` включает тесты Redis, RabbitMQ, MongoDB и Data Lake с реальными сервисами Compose. Если флаг задан, недоступность нужного сервиса приводит к ошибке. Каждый тест изолирует данные своим префиксом, топологией, базой или bucket. БД кошелька создаётся заново в H2 в режиме PostgreSQL. Адреса и учётные данные предназначены для локального стенда.

| Сервис | Адрес с хоста | Настройки |
|---|---|---|
| Redis | `redis://localhost:6379` | Переопределение через `LAB_REDIS_URI` |
| RabbitMQ | `amqp://learn:learn@localhost:5672` | Переопределение через `LAB_RABBIT_URI`. Management UI на `http://localhost:15672` |
| Kafka | `localhost:9092` | Spring-приложение читает `KAFKA_BOOTSTRAP_SERVERS`. В контейнерной сети адрес `kafka:29092` |
| MongoDB | `mongodb://localhost:27017/?replicaSet=lab-rs` | Переопределение через `LAB_MONGO_URI` |
| S3 для Data Lake | `http://localhost:8333` | `LAB_S3_ENDPOINT`, `LAB_S3_ACCESS_KEY`, `LAB_S3_SECRET_KEY` |

`docker compose --profile labs stop kafka redis rabbitmq mongo lake-s3` останавливает стенд. У Redis, RabbitMQ, MongoDB и SeaweedFS есть именованные тома. Kafka в этой конфигурации хранит данные в контейнере, его пересоздание сбросит стенд. Сервисы одноузловые.

## Маршрут изучения

| Этап | Что воспроизвести | Что проверить |
|---|---|---|
| [Redis](src/main/java/ru/learn/redis/README.md) | Cache miss и hit, истечение TTL, гонка инвалидации, лимит запросов, резерв последней единицы, восстановление pending в Streams | Вызовы источника, `PTTL`, остаток, повтор того же request ID, `XPENDING` |
| [Kafka, доставка](src/main/java/ru/learn/kafka/README.md#2-гарантии-доставки) | Остановить обработчик до или после commit offset, запустить заново с той же группой | Committed offset, количество фактических применений, баланс |
| [Kafka, транзакции](src/main/java/ru/learn/kafka/README.md#3-транзакции) | Отменить публикацию, затем отменить обработку после `send` и дождаться retry | Видимость в двух режимах изоляции, Last Stable Offset, выход и входной offset |
| [RabbitMQ](src/main/java/ru/learn/rabbitmq/README.md) | Ошибиться в routing key, закрыть канал до ack, исчерпать отложенные повторы | Confirm и return, флаг redelivered, заголовки DLQ, счётчик попыток |
| [MongoDB](src/main/java/ru/learn/mongo/README.md) | Запросить историю заказов с индексом и без него, откатить списание нескольких SKU, восстановить change stream | План выполнения, остатки и заказ после rollback, resume token |
| [Data Lake](src/main/java/ru/learn/datalake/README.md) | Загрузить Bronze, пересобрать Parquet после поздних данных, оставить незавершённую публикацию, запустить конкурирующую сборку | Карантин, партиции по дате события, старый и новый snapshot, конфликт ETag |

## Разбор одного инцидента тремя способами

У клиента истёк таймаут, хотя запрос мог выполниться. Повтори один и тот же идентификатор и проследи путь.

1. **Redis.** Потерян ответ резерва. `StockReservations` сначала проверяет маркер запроса, затем остаток. Даже когда остаток уже нулевой, повтор возвращает `ALREADY_RESERVED`. В опыте со Streams обработчик после сбоя забирает pending-запись через `XAUTOCLAIM` и подтверждает её после проверки inbox.
2. **Kafka.** Отдельные `send` дают отдельные offsets даже при включённой идемпотентности producer. Повтор из-за неподтверждённого offset возвращает прежнюю запись. В обоих случаях `WalletLedger` сверяет стабильный event ID и меняет баланс один раз.
3. **RabbitMQ.** Канал оборвался после commit БД до ack. Брокер повторяет доставку. `WalletLedger` пропускает уже применённое событие, после чего consumer подтверждает доставку на новом канале.

В [WalletLedger](src/main/java/ru/learn/delivery/WalletLedger.java) уникальный индекс защищает от гонки обработчиков, а commit БД объединяет отметку inbox и изменение баланса. [Тесты inbox](src/test/java/ru/learn/delivery/WalletLedgerTest.java) отдельно проверяют гонку и откат. Для обратного направления пройди [практику Outbox → Kafka → Inbox](docs/interview/labs.md#кошелёк-outbox-kafka-и-inbox) и сверься с [описанием Outbox и CDC](src/main/java/ru/learn/theory/MessageBrockers.md#9-outbox-и-cdc).

## Контрольное собеседование

- Потребитель Kafka сделал `poll()`, применил первые две записи из пачки и упал. Какой offset увидит новый процесс? Объясни разницу между локальной позицией и зафиксированной, покажи на `OffsetCommitLab`.
- Почему Kafka-транзакция не защитит начисление в БД? Обозначь две границы commit и покажи, как inbox переживает окно между ними.
- Почему producer RabbitMQ получил ACK, но очередь пуста? Проверь exchange, binding и обязательный return. Затем объясни, чем publisher confirm отличается от ack потребителя.
- Как проверить, почему вырос Kafka lag, число RabbitMQ `unacked` или нагрузка на источник после истечения Redis TTL? Для каждого случая назови конкретную метрику, гипотезу и один эксперимент.
- Что произойдёт при падении единственного брокера или Redis? Объясни границу стенда и отдельно назови, что в production проверил бы под отказом реплики.
- Где в MongoDB достаточно условного обновления одного документа, а где операция требует одной транзакции между несколькими документами? Покажи обе границы на остатках товара.
- В Data Lake пришло исправление к событию за вчера. Какие данные читаешь и переписываешь, где происходит commit и что увидит читатель, начавший запрос до публикации?

Теория для повторения — [брокеры](src/main/java/ru/learn/theory/MessageBrockers.md), [Redis](src/main/java/ru/learn/theory/RedisAndCaching.md), [MongoDB](src/main/java/ru/learn/theory/MongoDB.md) и [Data Lake](src/main/java/ru/learn/theory/DataLake.md).
