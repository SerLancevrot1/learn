# PostgreSQL: индексы и планы запросов

[Базы данных и кеши](../README.md)

## Главное

- **Индекс** ускоряет подходящие чтения ценой места и обслуживания при записи. Для составного B-tree смотри порядок колонок, предикаты и сортировку.
- **EXPLAIN ANALYZE** исполняет запрос. Сверь оценки с фактическими строками и числом проходов. `BUFFERS` помогает увидеть чтения страниц.

[SQL и схема](sql-and-schema.md) · [Локальный PostgreSQL](../../../../compose.yaml) · [Распределённые данные](../../architecture/distributed-data.md)

Партиционирование таблицы PostgreSQL делит её, например, по месяцам. Планировщик может отсечь лишние партиции, а старые данные можно удалять целыми партициями. Шардирование распределяет данные по узлам. Подбирай ключ под типичные запросы и учитывай стоимость операций между шардами. [Репликация, решардинг и CAP](../../architecture/distributed-data.md)

## Навигация

- [Индексы и планы выполнения](#3-индексы-и-планы-выполнения)
- [ORM и проблема N+1](../../spring/jpa.md#8-orm-и-проблема-n1)
- [Выбор хранилища](../storage-selection.md#9-выбор-хранилища)
- [Приложение: классы баз данных](../storage-selection.md#приложение-классы-баз-данных)
- [Источники в основном конспекте](sql-and-schema.md#11-источники)

## 3. Индексы и планы выполнения

### 3.1. Типы индексов

| Тип | Для чего | Пример использования |
|---|---|---|
| **B-tree** | Равенство, диапазоны, сортировка, уникальность. Тип по умолчанию | `WHERE created_at > ...`, `ORDER BY name` |
| **Hash** | Только равенство | `WHERE token = ...` — но B-tree обычно не хуже |
| **GIN** | Инвертированный индекс: элементы массивов, JSONB, полнотекстовый поиск, триграммы (`pg_trgm`) | `WHERE tags @> ARRAY['sql']` |
| **GiST / SP-GiST** | Каркасы для специализированных типов: геометрия, диапазоны, ближайшие соседи | `WHERE period && tsrange(...)` |
| **BRIN** | Компактный индекс диапазонов блоков для огромных таблиц, где физический порядок строк коррелирует со значением | Журнал событий, упорядоченный по времени |

Какие операторы поддерживает индекс, зависит от типа данных и operator class.

> Все индексы в PostgreSQL вторичные: таблица (heap) хранится отдельно, а индекс указывает на версии строк в ней.

### 3.2. Цена индекса

`[ ]` **Короткий ответ:** индекс ускоряет подходящие чтения, но занимает место, замедляет запись и используется не всегда — планировщик выбирает самый дешёвый по своей оценке план.

<details>
<summary>Подробности</summary>

- Индекс занимает место на диске и в памяти.
- Каждый `INSERT`, `UPDATE` (если не сработал HOT-update) и `DELETE` обновляет индексы.
- Если запрос читает значительную долю таблицы, последовательное чтение может быть дешевле множества случайных обращений по индексу.
- Индекс не применяется, если условие не совпадает с тем, что проиндексировано: `WHERE lower(email) = ?` при индексе на `email`, `WHERE created_at::date = ?` при индексе на `created_at`, `LIKE '%abc'` при обычном B-tree.
- План зависит от статистики. После массовой загрузки данных выполните `ANALYZE`.

</details>

### 3.3. Составные индексы

`[ ]` **Короткий ответ:** порядок колонок важен. B-tree `(a, b)` упорядочен сначала по `a`, затем по `b` внутри каждого `a`, поэтому эффективнее всего работает при условиях на ведущую колонку.

| Условие запроса | Польза от индекса `(a, b)` |
|---|---|
| `WHERE a = ?` | Да |
| `WHERE a = ? AND b = ?` | Да |
| `WHERE a = ? ORDER BY b` | Да, отдельная сортировка может не понадобиться |
| `WHERE a > ? AND b = ?` | Диапазон сужается по `a`; условие на `b` проверяется в индексе, но не обязательно сокращает читаемую часть |
| `WHERE b = ?` | Зависит от данных: см. ниже |

**Условие только по второй колонке.** Раньше ответ был «индекс почти бесполезен». С PostgreSQL 18 B-tree поддерживает **skip scan**: база перебирает различные значения `a` и для каждого ищет `b = ?`. Это выгодно, когда различных значений `a` немного. Если их много, планировщик обычно предпочтёт последовательное сканирование или полный проход по индексу.

<details>
<summary>Проверка на тестовых данных (1 млн строк, PostgreSQL 18.6)</summary>

Таблица `orders_big` из [3.6](#36-разбор-explain-от-медленного-запроса-к-быстрому): 3 значения `status`, около 10 000 значений `customer_id`.

Ведущая колонка с 3 значениями — skip scan:

```sql
CREATE INDEX ON orders_big (status, created_at);

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM orders_big WHERE created_at >= now() - interval '1 day';
```

```text
Index Only Scan using orders_big_status_created_at_idx on orders_big
  Index Cond: (created_at >= (now() - '1 day'::interval))
  Index Searches: 4
```

`Index Searches: 4` — несколько точечных спусков по дереву, а не проход по всему индексу.

Ведущая колонка с 10 000 значений:

```sql
CREATE INDEX ON orders_big (customer_id, status);

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM orders_big WHERE status = 'CANCELLED';
```

```text
Parallel Index Only Scan using orders_big_customer_id_status_idx on orders_big
  Index Cond: (status = 'CANCELLED'::text)
  Index Searches: 1
  Buffers: shared hit=3094 read=924
```

Здесь `Index Searches: 1` при тысячах прочитанных буферов: индекс прочитан целиком, без пропусков. Он использован, но эффективного поиска нет.

Вывод для собеседования: «используется индекс» и «индекс эффективен» — разные утверждения.

</details>

```sql
CREATE INDEX idx_orders_customer_created
    ON orders (customer_id, created_at);
```

Учитывайте селективность, сортировку, покрытие запроса и стоимость поддержки. Не создавайте индекс на каждую колонку автоматически.

### 3.4. Полезные разновидности

```sql
-- Частичный индекс: индексируются только нужные строки
CREATE INDEX idx_orders_pending ON orders (created_at)
    WHERE status = 'PENDING';

-- Покрывающий индекс: делает возможным Index Only Scan (но не гарантирует его, см. 3.5)
CREATE INDEX idx_orders_customer_incl ON orders (customer_id)
    INCLUDE (total);

-- Индекс по выражению: запрос должен использовать то же выражение
CREATE INDEX idx_users_email_lower ON users (lower(email));

-- Создание без длительной блокировки записи
CREATE INDEX CONCURRENTLY idx_orders_status ON orders (status);
```

<details>
<summary>Особенности CREATE INDEX CONCURRENTLY</summary>

- Нельзя выполнить внутри блока транзакции.
- Работает дольше обычного создания: таблица сканируется дважды, и нужно дождаться завершения транзакций, которые могут использовать старое состояние.
- При ошибке, например при нарушении уникальности, остаётся индекс в состоянии `INVALID`. Его нужно удалить и создать заново.

</details>

### 3.5. Index Only Scan и visibility map

`[ ]` **Вопрос:** почему в плане `Index Only Scan`, но `Heap Fetches` больше нуля?

**Короткий ответ:** все нужные колонки есть в индексе, но PostgreSQL должен ещё проверить, что версия строки видна текущему снимку MVCC. Информация о видимости хранится в таблице, а не в индексе. Если страница таблицы не отмечена в visibility map как all-visible, базе приходится читать саму строку.

<details>
<summary>Подробности</summary>

- Visibility map — битовая карта: для каждой страницы таблицы отмечено, видны ли все её строки всем транзакциям.
- Бит сбрасывается при изменении строк на странице и снова выставляется `VACUUM`.
- Если бит не выставлен, `Index Only Scan` не выигрывает у обычного `Index Scan`.
- Отсюда практическое следствие: покрывающие индексы полезны прежде всего для таблиц, которые меняются редко или регулярно обрабатываются `VACUUM`.
- `INCLUDE`-колонки увеличивают размер индекса. Добавляйте их осторожно, особенно широкие.
- В уникальном индексе с `INCLUDE` уникальность проверяется только по ключевым колонкам.

Как воспроизвести — в [3.6](#36-разбор-explain-от-медленного-запроса-к-быстрому), шаги 4–6.

</details>

### 3.6. Разбор EXPLAIN: от медленного запроса к быстрому

Ниже — реальные планы на PostgreSQL 18.6. Время у вас будет другим: сравнивайте **форму плана, число строк и буферы**, а не миллисекунды.

**Подготовка: 1 млн заказов, около 10 000 клиентов.**

```sql
DROP TABLE IF EXISTS orders_big;
CREATE TABLE orders_big (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id int         NOT NULL,
    status      text        NOT NULL,
    created_at  timestamptz NOT NULL,
    total       numeric(10, 2) NOT NULL
);

INSERT INTO orders_big (customer_id, status, created_at, total)
SELECT (random() * 9999)::int + 1,
       (ARRAY['NEW', 'PAID', 'CANCELLED'])[1 + (random() * 2)::int],
       now() - random() * interval '365 days',
       round((random() * 1000)::numeric, 2)
FROM generate_series(1, 1000000);

ANALYZE orders_big;
```

**Запрос, который будем ускорять:** 20 последних заказов клиента.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, created_at, total
FROM orders_big
WHERE customer_id = 42
ORDER BY created_at DESC
LIMIT 20;
```

> `EXPLAIN ANALYZE` действительно выполняет запрос. Для `UPDATE`/`DELETE` оборачивайте его в `BEGIN; ... ROLLBACK;`.

Как читать узел плана:

```text
Seq Scan on t  (cost=0.00..13318.33 rows=42 width=22) (actual time=0.642..23.796 rows=33.00 loops=3)
```

- `cost=0.00..13318.33` — оценка стоимости до первой и до последней строки в условных единицах;
- `rows=42` — сколько строк ожидает планировщик;
- `width=22` — средний размер строки в байтах;
- `actual time=0.642..23.796` — фактическое время до первой и до последней строки, мс;
- `actual rows=33.00` — фактическое число строк **на одно выполнение** узла;
- `loops=3` — сколько раз выполнялся узел; общее число строк — `rows × loops`.

План читается изнутри наружу: самый вложенный узел выполняется первым.

#### Шаг 1. Индекса нет

```text
Limit  (actual time=31.362..33.016 rows=20.00 loops=1)
  Buffers: shared hit=8186
  ->  Gather Merge
        Workers Launched: 2
        ->  Sort
              Sort Key: created_at DESC
              ->  Parallel Seq Scan on orders_big  (rows=42) (actual rows=33.00 loops=3)
                    Filter: (customer_id = 42)
                    Rows Removed by Filter: 333300
Execution Time: 33.066 ms
```

`[ ]` Что вы видите?

<details>
<summary>Разбор</summary>

- `Parallel Seq Scan` + `Rows Removed by Filter: 333300` в каждом из трёх процессов: прочитана вся таблица, чтобы найти около 100 строк.
- `Buffers: shared hit=8186` — прочитано около 8 тыс. страниц по 8 КБ, то есть вся таблица.
- `Sort` — сортировка выполняется отдельно, после фильтрации.
- Оценка `rows=42` против фактических 33 на процесс — статистика адекватна, проблема не в ней.

Гипотеза: нужен индекс по `customer_id`.

</details>

#### Шаг 2. Индекс только по фильтру

```sql
CREATE INDEX idx_ob_customer ON orders_big (customer_id);
```

```text
Limit  (actual time=0.470..0.473 rows=20.00 loops=1)
  Buffers: shared hit=102 read=3
  ->  Sort
        Sort Key: created_at DESC
        Sort Method: top-N heapsort  Memory: 27kB
        ->  Bitmap Heap Scan on orders_big  (actual rows=99.00 loops=1)
              Recheck Cond: (customer_id = 42)
              Heap Blocks: exact=99
              ->  Bitmap Index Scan on idx_ob_customer  (actual rows=99.00 loops=1)
                    Index Cond: (customer_id = 42)
Execution Time: 0.511 ms
```

`[ ]` Что улучшилось и что осталось?

<details>
<summary>Разбор</summary>

- Буферов стало около 100 вместо 8 тыс.
- `Heap Blocks: exact=99` — 99 строк клиента лежат на 99 разных страницах таблицы. Это нормально для данных, вставленных вперемешку.
- `Bitmap Heap Scan`: сначала по индексу собирается битовая карта страниц, затем страницы читаются в физическом порядке.
- По-прежнему есть `Sort`: читаются все 99 строк клиента, хотя нужны 20.

Гипотеза: добавить `created_at` в индекс, чтобы строки сразу выходили в нужном порядке.

</details>

#### Шаг 3. Индекс под фильтр и сортировку

```sql
DROP INDEX idx_ob_customer;
CREATE INDEX idx_ob_customer_created ON orders_big (customer_id, created_at DESC);
```

```text
Limit  (actual time=0.038..0.151 rows=20.00 loops=1)
  Buffers: shared hit=20 read=3
  ->  Index Scan using idx_ob_customer_created on orders_big  (actual rows=20.00 loops=1)
        Index Cond: (customer_id = 42)
        Index Searches: 1
Execution Time: 0.187 ms
```

<details>
<summary>Разбор</summary>

- Узла `Sort` больше нет: индекс отдаёт строки уже отсортированными.
- `actual rows=20` — благодаря `LIMIT` чтение остановилось после 20 строк.
- Около 23 буферов: несколько страниц индекса и около 20 страниц таблицы.
- `DESC` в определении индекса здесь не обязателен: B-tree можно читать в обратном направлении. Направление важно, когда сортировка смешанная, например `ORDER BY a ASC, b DESC`.

</details>

#### Шаг 4. Ловушка: INCLUDE без нужной колонки

```sql
DROP INDEX idx_ob_customer_created;
CREATE INDEX idx_ob_cc_incl ON orders_big (customer_id, created_at DESC) INCLUDE (total);
```

```text
->  Index Scan using idx_ob_cc_incl on orders_big
```

`[ ]` Почему не `Index Only Scan`?

<details>
<summary>Разбор</summary>

Запрос выбирает `id`, а его нет в индексе. Чтобы получить `id`, всё равно нужно читать таблицу. Покрывающий индекс должен содержать **все** колонки, которые использует запрос.

</details>

#### Шаг 5. Покрывающий индекс

```sql
DROP INDEX idx_ob_cc_incl;
CREATE INDEX idx_ob_cc_incl ON orders_big (customer_id, created_at DESC) INCLUDE (total, id);
VACUUM orders_big;
```

```text
Limit  (actual time=0.024..0.026 rows=20.00 loops=1)
  ->  Index Only Scan using idx_ob_cc_incl on orders_big
        Index Cond: (customer_id = 42)
        Heap Fetches: 0
        Buffers: shared hit=4
```

`Heap Fetches: 0`, 4 буфера: обращений к таблице нет.

#### Шаг 6. Обновили строки — Index Only Scan снова читает таблицу

```sql
UPDATE orders_big SET status = status WHERE customer_id = 42;
-- повторить EXPLAIN
```

```text
->  Index Only Scan using idx_ob_cc_incl on orders_big
      Heap Fetches: 40
      Buffers: shared hit=44
```

<details>
<summary>Разбор</summary>

Узел по-прежнему называется `Index Only Scan`, но `Heap Fetches: 40`, а буферов в 10 раз больше. `UPDATE` создал новые версии строк, и их страницы больше не отмечены как all-visible в visibility map. После `VACUUM orders_big` снова `Heap Fetches: 0`.

Выводы:

1. Название узла не гарантирует, что таблица не читается — смотрите `Heap Fetches`.
2. Для часто обновляемых таблиц выгода от покрывающего индекса может быть небольшой.
3. Каждый следующий шаг — это компромисс: индекс шага 5 больше и дороже в поддержке, чем индекс шага 3. Выигрыш от него стоит проверять на реальной нагрузке.

</details>

#### Чек-лист чтения плана

- [ ] Какой тип сканирования: `Seq Scan`, `Index Scan`, `Index Only Scan`, `Bitmap Heap Scan`?
- [ ] Сколько строк отброшено фильтром (`Rows Removed by Filter`)?
- [ ] Сильно ли расходятся `rows` (оценка) и `actual rows`? Если в 10+ раз — проверьте статистику (`ANALYZE`) и коррелированные условия.
- [ ] Есть ли отдельный `Sort`? Пишет ли он на диск (`Sort Method: external merge`)?
- [ ] Сколько буферов прочитано (`shared hit` — из кэша, `read` — с диска или из кэша ОС)?
- [ ] Для `Nested Loop`: сколько `loops` у внутреннего узла?
- [ ] Для `Index Only Scan`: сколько `Heap Fetches`?
