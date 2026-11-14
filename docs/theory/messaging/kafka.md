# Kafka: журнал, группы потребителей и транзакции

[Брокеры сообщений](README.md) · [Лабораторные Kafka](../../../src/main/java/ru/learn/kafka/README.md) · [Стенд с тремя брокерами](../../labs/kafka/README.md)

[Топики и репликация](#3-устройство-kafka) · [Producer](#4-producer-ключи-acks-идемпотентность) · [Consumer groups](#5-consumer-groups-и-offset) · [Порядок](#6-порядок-сообщений) · [Гарантии доставки](#7-гарантии-доставки-и-транзакции) · [Share groups](#11-share-groups-очереди-в-kafka) · [Производительность](#13-пропускная-способность-kafka)

[Опыт: commit offset и повторы](#разбор-гарантий-доставки-kafka) · [Опыт: commit и abort транзакции](#разбор-транзакций-kafka)

> Примеры Kafka проверялись на **Apache Kafka 4.4.0** (KRaft, один брокер) и **OpenJDK 25**: порядок по ключу, consumer lag, значения по умолчанию producer и consumer, транзакции с `read_committed`, share groups.

## На интервью

Назови область порядка, момент фиксации offset, поведение при повторной доставке и границы транзакции. **Exactly-once при обработке Kafka → Kafka** связывает выходные записи и offset. Внешний эффект нужно защищать отдельно.

**Код.** [Два listener с разными группами](../../../src/main/java/ru/learn/kafka/OrderListeners.java) · [Транзакционная публикация в два топика](../../../src/main/java/ru/learn/kafka/transactions/AtomicOrderPublisher.java) · [Тест abort и повторной обработки](../../../src/test/java/ru/learn/kafka/KafkaExactlyOnceIntegrationTest.java) · [Тест commit и rollback](../../../src/test/java/ru/learn/kafka/transactions/AtomicOrderPublisherIntegrationTest.java).

**Что проверить в тесте.** Предскажи, что увидят `read_committed` и `read_uncommitted` после abort. Найди проверку offset после commit. Затем объясни, что изменится при записи результата в отдельную БД.

**Запуск тестов из корня.** `.\mvnw.cmd '-Dtest=KafkaExactlyOnceIntegrationTest,AtomicOrderPublisherIntegrationTest' test`. Тесты поднимают Embedded Kafka. Для запуска приложения см. [инструкцию](../../../src/main/java/ru/learn/kafka/README.md). [Границы гарантий в документации Kafka](https://kafka.apache.org/34/design/design/)

## 3. Устройство Kafka

### 3.1. Топик, партиция, offset

`[ ]` **Короткий ответ:** топик — логическое имя потока, физически разбитое на партиции. Партиция — упорядоченный журнал только на дозапись; каждая запись получает возрастающий номер — offset. Партиция хранится на диске сегментами. Партиция реплицируется: одна реплика — лидер (принимает запись и чтение), остальные — followers, копируют данные. Партиция — единица параллелизма и порядка.

```text
topic orders, 3 партиции
 P0: [0][1][2][3][4] ...   лидер на broker-1, реплики на 2 и 3
 P1: [0][1][2] ...         лидер на broker-2
 P2: [0][1][2][3] ...      лидер на broker-3
```

<details>
<summary>Retention и compaction</summary>

- **Retention по времени или объёму** (`retention.ms`, `retention.bytes`): старые сегменты удаляются целиком. По умолчанию 7 дней. Сообщения удаляются независимо от того, прочитаны они или нет.
- **Log compaction** (`cleanup.policy=compact`): для каждого ключа остаётся последнее значение. Топик превращается в «снимок состояния» — так хранится `__consumer_offsets`, так делают changelog-топики Kafka Streams. Запись с ключом и `null`-значением (tombstone) означает удаление ключа.
- Можно совместить: `cleanup.policy=compact,delete`.
- Offset внутри партиции не переиспользуется, после compaction в нумерации появляются пропуски.

</details>

### 3.2. Репликация

- **Replication factor** — число копий партиции, обычно 3.
- **ISR (in-sync replicas)** — реплики, которые не отстают от лидера дольше `replica.lag.time.max.ms`. Только они могут стать лидером без потери подтверждённых данных.
- **High watermark** — offset, до которого запись реплицирована во все ISR; consumer видит только записи до него.
- **Unclean leader election** (`unclean.leader.election.enable=false` по умолчанию): если все ISR недоступны, лучше недоступность, чем выбор отстающей реплики и потеря данных.

### 3.3. KRaft и Kafka 4.x

`[ ]` **Короткий ответ:** с Kafka 4.0 (март 2025) ZooKeeper полностью удалён, метаданные кластера хранят контроллеры на протоколе Raft (KRaft). В том же релизе стал GA новый протокол перебалансировки consumer groups (KIP-848) и появился ранний доступ к очередям — share groups (KIP-932) ([анонс Kafka 4.0](https://kafka.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/)). В 4.1 share groups вышли в preview ([заметки об обновлении 4.1](https://kafka.apache.org/41/getting-started/upgrade/)), в 4.2 (февраль 2026) объявлены готовыми к production ([анонс Kafka 4.2](https://kafka.apache.org/blog/2026/02/17/apache-kafka-4.2.0-release-announcement/)).

<details>
<summary>Что это значит на практике</summary>

- Кластер на ZooKeeper нельзя обновить сразу до 4.0: сначала миграция на KRaft в линейке 3.x (мостовой релиз — 3.9) ([руководство по обновлению 4.0](https://kafka.apache.org/40/getting-started/upgrade/)).
- Опция `--zookeeper` исчезла из CLI; все инструменты работают через `--bootstrap-server`.
- Режим combined (брокер и контроллер в одном процессе) подходит для разработки; в production контроллеры обычно выделяют.

</details>

**Уточняющие вопросы:**

- Зачем был нужен ZooKeeper? Хранение метаданных, выбор контроллера. Минусы: вторая распределённая система в эксплуатации и узкое место при большом числе партиций.
- Сколько партиций делать? Исходя из целевого параллелизма consumers и throughput на партицию, с запасом. Уменьшить число партиций нельзя, увеличение меняет распределение ключей.

---

## 4. Producer: ключи, acks, идемпотентность

### 4.1. Ключ и партиционирование

`[ ]` **Короткий ответ:** если у записи есть ключ, партиция выбирается по хешу ключа (murmur2) по модулю числа партиций — все записи с одним ключом попадают в одну партицию и сохраняют порядок. Без ключа producer использует sticky-партиционирование: заполняет батч для одной партиции, затем переключается — это даёт крупные батчи. Увеличение числа партиций меняет отображение ключ → партиция для новых записей.

**Практика.** Шесть событий трёх заказов в топик с тремя партициями (реальный вывод Kafka 4.4.0):

```bash
printf 'order-1:created\norder-2:created\norder-1:paid\norder-3:created\norder-1:shipped\norder-2:paid\n' \
  | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic keys \
      --property parse.key=true --property key.separator=:
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic keys --from-beginning \
  --group lab-g --property print.key=true --property print.partition=true --property print.offset=true
```

```text
Partition:0	Offset:0	order-2	created
Partition:0	Offset:1	order-3	created
Partition:0	Offset:2	order-2	paid
Partition:1	Offset:0	order-1	created
Partition:1	Offset:1	order-1	paid
Partition:1	Offset:2	order-1	shipped
```

Все события `order-1` в партиции 1 в порядке отправки. `order-2` и `order-3` попали в одну партицию — это нормально, хеш не обязан раскладывать ключи равномерно на малом числе ключей. Партиция 2 пуста. Глобального порядка нет: consumer мог прочитать `order-1 created` раньше `order-2 created`, хотя отправлен он позже.

### 4.2. acks, ISR и min.insync.replicas

`[ ]` **Короткий ответ:** `acks=0` позволяет producer продолжать без ответа брокера, `acks=1` ждёт append на лидере, `acks=all` (`-1`) ждёт репликации на весь текущий ISR. `min.insync.replicas` задаёт минимально допустимый размер ISR для `acks=all`. При RF=3 и пороге 2 лидер ждёт все три реплики, если все в ISR. Если осталось две, запись всё ещё может пройти. Если осталась одна, broker вернёт ошибку `NotEnoughReplicasException` или `NotEnoughReplicasAfterAppendException` в зависимости от момента падения ниже порога. [Настройки брокера](https://kafka.apache.org/39/configuration/broker-configs/#min.insync.replicas)

| Настройка | Где | Типичное значение для важных данных |
|---|---|---|
| `replication.factor` | Топик | 3 |
| `min.insync.replicas` | Топик или брокер | 2 |
| `acks` | Producer | `all` (по умолчанию с 3.0) |
| `unclean.leader.election.enable` | Топик или брокер | `false` (по умолчанию) |

Если все три реплики были в ISR и две остаются доступны после отказа, запись может продолжиться после обновления ISR и, при падении лидера, его перевыборов. [Практикум с остановкой реплик](../../labs/kafka/README.md)

### 4.3. Идемпотентный producer и значения по умолчанию

`[ ]` **Короткий ответ:** при сетевой ошибке producer повторяет отправку, и без защиты запись может задублироваться в журнале. Идемпотентный producer получает producer id и нумерует батчи в каждой партиции; брокер отбрасывает повтор с уже виденным номером. Это защищает от дублей из-за ретраев одного экземпляра producer в одной сессии, но не от повторной отправки бизнес-события после перезапуска приложения и не от дублей на стороне consumer. С Kafka 3.0 по умолчанию `enable.idempotence=true` и `acks=all` ([Javadoc KafkaProducer](https://kafka.apache.org/40/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html)).

**Практика.** Значения по умолчанию, прочитанные из `ProducerConfig` клиента 4.4.0 (код — в [приложении](#приложение-код-лабораторных)):

```text
default acks = -1
default enable.idempotence = true
default linger.ms = 5
default batch.size = 16384
default max.in.flight.requests.per.connection = 5
default retries = 2147483647
default delivery.timeout.ms = 120000
default compression.type = none
```

<details>
<summary>Почему важны retries, delivery.timeout.ms и max.in.flight</summary>

- `retries` фактически бесконечны; реальную границу задаёт `delivery.timeout.ms` (2 минуты): после него `send` завершится ошибкой в callback. Ошибку нужно обработать, иначе потеря будет тихой.
- `max.in.flight.requests.per.connection` до 5 при идемпотентности сохраняет порядок даже при ретраях. Без идемпотентности и с `max.in.flight > 1` ретрай может переставить батчи местами.
- `linger.ms=5` — значение по умолчанию в линейке 4.x (в 3.x было 0): небольшая задержка ради более крупных батчей.
- `send()` асинхронный: возвращает `Future`. Вызов `.get()` на каждое сообщение превращает producer в синхронный и убивает throughput; используйте callback.
- Если явно задать несовместимые настройки (например, `acks=1` вместе с `enable.idempotence=true`), клиент выдаст ошибку конфигурации.

</details>

---

## 5. Consumer groups и offset

### 5.1. Как работает группа

`[ ]` **Короткий ответ:** consumers с одним `group.id` делят партиции топика: каждая партиция в каждый момент назначена ровно одному consumer группы. Поэтому consumers больше, чем партиций, — лишние простаивают. Разные группы читают топик независимо, каждая со своими offset — так реализуется pub/sub. Прогресс группы хранится в служебном топике `__consumer_offsets`.

```text
topic orders: P0 P1 P2
group billing:   C1 ← P0, P1   C2 ← P2
group analytics: C1 ← P0, P1, P2      (читает те же данные независимо)
```

### 5.2. Rebalance и KIP-848

`[ ]` **Короткий ответ:** rebalance — перераспределение партиций при входе, выходе или «зависании» consumer. В классическом протоколе он координируется клиентами, и при eager-стратегии все consumers группы останавливаются и отдают партиции. Новый протокол KIP-848 (GA в Kafka 4.0) переносит расчёт назначения на брокер-координатор и делает его инкрементальным: двигаются только затронутые партиции, остальные consumers продолжают работу ([анонс Kafka 4.0](https://kafka.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/)). На клиенте он включается `group.protocol=consumer`; по умолчанию в клиенте 4.4.0 по-прежнему `classic` (проверено в лаборатории ниже). Spring for Apache Kafka 4.0 поддерживает новый протокол ([что нового в Spring Kafka 4.0](https://docs.spring.io/spring-kafka/reference/4.0/whats-new.html)).

<details>
<summary>Почему consumer выкидывает из группы</summary>

- `session.timeout.ms` (45 с) — нет heartbeat: процесс упал или сеть недоступна.
- `max.poll.interval.ms` (5 минут) — между вызовами `poll()` прошло слишком много времени: обработка пачки слишком долгая. Consumer считается зависшим, его партиции отдают другим, а его последующий commit отклоняется. Классическая причина бесконечного цикла «обработал — не смог закоммитить — получил те же сообщения снова».
- Лечение: уменьшить `max.poll.records`, ускорить обработку, вынести долгие задачи из poll-потока с осторожным управлением offset, или использовать share groups.
- Static membership (`group.instance.id`) — перезапуск пода с тем же id в пределах session timeout не вызывает rebalance.

</details>

**Практика.** Значения по умолчанию consumer 4.4.0 (с заданным `group.id`):

```text
group.protocol = classic
enable.auto.commit = true
auto.commit.interval.ms = 5000
auto.offset.reset = latest
isolation.level = read_uncommitted
max.poll.records = 500
max.poll.interval.ms = 300000
session.timeout.ms = 45000
fetch.min.bytes = 1
fetch.max.wait.ms = 500
```

Без `group.id` клиент выставляет `enable.auto.commit=false`: коммитить некуда.

### 5.3. Commit offset и lag

`[ ]` **Короткий ответ:** закоммиченный offset — номер следующей записи, которую группа прочитает после перезапуска. Commit до обработки даёт at-most-once (упали — сообщение пропущено), commit после обработки — at-least-once (упали — сообщение обработано повторно). Автокоммит каждые 5 секунд коммитит то, что вернул предыдущий `poll()`, — при асинхронной обработке он может закоммитить ещё не обработанное. Lag — разница между концом журнала и закоммиченным offset; это главная метрика здоровья consumer.

**Практика.** Группа `lab-g` прочитала 6 сообщений, затем в партицию 1 дописали ещё три (реальный вывод):

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group lab-g
```

```text
TOPIC      PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
keys-12510 0          3               3               0
keys-12510 1          3               6               3
keys-12510 2          0               0               0
```

<details>
<summary>Как перечитать или пропустить сообщения</summary>

```bash
# группа должна быть остановлена
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group lab-g --topic orders \
  --reset-offsets --to-earliest --dry-run     # сначала посмотреть
kafka-consumer-groups.sh ... --reset-offsets --to-datetime 2026-10-26T10:00:00.000 --execute
kafka-consumer-groups.sh ... --reset-offsets --shift-by -100 --execute
```

- `auto.offset.reset` срабатывает только когда закоммиченного offset нет (новая группа) или он вышел за пределы retention. `latest` — новая группа пропустит всё существующее; `earliest` — прочитает с начала.
- Растущий lag: consumer медленный, мало партиций для параллелизма, частые rebalance, «ядовитое» сообщение блокирует партицию.

</details>

---

## 6. Порядок сообщений

`[ ]` **Короткий ответ:** Kafka гарантирует порядок только внутри партиции. Для порядка событий одного заказа публикуем их с ключом `orderId` — они попадут в одну партицию — и обрабатываем партицию последовательно. Глобальный порядок по всем заказам требует одной партиции и убивает параллелизм; он почти никогда не нужен.

Что ломает порядок:

| Причина | Что происходит | Защита |
|---|---|---|
| Нет ключа или ключ не тот | События заказа в разных партициях | Ключ = идентификатор агрегата |
| Увеличили число партиций | Новые события ключа в другой партиции, старые ещё не обработаны | Планировать партиции заранее; при изменении — дренировать или мигрировать |
| Ретраи producer без идемпотентности при `max.in.flight > 1` | Батчи переставлены | Идемпотентный producer (по умолчанию) |
| Параллельная обработка внутри consumer | Потоки обрабатывают события ключа одновременно | Параллелить по ключу, а не по записи |
| Retry topic | Неудачное событие обработают позже следующих | Осознанный компромисс; либо блокирующий retry |
| Несколько отправителей одного агрегата | Порядок между ними не определён | Один владелец агрегата, версия в событии |

<details>
<summary>Если порядок всё-таки может нарушиться</summary>

Делайте consumer устойчивым к перестановке: храните версию агрегата (`version` в событии) и игнорируйте события с версией не больше уже применённой; либо обрабатывайте только последнее состояние (state transfer вместо delta). Это дешевле, чем гарантировать порядок во всей цепочке.

</details>

---

## 7. Гарантии доставки и транзакции

### 7.1. Три гарантии

`[ ]` **Короткий ответ:**

- **At-most-once** — не больше одного раза, возможна потеря. Пример: offset закоммичен до обработки.
- **At-least-once** — не меньше одного раза, возможны дубликаты. Commit после обработки, ретраи. Стандартный выбор плюс идемпотентный consumer.
- **Exactly-once** — эффект ровно один раз, но только в пределах конкретной системы. Транзакции Kafka атомарно связывают запись выходных сообщений и commit входного offset внутри Kafka. Внешний побочный эффект — платёж, SQL, письмо — exactly-once не становится: нужна идемпотентность или транзакционная интеграция с этой системой.

Гарантия складывается из всей цепочки: `acks` и репликация на producer, момент commit и обработка ошибок на consumer. Ретраи сами по себе дают дубликаты, а не надёжность ([гарантии доставки, Confluent](https://developer.confluent.io/courses/architecture/guarantees/)).

### 7.2. Транзакции Kafka

`[ ]` **Короткий ответ:** транзакционный producer (`transactional.id`) атомарно пишет в несколько партиций и добавляет в ту же транзакцию offset входного топика (`sendOffsetsToTransaction`). Consumer с `isolation.level=read_committed` видит только зафиксированные транзакции; по умолчанию `read_uncommitted` видит и отменённые. Это основа exactly-once для схемы consume → transform → produce, на которой построен Kafka Streams (`processing.guarantee=exactly_once_v2`).

**Практика.** Одна транзакция зафиксирована (A, B), вторая отменена (C). Читаем двумя уровнями изоляции (реальный вывод):

```java
producer.initTransactions();
producer.beginTransaction();
producer.send(new ProducerRecord<>(topic, "order-1", "committed-A"));
producer.send(new ProducerRecord<>(topic, "order-1", "committed-B"));
producer.commitTransaction();
producer.beginTransaction();
producer.send(new ProducerRecord<>(topic, "order-1", "aborted-C"));
producer.flush();
producer.abortTransaction();
```

```text
read_uncommitted: [committed-A@0, committed-B@1, aborted-C@3]
read_committed: [committed-A@0, committed-B@1]
```

Отменённая запись физически лежит в журнале, и `read_uncommitted` её видит. Offset 2 пропущен: его занимает служебный маркер commit — поэтому offset в транзакционном топике не идут подряд и их нельзя использовать как счётчик сообщений.

<details>
<summary>Ограничения транзакций</summary>

- `transactional.id` должен быть стабильным для экземпляра: при перезапуске новый producer с тем же id «огораживает» (fencing) старого зомби — тот получит `ProducerFencedException`.
- Транзакции добавляют задержку: `read_committed` consumer не видит записи, пока транзакция не завершена; долгая открытая транзакция задерживает чтение всей партиции после неё (last stable offset).
- Запись в БД внутри «транзакции Kafka» не участвует в ней. Если нужно атомарно изменить БД и отправить событие — outbox.

</details>

---

### Разбор гарантий доставки Kafka

`[ ]` **Короткий ответ.** Если зафиксировать offset до обработки, сбой между commit и бизнес-операцией пропустит её. Если фиксировать после обработки, сбой в обратном окне приведёт к повтору. Для внешней БД обработчик может пережить повтор через inbox и изменение данных в одной транзакции БД.

<details>
<summary>Падение до и после commit offset</summary>

В [OffsetCommitLab](../../../src/main/java/ru/learn/kafka/delivery/OffsetCommitLab.java) `poll()` продвигает локальную позицию сразу на всю полученную пачку. Код явно передаёт `record.offset() + 1` для конкретной партиции. На собеседовании нарисуй пачку с offsets 10, 11 и 12. Если обработаны 10 и 11, зафиксируй 12. Если готова только 11, подтверждать 12 ещё рано. Нужен непрерывный обработанный префикс по каждой партиции.

| Сбой в тесте | Что видно после перезапуска с тем же group.id |
|---|---|
| Commit offset 1, затем падение до обработки записи 0 | Группа читает следующую запись. Начисление для записи 0 пропущено. |
| Начисление 100, затем падение до commit offset | Группа снова читает запись 0. Без inbox баланс становится 200. |
| Commit БД с inbox, затем падение до commit offset | Запись 0 приходит повторно. Отдельный `send` того же события даёт запись 1. [WalletLedger](../../../src/main/java/ru/learn/delivery/WalletLedger.java) пропускает обе попытки по event ID. Баланс остаётся 100, группа фиксирует offset 2. |

Тесты назначают партицию явно и выключают auto-commit, чтобы изолировать окно сбоя. При ребалансе следующий владелец тоже опирается на зафиксированный offset группы. [KafkaConsumer API](https://kafka.apache.org/39/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)

</details>

**Уточняющий вопрос.** Почему при `enable.idempotence=true` в журнале могут быть две записи с одним event ID? Producer дедуплицирует протокольные ретраи отправки. Два вызова `send` создают две записи. Для порядка обычно выбирают ключ агрегата, а для дедупликации хранят стабильный ID события.

**Практика.** В [KafkaDeliveryIntegrationTest](../../../src/test/java/ru/learn/kafka/delivery/KafkaDeliveryIntegrationTest.java) остановись перед исключением в каждом опыте. Предскажи следующий offset и баланс, затем сравни с assertions. [Команда запуска](../../../src/main/java/ru/learn/kafka/README.md#2-гарантии-доставки)

### Разбор транзакций Kafka

`[ ]` **Короткий ответ.** Транзакция Kafka позволяет зафиксировать записи в нескольких партициях и, при Kafka → Kafka, сохранить вместе с ними offsets входной группы. При abort потребитель с `read_committed` пропускает отменённый результат. Вход можно обработать повторно, потому что его offset ещё не зафиксирован.

<details>
<summary>Что фиксируется в Java API и Spring Kafka</summary>

1. [AtomicOrderPublisher](../../../src/main/java/ru/learn/kafka/transactions/AtomicOrderPublisher.java) через `KafkaTemplate.executeInTransaction` отправляет заказ и аудит в два топика. [Тест публикации](../../../src/test/java/ru/learn/kafka/transactions/AtomicOrderPublisherIntegrationTest.java) проверяет видимость обеих записей после commit и отмену при исключении. У читателей по-прежнему независимые вызовы `poll()`.
2. [KafkaExactlyOnceIntegrationTest](../../../src/test/java/ru/learn/kafka/KafkaExactlyOnceIntegrationTest.java) показывает низкоуровневый путь. Первая попытка успевает отправить выход и делает `abortTransaction()`. Её выход виден в `read_uncommitted`. Тест возвращает входной consumer к исходной записи, а во второй транзакции передаёт `offset + 1` и метаданные группы в `sendOffsetsToTransaction`. После `commitTransaction()` видно один зафиксированный результат и committed offset 1.
3. [SpringKafkaTransactionIntegrationTest](../../../src/test/java/ru/learn/kafka/transactions/SpringKafkaTransactionIntegrationTest.java) бросает исключение после `send().join()`. Контейнер откатывает транзакцию и возвращается к входной записи. Тест проверяет два вызова обработчика, только `attempt-2:created` в `read_committed` и committed offset 1.
4. В опыте с Last Stable Offset открытая транзакция в начале партиции задерживает чтение даже более поздней нетранзакционной записи из той же партиции. После abort читатель с `read_committed` пропускает первую запись и получает `later`. [KafkaConsumer API](https://kafka.apache.org/39/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)

</details>

**Уточняющий вопрос.** Что будет, если после записи в SQL-БД Kafka-транзакция отменится? SQL commit уже состоялся, а входная запись может прийти снова. Для БД используй границу [inbox](delivery-patterns.md#8-идемпотентный-consumer-и-inbox). Для направления БД → брокер разбери [outbox](delivery-patterns.md#9-outbox-и-cdc).

**Практика.** В тесте с Java API проследи за `send().get()`, abort и финальным commit. Объясни, почему успешная отправка и видимость для `read_committed` наступают в разные моменты. Затем представь успешный commit выхода без `sendOffsetsToTransaction` и предскажи, что прочитает группа после перезапуска. [Команда запуска](../../../src/main/java/ru/learn/kafka/README.md#3-транзакции)

## 10. Обработка ошибок: retry, DLQ, fail-fast

`[ ]` **Короткий ответ:** сначала классифицируем ошибку. Временная (таймаут, недоступность зависимости) — ограниченный retry с экспоненциальной задержкой. Постоянная (не парсится, нарушает бизнес-правило) — повторять бессмысленно: сразу в DLQ с причиной и метаданными. Если пропуск события недопустим и порядок важнее доступности — fail-fast: останавливаем обработку партиции и зовём человека. Главная опасность в Kafka — poison message: одно сломанное сообщение, на котором consumer бесконечно падает, блокируя всю партицию.

| Подход | Плюсы | Минусы | Когда |
|---|---|---|---|
| Блокирующий retry в consumer | Сохраняет порядок | Блокирует партицию, растёт lag, риск выпасть по `max.poll.interval.ms` | Короткие временные сбои, порядок важен |
| Retry topics (`orders-retry-1m`, `-10m`) | Не блокирует основной поток | Нарушает порядок, больше топиков | Порядок не важен, сбои долгие |
| DLQ / DLT | Изолирует poison message, можно разобрать и переиграть | Требует алертов, доступа, процесса replay | Постоянные ошибки, исчерпанные попытки |
| Fail-fast | Ошибка не проходит незамеченной | Остановка, lag | Финансовые события, где пропуск хуже простоя |

<details>
<summary>Что должно быть в DLQ и что делать с ним</summary>

- Исходный payload без изменений, ключ, заголовки, исходный топик, партиция, offset, время, класс и текст ошибки, число попыток.
- Алерт на появление сообщений в DLQ — DLQ без алерта превращается в кладбище.
- Retention DLQ дольше основного топика; доступ ограничен — там могут быть персональные данные. Секреты и PII в логах маскировать.
- Replay: после исправления бага публикуем обратно в основной топик. Это нарушает исходный порядок и создаёт возможность дубля — consumer должен быть идемпотентным.
- Лог одной ошибки без исходного события — не замена DLQ: повторить обработку по нему нельзя.
- В Spring Kafka: `DefaultErrorHandler` с `BackOff` для повторов и `DeadLetterPublishingRecoverer` для отправки в `<topic>-dlt`; неретраибельные исключения (например, ошибки десериализации) настраиваются отдельно.

</details>

**Транзакционная обработка потока.** Kafka Streams и аналогичные фреймворки атомарно записывают результат и offset/checkpoint в пределах своей транзакционной границы. Это снижает дубли внутри Kafka, но не делает exactly-once внешний вызов, и не отменяет необходимость DLQ для постоянно ошибочных событий.

---

## 11. Share groups: очереди в Kafka

`[ ]` **Короткий ответ:** share group (KIP-932, «Queues for Kafka») — новый вид группы, где несколько consumers совместно читают одну и ту же партицию, без закрепления партиции за одним consumer. Каждая запись подтверждается отдельно (accept, release, reject), брокер считает число попыток доставки. Это даёт семантику очереди: параллелизм не ограничен числом партиций, долгая задача не блокирует остальные. Цена — нет гарантии порядка. Подходит для независимых задач, обрабатываемых по одной ([заметки об обновлении 4.1](https://kafka.apache.org/41/getting-started/upgrade/)). Production-ready с Kafka 4.2 ([анонс Kafka 4.2](https://kafka.apache.org/blog/2026/02/17/apache-kafka-4.2.0-release-announcement/)).

**Практика.** Топик из одной партиции, 12 задач, два `KafkaShareConsumer` в одной share group, явные подтверждения. Задачу `job-5` первый consumer при первой доставке отпускает (`RELEASE`), остальные принимает (реальный вывод, `#N` — номер попытки доставки):

```text
C1: [job-1#1, job-2#1, job-3#1, job-4#1, job-5#1, job-6#1, job-7#1, job-8#1, job-9#1, job-10#1, job-11#1, job-12#1]
C2: [job-5#2]
accepted: 12
```

При повторном запуске имена C1 и C2 могут поменяться местами, суть та же. Отпущенная задача ушла второму consumer из той же партиции — в consumer group это невозможно. Остальные задачи достались C1 потому, что они уже были в топике и брокер отдал их пачками первому подключившемуся; распределение между consumers не обязано быть равномерным.

<details>
<summary>Как устроено и что спросят</summary>

- Записи берутся consumer во временное владение (acquisition lock с таймаутом). Не подтвердил вовремя — запись снова доступна другим.
- `ACCEPT` — обработано; `RELEASE` — вернуть для повторной доставки; `REJECT` — запись непригодна, больше не выдавать; `RENEW` в 4.2 — продлить блокировку для долгой обработки.
- После лимита попыток доставки запись помечается как архивная и больше не выдаётся — защита от poison message.
- `share.auto.offset.reset` по умолчанию `latest`: новая share group не видит уже записанное. В лаборатории для группы явно выставлено `earliest`.
- Включение на кластере: фича `share.version` через `kafka-features.sh`; в 4.4.0 она уже финализирована на уровне 2.
- Когда всё-таки consumer group: нужен порядок по ключу, stateful-обработка, Kafka Streams.

</details>

---

## 13. Пропускная способность Kafka

`[ ]` **Короткий ответ:** throughput растёт от батчинга и сжатия, параллелизм — от числа партиций и consumers. Каждая настройка обменивает пропускную способность на задержку, память или надёжность. Менять по одной и измерять под нагрузкой; снижение надёжности (`acks`, `min.insync.replicas`) ради скорости — осознанное решение, а не оптимизация.

### Producer

| Настройка | Что даёт | Цена |
|---|---|---|
| `linger.ms` (по умолчанию 5) | Крупнее батчи | Задержка |
| `batch.size` (16 КБ) | Меньше запросов | Память, задержка |
| `compression.type` (`lz4`, `zstd`, `snappy`) | Меньше сеть и диск; сжимается батч целиком | CPU |
| `buffer.memory` | Переживает всплески | Память; при заполнении `send` блокируется до `max.block.ms` |
| `acks` | `all` — надёжно; `1`/`0` — быстрее | Риск потери |
| Асинхронный `send` с callback | Не ждёт каждое сообщение | Обработка ошибок в callback |

### Consumer

| Настройка | Что даёт | Цена |
|---|---|---|
| `fetch.min.bytes`, `fetch.max.wait.ms` | Брокер копит более крупный ответ | Задержка |
| `max.partition.fetch.bytes` | Больше данных за запрос | Память |
| `max.poll.records` | Больше записей за `poll` | Дольше обработка пачки, риск `max.poll.interval.ms` |
| Больше партиций и consumers | Параллелизм | Метаданные, rebalance, перераспределение ключей |
| Параллельная обработка внутри consumer | Утилизация CPU | Порядок и управление offset |
| Пакетная запись в БД | Меньше round-trip | Сложнее частичные сбои |
| Реже commit | Меньше накладных расходов | Больше повторов после сбоя |

### Кластер, инфраструктура, процесс

- Число партиций — под целевой параллелизм с запасом, но не чрезмерно: каждая партиция — файлы, метаданные, работа контроллера.
- Диски брокеров (SSD), сеть, CPU клиентов (сжатие и сериализация), `replica.fetch.max.bytes` для репликации.
- Метрики: request latency, records/s, средний размер батча, `record-queue-time`, consumer lag, CPU, сеть, GC.
- Нагрузочный тест, одна настройка за раз, задокументированный план отката.

---

## 15. Источники

- [Apache Kafka 4.0.0 Release Announcement](https://kafka.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/) — KRaft-only, KIP-848 GA, ранний доступ к share groups.
- [Apache Kafka 4.2.0 Release Announcement](https://kafka.apache.org/blog/2026/02/17/apache-kafka-4.2.0-release-announcement/) — share groups готовы к production.
- [Kafka 4.0: обновление](https://kafka.apache.org/40/getting-started/upgrade/) и [Kafka 4.1: обновление](https://kafka.apache.org/41/getting-started/upgrade/).
- [KafkaProducer Javadoc (4.0)](https://kafka.apache.org/40/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html) — идемпотентность и транзакции.
- [Confluent: гарантии доставки](https://developer.confluent.io/courses/architecture/guarantees/).
- [Spring for Apache Kafka 4.0: что нового](https://docs.spring.io/spring-kafka/reference/4.0/whats-new.html).
- [Дистрибутив Kafka 4.4.0](https://downloads.apache.org/kafka/), на котором выполнены лабораторные.

## Приложение: код лабораторных

<details>
<summary>KafkaLab.java — значения по умолчанию producer и транзакции</summary>

Запуск: `java -cp "kafka_2.13-4.4.0/libs/*" KafkaLab.java` при работающем брокере на `localhost:9092`.

```java
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;
import java.time.Duration;
import java.util.*;

public class KafkaLab {
    static final String BS = "localhost:9092";

    public static void main(String[] args) throws Exception {
        String topic = "tx-demo-" + System.currentTimeMillis();
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", BS))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        }

        Properties p = new Properties();
        p.put("bootstrap.servers", BS);
        p.put("key.serializer", StringSerializer.class.getName());
        p.put("value.serializer", StringSerializer.class.getName());
        ProducerConfig cfg = new ProducerConfig(p);
        for (String k : List.of("acks", "enable.idempotence", "linger.ms", "batch.size",
                "max.in.flight.requests.per.connection", "retries", "delivery.timeout.ms", "compression.type"))
            System.out.println("default " + k + " = " + cfg.values().get(k));

        p.put("transactional.id", "lab-tx");
        try (KafkaProducer<String, String> prod = new KafkaProducer<>(p)) {
            prod.initTransactions();
            prod.beginTransaction();
            prod.send(new ProducerRecord<>(topic, "order-1", "committed-A"));
            prod.send(new ProducerRecord<>(topic, "order-1", "committed-B"));
            prod.commitTransaction();
            prod.beginTransaction();
            prod.send(new ProducerRecord<>(topic, "order-1", "aborted-C"));
            prod.flush();
            prod.abortTransaction();
        }
        for (String iso : List.of("read_uncommitted", "read_committed")) {
            Properties c = new Properties();
            c.put("bootstrap.servers", BS);
            c.put("group.id", "g-" + iso + System.nanoTime());
            c.put("auto.offset.reset", "earliest");
            c.put("isolation.level", iso);
            c.put("key.deserializer", StringDeserializer.class.getName());
            c.put("value.deserializer", StringDeserializer.class.getName());
            try (KafkaConsumer<String, String> cons = new KafkaConsumer<>(c)) {
                cons.subscribe(List.of(topic));
                List<String> got = new ArrayList<>();
                long end = System.currentTimeMillis() + 8000;
                while (System.currentTimeMillis() < end && got.size() < 3)
                    for (var r : cons.poll(Duration.ofMillis(500))) got.add(r.value() + "@" + r.offset());
                System.out.println(iso + ": " + got);
            }
        }
    }
}
```

</details>

<details>
<summary>ShareLab.java — share group с RELEASE и повторной доставкой</summary>

```java
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class ShareLab {
    static final String BS = "localhost:9092";

    public static void main(String[] args) throws Exception {
        String topic = "share-" + System.currentTimeMillis(), group = "sg-" + System.nanoTime();
        try (Admin a = Admin.create(Map.of("bootstrap.servers", BS))) {
            a.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
            var res = new ConfigResource(ConfigResource.Type.GROUP, group);
            a.incrementalAlterConfigs(Map.of(res, List.of(new AlterConfigOp(
                    new ConfigEntry("share.auto.offset.reset", "earliest"), AlterConfigOp.OpType.SET)))).all().get();
        }
        Properties p = new Properties();
        p.put("bootstrap.servers", BS);
        p.put("key.serializer", StringSerializer.class.getName());
        p.put("value.serializer", StringSerializer.class.getName());
        try (var prod = new KafkaProducer<String, String>(p)) {
            for (int i = 1; i <= 12; i++) prod.send(new ProducerRecord<>(topic, "job-" + i));
        }
        Map<String, List<String>> seen = new ConcurrentHashMap<>();
        Set<String> done = ConcurrentHashMap.newKeySet();
        ExecutorService ex = Executors.newFixedThreadPool(2);
        for (String name : List.of("C1", "C2")) ex.submit(() -> {
            Properties c = new Properties();
            c.put("bootstrap.servers", BS);
            c.put("group.id", group);
            c.put("share.acknowledgement.mode", "explicit");
            c.put("max.poll.records", "2");
            c.put("key.deserializer", StringDeserializer.class.getName());
            c.put("value.deserializer", StringDeserializer.class.getName());
            try (var cons = new KafkaShareConsumer<String, String>(c)) {
                cons.subscribe(List.of(topic));
                long end = System.currentTimeMillis() + 20000;
                while (System.currentTimeMillis() < end && done.size() < 12) {
                    for (var r : cons.poll(Duration.ofMillis(500))) {
                        int attempt = r.deliveryCount().orElse((short) 0);
                        seen.computeIfAbsent(name, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(r.value() + "#" + attempt);
                        Thread.sleep(150);
                        if (r.value().equals("job-5") && attempt == 1) cons.acknowledge(r, AcknowledgeType.RELEASE);
                        else { cons.acknowledge(r, AcknowledgeType.ACCEPT); done.add(r.value()); }
                    }
                    cons.commitSync();
                }
            } catch (Exception e) { e.printStackTrace(); }
            return null;
        });
        ex.shutdown();
        ex.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("C1: " + seen.get("C1"));
        System.out.println("C2: " + seen.get("C2"));
        System.out.println("accepted: " + done.size());
    }
}
```

</details>
