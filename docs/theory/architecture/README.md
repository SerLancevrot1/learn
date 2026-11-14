# Архитектура и system design

[Все конспекты](../README.md)

Для существующей системы начни с [требований и исходных замеров](design-process.md), затем определи [границы и владельцев данных](domain-boundaries.md) и запиши [контракты](communications.md). В [кейсе платежей](cases/payments.md) эти шаги связаны в одном примере.

| Тема | Где читать |
| --- | --- |
| Аудит кода, архитектурные драйверы, C4 и ADR | [Процесс системного дизайна](design-process.md) |
| Bounded context, карта контекстов, данные, Spring Modulith и ArchUnit | [Границы системы](domain-boundaries.md) |
| Команда, событие, запрос, REST, gRPC, очередь и журнал | [Коммуникации и контракты](communications.md) |
| Монолит и микросервисы, Saga, CQRS, миграции | [Архитектура сервисов](service-architecture.md) |
| SOLID и 23 паттерна GoF с примерами Java | [Проектирование кода](design-patterns.md) |
| SLI, SLO, SLA, p99, error budget, RPO и RTO | [Нефункциональные требования](non-functional-requirements.md) |
| Балансировка L4/L7, API Gateway, BFF, CDN и закон Литтла | [Масштабирование и трафик](scaling.md) |
| CAP, PACELC, согласованность, репликация и шардирование | [Распределённые данные](distributed-data.md) |
| Deadline, retry, circuit breaker, bulkhead и backpressure | [Устойчивость](reliability.md) |
| План интервью, оценки QPS и объёма данных | [Ход system design интервью](system-design-interview.md) |

**Собеседование:** [базовые вопросы](../../interview/base/design.md) · [продвинутые вопросы](../../interview/advanced/system-design.md) · [SOLID и паттерны](../../interview/base/patterns.md)

## Кейсы system design

- [Платёжная система: состояния, возвраты и идемпотентность](cases/payments.md)
- [Rate limiter](cases/rate-limiter.md)
- [Короткие ссылки](cases/short-url.md)
- [Чат](cases/chat.md)
- [Лента](cases/news-feed.md)
- [Key-value-хранилище](cases/key-value.md)
- [Генератор ID](cases/id-generator.md)
- [Веб-краулер](cases/web-crawler.md)
- [SMS и уведомления](cases/notifications.md)
