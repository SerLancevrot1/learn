# Шпаргалки для собеседования по Java-бэкенду

Открой тему по ключевому слову. В начале файла — опорные тезисы, дальше — детали, вопросы и практика. **Для устного ответа назови механизм, приведи пример и обозначь ограничение.**

## Java и Spring

| Ищу | Открыть |
|---|---|
| Heap, stack, metaspace, GC, JIT, профилирование | [JVM](JVM.md) |
| HashMap, equals/hashCode, коллекции, PECS, Stream, Optional, record | [Java Core](Java.md) |
| Happens-before, volatile, CAS, synchronized, deadlock, ConcurrentHashMap | [Многопоточность](Concurrent.md) |
| Executor, очередь, CompletableFuture, Fork/Join, virtual threads, ThreadLocal | [Выполнение задач](AsyncJava.md) |
| Bean, DI, proxy, self-invocation, propagation, rollback, Spring Boot | [Spring](Spring.md) |
| SOLID и 23 паттерна GoF с примерами на Java | [Паттерны проектирования](DesignPatterns.md) |
| Два указателя, окно, бинарный поиск, дерево, граф, DP | [Алгоритмы](Algorithms.md) |

## Данные и эксплуатация

| Ищу | Открыть |
|---|---|
| JOIN, GROUP BY, NULL, оконные функции, ключи, нормализация | [SQL и схема](DataBases.md) |
| B-tree, составной индекс, EXPLAIN, N+1, выбор хранилища | [Индексы и запросы](DatabasePerformance.md) |
| ACID, MVCC, уровни изоляции, блокировки, write skew | [Транзакции в БД](DatabaseTransactions.md) |
| Партиции, offset, ack, exactly-once, RabbitMQ, DLQ | [Брокеры сообщений](MessageBrockers.md) |
| Падение до и после commit offset, повтор и inbox | [Kafka · гарантии доставки](MessageBrockers.md#разбор-гарантий-доставки-kafka) |
| Abort, read_committed, Last Stable Offset, Spring Kafka | [Kafka · транзакции](MessageBrockers.md#разбор-транзакций-kafka) |
| Publisher confirm, mandatory return, prefetch, retry и DLQ | [RabbitMQ · разбор доставки](MessageBrockers.md#разбор-доставки-rabbitmq) |
| TTL, cache-aside, stampede, Redis Sentinel и Cluster | [Кеширование и Redis](RedisAndCaching.md) · [Разбор лабораторной](RedisAndCaching.md#разбор-лабораторной-redis) |
| Запускаемые лабораторные Redis, Kafka, RabbitMQ, MongoDB и Data Lake | [Практикум](../../../../../../LABS.md) |
| BSON, составной индекс, транзакции, репликация, шардирование и Change Streams | [MongoDB](MongoDB.md) · [Практикум](../mongo/README.md) |
| Bronze/Silver/Gold, Parquet, поздние данные, снимки и Iceberg | [Data Lake и Lakehouse](DataLake.md) · [Практикум](../datalake/README.md) |
| TLS, mTLS, сертификаты, handshake | [Безопасность](Security.md) |
| Docker, Kubernetes, пробы, rollout, HPA | [DevOps](Devops.md) |
| Prometheus, PromQL, кардинальность, логи, трассировки | [Observability](Observability.md) |

## Архитектура и system design

| Ищу | Открыть |
|---|---|
| План интервью, API, QPS, оценка объёма | [Ход интервью](system/design/Interview.md) |
| SLI/SLO/SLA, p99, error budget, RPO/RTO | [Нефункциональные требования](system/design/proselyte/1_NonFunctionalRequirements.md) |
| Deadline, retry, circuit breaker, bulkhead, backpressure | [Паттерны устойчивости](system/design/Reliability.md) |
| Load balancer, L4/L7, Gateway, BFF, CDN, закон Литтла | [Масштабирование систем и управление API-трафиком](system/design/proselyte/2_SystemScaling.md) |
| CAP, PACELC, согласованность, репликация, шардирование | [Распределённые данные](system/design/DistributedData.md) |
| Микросервисы, границы, Saga, Outbox, Inbox, CQRS, миграции | [Архитектура и интеграция](Architecture.md) |

## Кейсы system design

- [Rate limiter](system/design/example/RateLimiter.md)
- [Короткие ссылки](system/design/example/ShortUrl.md)
- [Чат](system/design/example/Chat.md)
- [Лента](system/design/example/NewsFeed.md)
- [Key-value](system/design/example/KeyValue.md)
- [Генератор ID](system/design/example/IdGenerator.md)
- [Веб-краулер](system/design/example/SearchRobot.md)
- [SMS и уведомления](system/design/example/SmsDeliveryService.md)

## Куда переехали разделы

| Исходный конспект | Новое место |
|---|---|
| Архитектура: ход интервью, оценки, пробное собеседование | [System design интервью](system/design/Interview.md) |
| Архитектура: масштабирование, балансировка, API Gateway и BFF | [Масштабирование и API-трафик](system/design/proselyte/2_SystemScaling.md) |
| Архитектура: репликация, шардирование, CAP и PACELC. Интервью: согласованное хеширование | [Распределённые данные](system/design/DistributedData.md) |
| Архитектура: устойчивость и SOLID | [Паттерны устойчивости](system/design/Reliability.md) · [SOLID и паттерны проектирования](DesignPatterns.md) |
| Java: память, GC, ссылки и профилирование | [JVM](JVM.md) |
| Многопоточность: executors, futures, Fork/Join, виртуальные потоки и ThreadLocal | [Выполнение задач](AsyncJava.md) |
| Базы данных: индексы, планы, ORM, выбор хранилища. Транзакции, изоляция и блокировки | [Индексы и запросы](DatabasePerformance.md) · [Транзакции](DatabaseTransactions.md) |

## Практика в репозитории

- **Java.** Разбери [атомарный инкремент с монитором](../cuncurrent/IncrementCounterAtomicDemo.java), пройди [цепочку CompletableFuture](../cuncurrent/CompletableFutureDemo.java) или реши [задачу на скользящее окно](../leet/code/easy/LongestSubstringWithoutRepeatingCharacters.java).
- **Kafka.** Начни с [инструкции по запуску](../kafka/README.md), затем сравни режимы подтверждения в [обработчиках заказа](../kafka/OrderListeners.java).
- **PostgreSQL.** Используй [локальный сервис postgres](../../../../../../compose.yaml) и SQL из конспектов. Для экспериментов с блокировками открой две сессии к одной БД.
- **Версии.** [Проект](../../../../../../pom.xml) компилируется под Java 17. Примеры новых возможностей Java в конспектах помечены отдельно.
- **Служебное.** [Рабочий процесс с ИИ](AI.md).
