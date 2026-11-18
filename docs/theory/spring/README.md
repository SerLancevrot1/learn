# Spring и JPA

[Все конспекты](../README.md)

В конспекте Spring разбирается работа контейнера и прокси. За изоляцией, MVCC и блокировками базы переходи к [транзакциям PostgreSQL](../data/postgresql/transactions.md).

| Тема | Где читать |
| --- | --- |
| Контейнер, DI, AOP, self-invocation, @Transactional, @Async и Spring Boot | [Spring Framework](spring-framework.md) |
| Вложенные вызовы, savepoints, события после commit, несколько ресурсов и повторы | [Сценарии транзакций Spring](transaction-cases.md) |
| Стартеры, условия автоконфигурации, properties, профили и диагностика | [Запуск Spring Boot](boot-configuration.md) |
| Состояния сущности, dirty checking, flush, связи, версии и N+1 | [JPA и Hibernate](jpa.md) |
| Connection, autocommit, параметры, батчи и потоковые выборки | [JDBC и PostgreSQL](jdbc-access.md) |
| Путь запроса, валидация, обработка ошибок, MockMvc и интеграционные тесты | [Spring MVC и тестирование](web-and-testing.md) |
| Конкурентные тесты, контрольные точки отказа и проверка бизнес-инвариантов | [Тестирование инвариантов](testing-invariants.md) |
| Event loop, backpressure, отмена, Context и реактивные транзакции | [Reactor и WebFlux](reactive-boundaries.md) |
| Scheduled, CronJob, пропущенные запуски и долговечная обработка | [Фоновые задачи и расписание](scheduled-work.md) |

**Практика:** [прокси и откат](../../labs/spring-postgres-outbox.md#spring-прокси-и-откат) · [JPA и план загрузки](../../interview/tech-lead.md#jpa-и-план-загрузки)

**Собеседование:** [Spring, JPA и тестирование](../../interview/base/spring.md) · [Продвинутые вопросы по Spring](../../interview/advanced/spring.md)
