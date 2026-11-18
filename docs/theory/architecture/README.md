# Архитектура и system design

[Все конспекты](../README.md)

Для существующей системы начни с [требований и исходных замеров](design-process.md), затем определи [границы и владельцев данных](domain-boundaries.md) и запиши [контракты](communications.md). В [кейсе платежей](cases/payments.md) эти шаги связаны в одном примере.

| Тема | Где читать |
| --- | --- |
| Аудит кода, архитектурные драйверы, C4 и ADR | [Процесс системного дизайна](design-process.md) |
| Bounded context, карта контекстов, данные, Spring Modulith и ArchUnit | [Границы системы](domain-boundaries.md) |
| Команды, события, REST и gRPC, дедлайны, HTTP-кеш и совместимость API | [Коммуникации и контракты](communications.md) |
| Keyset, формат курсора, конкурентные изменения и согласованный снимок | [Пагинация API](pagination-contracts.md) |
| Монолит и микросервисы, Saga, CQRS, миграции | [Архитектура сервисов](service-architecture.md) |
| Ответственность за данные, shadow read, смена владельца и откат | [Выделение сервиса](service-extraction.md) |
| SOLID и 23 паттерна GoF с примерами Java | [Проектирование кода](design-patterns.md) |
| SLI, SLO, SLA, p99, error budget, RPO и RTO | [Нефункциональные требования](non-functional-requirements.md) |
| Балансировка L4/L7, API Gateway, BFF, CDN и закон Литтла | [Масштабирование и трафик](scaling.md) |
| CAP, PACELC, согласованность, репликация и шардирование | [Распределённые данные](distributed-data.md) |
| Журнал Raft, чтение с лидера, кворумы, lease и fencing | [Консенсус и владение](consensus-and-leases.md) |
| Deadline, retry, circuit breaker, bulkhead и backpressure | [Устойчивость](reliability.md) |
| Бюджеты ресурсов, рост очереди, отказ на входе и расчёт времени восстановления | [Перегрузка сервиса](overload-scenarios.md) |
| План интервью, оценки QPS и объёма данных | [Ход system design интервью](system-design-interview.md) |

**Собеседование:** [базовые вопросы](../../interview/base/design.md) · [продвинутые вопросы](../../interview/advanced/system-design.md) · [SOLID и паттерны](../../interview/base/patterns.md)

## Кейсы system design

- [Платёжная система: состояния, возвраты и идемпотентность](cases/payments.md) · [Конкурентные частичные возвраты](cases/payments.md#конкурентные-частичные-возвраты)
- [Бронирование ресурсов и гонка истечения резерва](cases/reservations.md)
- [Массовый импорт файлов и возобновление работы](cases/bulk-import.md)
- [Поиск по каталогу и смена поискового индекса](cases/catalog-search.md)
- [Rate limiter](cases/rate-limiter.md)
- [Короткие ссылки](cases/short-url.md)
- [Чат](cases/chat.md)
- [Лента](cases/news-feed.md)
- [Key-value-хранилище](cases/key-value.md)
- [Генератор ID](cases/id-generator.md)
- [Веб-краулер](cases/web-crawler.md)
- [SMS и уведомления](cases/notifications.md)
