# RabbitMQ на практике

```powershell
docker compose --profile labs up -d --wait rabbitmq
.\mvnw.cmd "-Dlabs.external=true" "-Dtest=RabbitMqLabIntegrationTest" test
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged
```

Management UI на `http://localhost:15672`, вход `learn / learn`. Тесты удаляют свою топологию в конце. Чтобы посмотреть её во время обработки, остановись в тесте на breakpoint сразу после `basicGet(..., false)`. В UI рабочая очередь покажет одну `Unacked` доставку. Поставь второй breakpoint после закрытия канала и до нового получения. Запись вернётся в `Ready`.

## Код и сценарии

1. [WalletTopology](WalletTopology.java) объявляет direct exchange и три bindings. `wallet.credit` ведёт в рабочую очередь, `wallet.retry` в очередь ожидания, `wallet.dead` в DLQ. Exchange маршрутизирует, очередь хранит.
2. [ConfirmedPublisher](ConfirmedPublisher.java) включает confirms и посылает с `mandatory=true`. Отдельный канал и одна публикация в полёте позволяют сопоставить return с текущим сообщением. В тесте ключ `typo` получает `NO_ROUTE` даже при положительном confirm. При таймауте исход неизвестен. Повтор использует прежний event ID и новый канал. [Подтверждения RabbitMQ](https://www.rabbitmq.com/docs/confirms)
3. [WalletWorker](WalletWorker.java) читает с ручным ack. После успешного commit кошелька отправляет `basicAck`. Если канал закрыть между этими шагами, брокер повторит доставку. Inbox не даст повторно изменить баланс. Delivery tag относится к каналу, а message ID — к бизнес-событию.
4. При временной ошибке worker публикует копию в retry-очередь и подтверждает оригинал после успешного confirm без return. TTL возвращает копию в рабочую очередь через DLX. Тесты проверяют успешный retry, исчерпание попыток, некорректный payload и удаление binding для retry. В последнем случае исходное сообщение остаётся неподтверждённым. [Dead-lettering RabbitMQ](https://www.rabbitmq.com/docs/dlx)
5. [Тест prefetch](../../../../../test/java/ru/learn/rabbitmq/RabbitMqLabIntegrationTest.java) подписывается через `basic.consume`. При `basicQos(1)` второе сообщение приходит после ack первого. В остальных тестах `basic.get` позволяет остановиться между доставкой, бизнес-эффектом и ack.

## Опыт с отказом

- Замени ack после обработки на ack до неё. В тесте с закрытием канала имитируй сбой до изменения баланса. Увидишь пустую очередь и пропущенное пополнение.
- Замени `requeue=false` на `true` для некорректного сообщения. Вручную получи его несколько раз и объясни, почему образуется быстрый цикл. TTL retry и DLQ разделяют временные и постоянные ошибки.
- Поставь паузу между confirm повтора и ack оригинала. Закрытие канала оставляет оригинал для повторной доставки и может сохранить копию в retry. Лимит `lab-attempt` ограничивает число отложенных повторов для одной цепочки, а inbox защищает бизнес-эффект при дублировании цепочки.
- Увеличь prefetch до двух. Проверь, что обе записи приходят до первого ack. Объясни, как размер prefetch влияет на throughput, число `unacked` и перераспределение работы после обрыва связи.

В стенде используются classic queues. Встроенный перенос через DLX по умолчанию не даёт строгой гарантии сохранности при недоступности цели. Отдельно изучи quorum queues и условия at-least-once dead lettering, если потеря при переносе недопустима. Для production правила DLX удобнее задавать политикой, а возвращённые или зависшие сообщения нужно наблюдать и разбирать перед replay. [Dead-lettering RabbitMQ](https://www.rabbitmq.com/docs/dlx)

[Общий маршрут](../../../../../../LABS.md) · [Теория](../theory/MessageBrockers.md)
