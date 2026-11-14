# Kafka на практике

[Общий маршрут](../../../../../../LABS.md) · [Теория](../../../../../../docs/theory/messaging/kafka.md)

## 1. Публикация и чтение на стенде

```powershell
docker compose up -d --wait kafka
$topics = 'learn.orders', 'learn.orders.processed', 'learn.tx.orders', 'learn.tx.order-audit'
foreach ($topic in $topics) {
    docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --create --if-not-exists --topic $topic --partitions 1 --replication-factor 1
}
```

Для ручного прогона Spring listener запусти `docker compose up -d postgres`, затем `LearnApplication` из IDE с переменной `SPRING_LIQUIBASE_ENABLED=false`. Spring Boot берёт адрес PostgreSQL из Compose. Для опытов с Java-кодом можно сразу перейти к тестам ниже, они поднимают собственный брокер и при необходимости БД. Для ручной публикации открой CLI и введи `order-1:created`:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:29092 --topic learn.orders --property parse.key=true --property key.separator=:
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:29092 --topic learn.orders.processed --from-beginning --isolation-level read_committed --property print.key=true --property print.offset=true
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --describe --group learn-orders-exactly-once
```

Команды выполняй в разных терминалах. `CURRENT-OFFSET` — следующая позиция группы. `LOG-END-OFFSET` — конец журнала, а `LAG` — разница позиций. Адрес приложения на хосте — `localhost:9092`, его можно заменить через `KAFKA_BOOTSTRAP_SERVERS`.

### Как читать Spring-код

- [OrderProducer](OrderProducer.java) использует `executeInTransaction` для публикации заказа. Вызов ждёт завершения транзакции. Возвращаемый future относится к отдельному `send`, а ошибки commit могут выйти из самого вызова.
- [KafkaListenerConfiguration](KafkaListenerConfiguration.java) задаёт контейнер с ручным ack и контейнер с менеджером Kafka-транзакций.
- [OrderListeners](OrderListeners.java) показывает обе конфигурации. Разные `groupId` позволяют каждому listener независимо обработать один и тот же вход. При исключении поведение повторной обработки зависит от настроек error handler и after-rollback processor.

## 2. Гарантии доставки

```powershell
.\mvnw.cmd "-Dtest=KafkaDeliveryIntegrationTest,WalletLedgerTest" test
```

[OffsetCommitLab](delivery/OffsetCommitLab.java) сравнивает порядок действий на реальном брокере. [Тест доставки](../../../../../test/java/ru/learn/kafka/delivery/KafkaDeliveryIntegrationTest.java) закрывает старый consumer и открывает новый с той же группой. Ручное назначение партиции позволяет отдельно изучить commit без влияния rebalance.

| Опыт | Что случилось до сбоя | Что увидит новый consumer |
|---|---|---|
| At-most-once | Offset уже зафиксирован, бизнес-операция ещё не выполнена | Следующую запись. Пополнение пропущено. |
| At-least-once | Пополнение выполнено, offset ещё не зафиксирован | Ту же запись. Без inbox баланс увеличится со 100 до 200. |
| At-least-once с inbox | Пополнение и маркер события зафиксированы одной транзакцией БД | Ту же запись и отдельную повторную публикацию с новым offset. Баланс остаётся 100. |

`poll()` сдвигает локальную позицию. При перезапуске исходной точкой служит committed offset. Коммит записи с offset `n` сохраняет `n + 1`. Если обработал только часть пачки, подтверждай только непрерывный обработанный префикс каждой партиции. [KafkaConsumer API](https://kafka.apache.org/39/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)

`acks=all` ждёт подтверждений текущего ISR, а `min.insync.replicas` задаёт минимальное число синхронных реплик для такой записи. На одном брокере эта настройка не даёт отказоустойчивости. `enable.idempotence=true` устраняет дубли при сетевых ретраях producer в рамках его протокола. Тест с двумя отдельными `send` показывает, зачем ещё нужен стабильный event ID.

## 3. Транзакции

```powershell
.\mvnw.cmd "-Dtest=AtomicOrderPublisherIntegrationTest,KafkaExactlyOnceIntegrationTest,SpringKafkaTransactionIntegrationTest" test
```

1. [AtomicOrderPublisher](transactions/AtomicOrderPublisher.java) публикует заказ и аудит в два топика. Тесты показывают, что после commit оба события видны, а выброшенное исключение вызывает abort.
2. [Тест Kafka API](../../../../../test/java/ru/learn/kafka/KafkaExactlyOnceIntegrationTest.java) явно вызывает `beginTransaction`, `abortTransaction`, `sendOffsetsToTransaction` и `commitTransaction`. Первая попытка физически попадает в журнал. `read_uncommitted` видит её, `read_committed` пропускает. Во второй попытке Kafka фиксирует выход и offset входной группы вместе.
3. [Тест контейнера Spring](../../../../../test/java/ru/learn/kafka/transactions/SpringKafkaTransactionIntegrationTest.java) выбрасывает исключение после успешного `send().join()`. Контейнер откатывает транзакцию и повторно читает вход. Два вызова обработчика дают один видимый результат для `read_committed`, а входной offset становится равен 1.
4. Отдельный тест удерживает транзакцию открытой, пока другой producer публикует обычное сообщение в ту же партицию. `read_committed` упирается в Last Stable Offset. После abort он пропускает отменённую запись и читает более позднее сообщение. [Изоляция в KafkaConsumer](https://kafka.apache.org/39/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)

Exactly-once здесь относится к атомарному Kafka → Kafka: выходные записи и offsets. Сам обработчик может исполняться несколько раз. Для изменения отдельной БД смотри [WalletLedger](../delivery/WalletLedger.java), для публикации после изменения БД — [Outbox](../../../../../../docs/theory/messaging/delivery-patterns.md#9-outbox-и-cdc).

## 4. Эксперименты перед собеседованием

- Перенеси commit offset до обработки в тесте с inbox. Вызови сбой до транзакции БД и объясни, почему inbox не может восстановить уже пропущенное событие.
- Убери передачу offset в Kafka-транзакцию, оставив публикацию результата. Перезапусти входной consumer и проверь, почему он снова читает вход с прежнего offset.
- Переключи выходного читателя на `read_uncommitted`. Найди отменённую попытку и объясни, почему «одна запись физически в логе» и «один зафиксированный результат» — разные утверждения.
- В тесте Last Stable Offset замени abort на commit и прочитай обе записи по порядку. Сопоставь долгую транзакцию с симптомом «producer пишет, consumer не видит более поздние события».
- Объясни, как разделить идентификатор агрегата для порядка в партиции и идентификатор события для дедупликации. В примере кошелька event ID лежит в key для простоты. В рабочей модели это могут быть разные поля.
