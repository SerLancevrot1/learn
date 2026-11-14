# Базы данных, кеши и хранилища

[Все конспекты](../README.md)

Для работы с PostgreSQL начни с SQL и схемы. Подбор типа хранилища и кеширование можно читать отдельно.

| Тема | Где читать |
| --- | --- |
| JOIN, NULL, оконные функции, ключи, ограничения и нормализация | [SQL и схема](postgresql/sql-and-schema.md) |
| Составные индексы, B-tree, EXPLAIN и BUFFERS | [Индексы и планы](postgresql/performance.md) |
| ACID, MVCC, уровни изоляции, блокировки, write skew | [Транзакции PostgreSQL](postgresql/transactions.md) |
| TTL, cache-aside, stampede, Redis Sentinel, Cluster и Streams | [Кеширование и Redis](redis-and-caching.md) |
| BSON, индексы, репликация, шарды и Change Streams | [MongoDB](mongodb.md) |
| Bronze/Silver/Gold, Parquet, снимки, Iceberg, поздние данные | [Data Lake и Lakehouse](data-lake.md) |
| SQL, NoSQL, OLTP и OLAP: что подходит под запросы и гарантии | [Выбор хранилища](storage-selection.md) |

**Практика:** [две сессии PostgreSQL](../../labs/spring-postgres-outbox.md#postgresql-потерянное-обновление) · [репликация и восстановление PostgreSQL](../../labs/postgresql/README.md) · [Redis](../../../src/main/java/ru/learn/redis/README.md) · [MongoDB](../../../src/main/java/ru/learn/mongo/README.md) · [Data Lake](../../../src/main/java/ru/learn/datalake/README.md)

**Собеседование:** [SQL и PostgreSQL](../../interview/base/sql.md) · [MongoDB и Data Lake](../../interview/base/data.md) · [Redis, MongoDB и Data Lake](../../interview/advanced/storage.md)

Для [проблемы N+1 и планов загрузки](../spring/jpa.md) смотри раздел Spring. Для [CAP, репликации и шардирования](../architecture/distributed-data.md) — архитектуру.
