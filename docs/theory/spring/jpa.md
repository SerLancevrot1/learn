# JPA: планы загрузки и проблема N+1

[Spring](README.md) · [Лабораторная JPA и план загрузки](../../interview/tech-lead.md#jpa-и-план-загрузки) · [Индексы и EXPLAIN](../data/postgresql/performance.md)

## 8. ORM и проблема N+1

`[ ]` **Короткий ответ:** N+1 — один запрос за списком сущностей и ещё по одному на каждую связанную сущность. Обнаруживают по логам SQL или счётчику запросов в тестах. Решают загрузкой связей одним или несколькими запросами: DTO-проекция, `JOIN FETCH`, `@EntityGraph`, batch fetching.

```java
List<Order> orders = orderRepository.findAll();      // 1 запрос
for (Order order : orders) {
    order.getCustomer().getName();                    // + N запросов
}
```

`FetchType.LAZY` не устраняет проблему: он лишь откладывает загрузку до первого обращения.

| Способ | Когда подходит |
|---|---|
| DTO / projection | Нужны только отдельные поля, а не сущности целиком |
| `JOIN FETCH` | Нужна связь `to-one` или небольшая `to-many` без пагинации |
| `@EntityGraph` | То же, но декларативно, без переписывания JPQL |
| Batch fetching (`@BatchSize`, `hibernate.default_batch_fetch_size`) | Коллекции + пагинация: вместо N запросов — несколько запросов с `IN (...)` |

```java
@Query("select o from Order o join fetch o.customer where o.status = :status")
List<Order> findWithCustomerByStatus(@Param("status") OrderStatus status);
```

Подводные камни:

- `JOIN FETCH` для `to-many` размножает строки, а вместе с пагинацией Hibernate может выполнить её в памяти (предупреждение `HHH90003004` в Hibernate 6 / `HHH000104` в Hibernate 5).
- Два `JOIN FETCH` по разным коллекциям дают декартово произведение строк; для `List` Hibernate выбросит `MultipleBagFetchException`.
- Один гигантский join может оказаться хуже нескольких ограниченных запросов.
- Не загружайте весь граф объектов «на всякий случай».

Как обнаружить: логирование SQL (`spring.jpa.show-sql`, `org.hibernate.SQL`), статистика Hibernate (`hibernate.generate_statistics`), подсчёт запросов в интеграционных тестах.

<details>
<summary>Как N+1 выглядит на стороне базы?</summary>

Множество одинаковых коротких запросов, отличающихся только параметром: `SELECT ... FROM customers WHERE id = $1`. В `pg_stat_statements` у такого запроса огромное число вызовов при маленьком среднем времени. По отдельности каждый запрос быстрый — тормозит их количество и сетевые задержки.

</details>
