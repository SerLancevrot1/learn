# RabbitMQ: очереди, маршрутизация и подтверждения

[Брокеры сообщений](README.md) · [Практикум RabbitMQ](../../../src/main/java/ru/learn/rabbitmq/README.md) · [Идемпотентная обработка](delivery-patterns.md#8-идемпотентный-consumer-и-inbox)

[Exchange и binding](#121-модель-exchange-binding-queue) · [Подтверждения и prefetch](#122-подтверждения-и-prefetch) · [Типы очередей](#123-типы-очередей-и-rabbitmq-4x) · [Разбор доставки](#разбор-доставки-rabbitmq)

> В исходной проверке конспекта RabbitMQ не запускали. Репозиторий содержит отдельные интеграционные тесты для сценариев в разделе «Разбор доставки RabbitMQ».

## 12.1. Модель exchange, binding, queue

`[ ]` **Короткий ответ:** producer публикует сообщение не в очередь, а в exchange с routing key. Exchange по правилам bindings копирует сообщение в одну или несколько очередей. Consumer читает из очереди. Типы exchange:

| Тип | Маршрутизация | Пример |
|---|---|---|
| `direct` | Точное совпадение routing key и binding key | `payments.failed` → очередь алертов |
| `topic` | Шаблон: `*` — одно слово, `#` — ноль и более | `order.*.created`, `order.#` |
| `fanout` | Во все привязанные очереди, ключ игнорируется | Широковещательные уведомления |
| `headers` | По заголовкам сообщения | Редко |
| Default exchange (`""`) | Routing key = имя очереди | Простейший случай |

## 12.2. Подтверждения и prefetch

`[ ]` **Короткий ответ:** надёжность в RabbitMQ — это три вещи одновременно. Publisher confirms — брокер подтверждает, что принял сообщение (для replicated queue — после записи на кворум). Durable queue и persistent message — переживают перезапуск. Manual ack у consumer — сообщение удаляется только после `basicAck`; если consumer упал без ack, сообщение вернётся в очередь. Prefetch (`basic.qos`) ограничивает число неподтверждённых сообщений на consumer: без него брокер может отдать одному медленному consumer тысячи сообщений.

<details>
<summary>ack, nack, reject и dead lettering</summary>

- `basicAck` — обработано. `basicNack(requeue=true)` — вернуть в очередь (опасно: poison message будет крутиться бесконечно). `basicNack/basicReject(requeue=false)` — отбросить или отправить в dead letter exchange (DLX), если он настроен.
- DLX настраивается аргументом или политикой очереди (`x-dead-letter-exchange`). Сообщение попадает туда при reject без requeue, истечении TTL, переполнении очереди, превышении лимита доставок.
- Отложенный retry классически делают через очередь с TTL без consumers, у которой DLX указывает обратно на рабочий exchange.
- Quorum queues считают попытки доставки и поддерживают лимит (`delivery-limit`), после которого сообщение уходит в DLX.
- Autoack (`autoAck=true`) — at-most-once: сообщение считается доставленным в момент отправки consumer.

</details>

## 12.3. Типы очередей и RabbitMQ 4.x

`[ ]` **Короткий ответ:** в RabbitMQ 4.0 классическое зеркалирование очередей (mirrored classic queues) окончательно удалено после трёх лет устаревания. Для репликации используют quorum queues (Raft) или streams; classic queues остаются, но теперь не реплицируются ([что нового в RabbitMQ 4.0](https://blog.rabbitmq.com/docs/4.0/whats-new)). После обновления параметры зеркалирования в политиках просто перестают действовать ([релиз 4.0.1](https://github.com/rabbitmq/rabbitmq-server/releases/tag/v4.0.1)).

| Тип | Репликация | Для чего |
|---|---|---|
| Classic | Нет (с 4.0) | Временные, некритичные очереди, где потеря при отказе узла допустима |
| Quorum | Raft, кворум реплик | Надёжные рабочие очереди по умолчанию ([документация quorum queues](https://www.rabbitmq.com/docs/quorum-queues)) |
| Stream | Реплицируемый журнал | Replay, много потребителей одного потока, большие объёмы |

**Уточняющие вопросы:**

- Как масштабировать чтение? Несколько consumers на одну очередь (competing consumers); порядок при этом между ними не гарантирован.
- Что будет, если consumers нет, а сообщения идут? Очередь растёт, расходует память и диск; срабатывают memory/disk alarms, и брокер блокирует publishers. Нужны лимиты длины очереди и мониторинг.

---

## Разбор доставки RabbitMQ

`[ ]` **Короткий ответ.** Publisher confirm сообщает исход публикации со стороны брокера. При `mandatory=true` отдельно проверяю `basic.return`, чтобы обнаружить отсутствие маршрута. Потребитель отправляет свой ack после commit бизнес-операции. Обрыв до ack может вернуть доставку в очередь, поэтому для начисления нужен стабильный event ID.

<details>
<summary>От routing key до повторной доставки и DLQ</summary>

1. [WalletTopology](../../../src/main/java/ru/learn/rabbitmq/WalletTopology.java) связывает direct exchange с тремя очередями. `wallet.credit` ведёт в рабочую очередь, `wallet.retry` в очередь ожидания, `wallet.dead` в DLQ. В [ConfirmedPublisher](../../../src/main/java/ru/learn/rabbitmq/ConfirmedPublisher.java) один канал и одна публикация в полёте позволяют сопоставить return с сообщением. Ключ `typo` даёт `NO_ROUTE` даже при положительном confirm. При таймауте исход неизвестен, поэтому повтор сохраняет исходный message ID. [Подтверждения RabbitMQ](https://www.rabbitmq.com/docs/confirms)
2. [WalletWorker](../../../src/main/java/ru/learn/rabbitmq/WalletWorker.java) вызывает `basicAck` после возврата обработчика. В тесте commit кошелька уже прошёл, но канал закрывается до ack. На новом канале брокер доставляет событие повторно, а inbox сохраняет одно начисление. Ack использует delivery tag текущего канала, дедупликация использует message ID.
3. При временной ошибке worker публикует копию с увеличенным `lab-attempt` и подтверждает исходную доставку после успешной публикации. TTL в retry-очереди запускает перенос обратно в рабочую очередь через DLX. Некорректный payload или исчерпание попыток приводят к `basicReject(tag, false)`, который в этой топологии направляет сообщение в DLQ. При отсутствии binding для retry исходная доставка остаётся unacked. В учебных classic queues перенос через DLX сам по себе может потерять сообщение при недоступной цели. [Dead-lettering RabbitMQ](https://www.rabbitmq.com/docs/dlx)
4. Тест prefetch использует подписку `basic.consume`. При `basicQos(1)` брокер ждёт первого ack перед второй доставкой этому подписчику. Остальные опыты используют `basic.get`, чтобы точно остановиться между получением и ack. [Prefetch и подтверждения](https://www.rabbitmq.com/docs/confirms)

</details>

**Уточняющий вопрос.** Что останется, если связь оборвётся после confirm копии для retry, но до ack оригинала? Возможны две цепочки обработки: оригинал вернётся в рабочую очередь, а копия вернётся туда позже по TTL. Счётчик попыток ограничивает одну цепочку. Общий event ID позволяет inbox защитить баланс от обеих.

**Практика.** В [RabbitMqLabIntegrationTest](../../../src/test/java/ru/learn/rabbitmq/RabbitMqLabIntegrationTest.java) начни с `mandatoryReturnDetectsUnroutableMessageDespiteConfirm`. Затем в тесте закрытия канала проследи `Unacked → Ready → redelivered` и проверь баланс после нового ack. [Команды запуска и Management UI](../../../src/main/java/ru/learn/rabbitmq/README.md)

## Источники

- [What's New in RabbitMQ 4.0](https://blog.rabbitmq.com/docs/4.0/whats-new), [RabbitMQ 4.0.1](https://github.com/rabbitmq/rabbitmq-server/releases/tag/v4.0.1), [Quorum Queues](https://www.rabbitmq.com/docs/quorum-queues).
