# Практикум для собеседований

[Маршрут](../../INTERVIEW.md) · [Практикум по внешним сервисам](../../LABS.md)

Для Maven-команд нужен JDK 17 или новее. Если Wrapper не находит Java, укажи путь к установленному JDK в `JAVA_HOME` текущего терминала. Команды рассчитаны на PowerShell и выполняются из корня репозитория. В Bash используй `./mvnw`.

## Spring: прокси и откат

**Задача.** Сервис вставляет заказ, затем выбрасывает исключение. Предскажи, сохранится ли строка при вызове через `this` и при вызове из другого bean. Отдельно разбери случай, когда внешний транзакционный метод ловит ошибку внутреннего `REQUIRED`.

```powershell
.\mvnw.cmd "-Dtest=ProxyTransactionLabTest" test
```

Открой [ProxyTransactionLabTest](../../src/test/java/ru/learn/interview/ProxyTransactionLabTest.java). Поставь breakpoint в `insertThenFail` и проверь `TransactionSynchronizationManager.isActualTransactionActive()`. Измени вызов в учебном примере так, чтобы он проходил через прокси, и предскажи, какое утверждение теста станет неверным.

<details>
<summary>Подсказка</summary>

Отдельно проверь, кто открывает транзакцию и кто видит исключение. Spring создаёт прокси для bean, поэтому значение имеет маршрут вызова.

</details>

<details>
<summary>Разбор</summary>

В `placeFromSameBean` вызов `this.insertThenFail` обходит прокси. Вставка выполняется без транзакции Spring и сохраняется. Из `Checkout.place` вызов идёт через прокси `OrderWriter`, открывается транзакция, при исключении вставка откатывается. В `catchRequiredFailure` оба метода используют одну физическую транзакцию. Внутренний interceptor помечает её rollback-only. Внешний метод ловит исключение и успевает выполнить ещё одну вставку, затем Spring откатывает обе и выбрасывает `UnexpectedRollbackException`.

Если в том же bean уже есть активная внешняя транзакция, self-invocation всё равно обходит перехватчик, а JDBC-операции могут участвовать в текущей транзакции. Это полезное уточнение на собеседовании.

[Конспект](../../src/main/java/ru/learn/theory/Spring.md#42-self-invocation) · [Документация Spring](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

</details>

## PostgreSQL: потерянное обновление

**Задача.** В двух транзакциях клиенты прочитали 100. Первый увеличивает значение на 10, второй на 20. Воспроизведи ошибку и исправь её на SQL-уровне.

```powershell
docker compose up -d --wait postgres
docker compose exec postgres psql -X -U myuser -d mydatabase
```

Открой команду `psql` в двух терминалах, **A** и **B**. Один раз в A создай таблицу лабораторной.

```sql
CREATE SCHEMA IF NOT EXISTS interview_labs;
CREATE TABLE IF NOT EXISTS interview_labs.counter (id int PRIMARY KEY, value int NOT NULL);
INSERT INTO interview_labs.counter VALUES (1, 100)
ON CONFLICT (id) DO UPDATE SET value = EXCLUDED.value;
```

| Шаг | Сессия | Что выполнить |
| --- | --- | --- |
| 1 | A | `BEGIN ISOLATION LEVEL READ COMMITTED; SELECT value FROM interview_labs.counter WHERE id = 1;` |
| 2 | B | `BEGIN ISOLATION LEVEL READ COMMITTED; SELECT value FROM interview_labs.counter WHERE id = 1;` |
| 3 | A | `UPDATE interview_labs.counter SET value = 110 WHERE id = 1; COMMIT;` |
| 4 | B | `UPDATE interview_labs.counter SET value = 120 WHERE id = 1; COMMIT;` |
| 5 | A | `SELECT value FROM interview_labs.counter WHERE id = 1;` |

**Исправление.** Верни 100 отдельным `UPDATE`. Повтори те же шаги, но на шагах 3 и 4 используй `SET value = value + 10` и `SET value = value + 20`. Для дополнительного опыта запусти второй `UPDATE` до коммита A. Посмотри ожидание, затем закоммить A и проверь результат B.

Автоматическая проверка использует две JDBC-сессии и свою временную схему. Для подключения к Compose узнай фактически опубликованный порт.

```powershell
$port = (docker compose port postgres 5432).Split(':')[-1]
$env:LAB_POSTGRES_URL = "jdbc:postgresql://127.0.0.1:$port/mydatabase"
.\mvnw.cmd "-Dtest=PostgresConcurrencyLabTest" test
Remove-Item Env:LAB_POSTGRES_URL
```

При другом стенде можно задать `LAB_POSTGRES_USER` и `LAB_POSTGRES_PASSWORD`. Пользователю БД нужны права создания схемы в учебной базе. После опыта выйди из `psql` через `\q`. Для остановки сервиса используй `docker compose stop postgres`.

<details>
<summary>Разбор</summary>

После первой последовательности останется 120, потому что приложение рассчитало абсолютные значения на старом снимке. Во второй последовательности UPDATE использует актуальную версию строки и сохраняет оба приращения, получается 130. Для условного решения на основе прочитанного значения пригодятся версия, `SELECT ... FOR UPDATE` или другая граница изоляции. При ошибке сериализации повторяй всю транзакцию с новым чтением данных.

[Автоматическая проверка](../../src/test/java/ru/learn/interview/PostgresConcurrencyLabTest.java) · [Другие опыты в двух сессиях](../../src/main/java/ru/learn/theory/DatabaseTransactions.md#65-лабораторные-две-сессии) · [Документация PostgreSQL](https://www.postgresql.org/docs/current/transaction-iso.html)

</details>

## Кошелёк: Outbox, Kafka и Inbox

**Задача.** У заявки на пополнение есть ID `credit-1` и сумма 100 копеек. Предскажи состояние заявки, журнала Kafka и баланса после каждого сбоя. Затем проверь, почему две записи в Kafka дают одно начисление.

```powershell
.\mvnw.cmd "-Dtest=WalletOutboxTest,WalletLedgerTest,KafkaDeliveryIntegrationTest" test
```

Используются Embedded Kafka и две независимые H2-базы в режиме PostgreSQL. Исходная БД хранит заявку и строку Outbox. БД получателя хранит кошелёк и Inbox. HTTP-контракт из [пробного system design](../../INTERVIEW.md#6-system-design-и-рассказ-о-проекте--40-минут) — задание для обсуждения. В исполняемой лабораторной роль клиента выполняет тест.

| Когда произошёл сбой | Что предсказать |
| --- | --- |
| До коммита заявки и Outbox | Есть ли что публиковать после перезапуска |
| После коммита заявки, до отправки | Как новый relay найдёт заявку |
| После ACK Kafka, до отметки в Outbox | Сколько записей будет в Kafka после повтора |
| После коммита баланса, до коммита offset | Что увидит новый consumer и изменится ли баланс |

<details>
<summary>Подсказка</summary>

Отдели идентификатор бизнес-события от Kafka offset. Посмотри, какая транзакция владеет таблицами на каждой стороне.

</details>

<details>
<summary>Разбор</summary>

1. `enqueue` атомарно пишет заявку и строку Outbox. Rollback убирает обе. С тем же ID и суммой повтор заявки безопасен, с другим payload возникает конфликт.
2. После коммита новый relay читает незавершённую строку. `publishNext` отмечает её выполненной только после подтверждения публикации.
3. Если ACK уже пришёл, а отметка не зафиксирована, новый relay публикует тот же ID. Kafka содержит две записи с разными offsets. Потребитель фиксирует ID и меняет баланс в одной локальной транзакции. Первое событие даёт 100, второе оставляет 100.
4. Сбой consumer после записи в БД до коммита offset воспроизводится отдельно в `KafkaDeliveryIntegrationTest`. Повторное чтение безопасно при работающем Inbox.

`FOR UPDATE SKIP LOCKED` позволяет конкурирующим relay не брать уже захваченную строку. Учебная реализация держит транзакцию и соединение на время ожидания ACK. При росте нагрузки обсуди короткий claim с lease и повторным захватом, ограничение попыток, возраст старейшей заявки и требования к порядку. Стабильный ключ партиции помогает упорядочивать записи одного кошелька, а конкуренция relay и повторы требуют отдельной гарантии порядка бизнес-событий.

[WalletOutbox](../../src/main/java/ru/learn/delivery/WalletOutbox.java) · [Сценарии отправителя](../../src/test/java/ru/learn/delivery/WalletOutboxTest.java) · [Сценарии потребителя](../../src/test/java/ru/learn/kafka/delivery/KafkaDeliveryIntegrationTest.java) · [Гарантии Kafka](https://kafka.apache.org/32/design/design/#message-delivery-semantics)

</details>

### Усложнение

- Измени relay так, чтобы он отмечал запись до подтверждения и фиксировал эту отметку отдельно. Смоделируй отказ публикации и объясни, почему повтор больше не найдёт запись.
- Убери Inbox только в тестовом сценарии и предскажи баланс после повторной доставки.
- Допусти два одновременных пополнения разными суммами. Объясни, когда порядок не влияет на результат, а когда понадобится версия кошелька или последовательность событий.
- Спроектируй API статуса заявки. Опиши состояния, которые клиент увидит при недоступности Kafka и при таймауте после коммита заявки.
