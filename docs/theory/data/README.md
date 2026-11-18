# Базы данных, кеши и хранилища

[Все конспекты](../README.md)

Для работы с PostgreSQL начни с SQL и схемы. Подбор типа хранилища и кеширование можно читать отдельно.

| Тема | Где читать |
| --- | --- |
| JOIN, NULL, оконные функции, ключи, ограничения и нормализация | [SQL и схема](postgresql/sql-and-schema.md) |
| Зерно результата, интервалы из событий, LATERAL и рекурсивный CTE | [SQL для истории и отчётности](postgresql/query-patterns.md) |
| Составные индексы, B-tree, EXPLAIN и BUFFERS | [Индексы и планы](postgresql/performance.md) |
| Секции, pruning, BRIN, HOT и операции хранения истории | [Большие таблицы PostgreSQL](postgresql/large-tables.md) |
| ACID, MVCC, уровни изоляции, блокировки, write skew | [Транзакции PostgreSQL](postgresql/transactions.md) |
| JDBC-пул, lock waits, pg_stat_statements и диагностика планов | [Диагностика PostgreSQL](postgresql/diagnostics.md) |
| Expand/migrate/contract, backfill, DDL-блокировки и ограничения | [Миграции схемы](postgresql/schema-migrations.md) |
| WAL, LSN, репликация, failover, резервные копии и PITR | [Реплики и восстановление PostgreSQL](postgresql/recovery.md) |
| Кеш цен, TTL, счётчики, Sentinel, Cluster и Streams | [Redis для начинающих](redis-and-caching.md#гайд-для-начинающих) · [Конспект](redis-and-caching.md#содержание) |
| Холодный кеш, single flight, L1/L2, слоты и восстановление pending | [Кеши при сбое](cache-failures.md) |
| Документы, индексы, транзакции, репликация и Change Streams | [MongoDB для начинающих](mongodb.md#гайд-для-начинающих) · [Конспект](mongodb.md#на-интервью) |
| От событий до отчёта, Parquet, снимки, Iceberg и поздние данные | [Data Lake для начинающих](data-lake.md#гайд-для-начинающих) · [Конспект](data-lake.md#на-интервью) |
| SQL, NoSQL, OLTP и OLAP: что подходит под запросы и гарантии | [Выбор хранилища](storage-selection.md) |
| Версии производных документов, снимки Iceberg и пересчёт витрин | [От событий к MongoDB и Iceberg](derived-views.md) |

**Практика:** [две сессии PostgreSQL](../../labs/spring-postgres-outbox.md#postgresql-потерянное-обновление) · [репликация и восстановление PostgreSQL](../../labs/postgresql/README.md) · [Redis](../../../src/main/java/ru/learn/redis/README.md) · [MongoDB](../../../src/main/java/ru/learn/mongo/README.md) · [Data Lake](../../../src/main/java/ru/learn/datalake/README.md)

**Собеседование:** [SQL и PostgreSQL](../../interview/base/sql.md) · [MongoDB и Data Lake](../../interview/base/data.md) · [Redis, MongoDB и Data Lake](../../interview/advanced/storage.md)

Для [проблемы N+1 и планов загрузки](../spring/jpa.md) смотри раздел Spring. Для [CAP, репликации и шардирования](../architecture/distributed-data.md) — архитектуру.
