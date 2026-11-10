# Java Stream API

- [40 задач с решениями](EXERCISES.md) — от `filter` и `map` до группировки, параллельного `reduce` и собственного коллектора.
- [StreamApiCases.java](StreamApiCases.java) — семь запускаемых примеров с разбором ниже.
- [MapTask.java](MapTask.java) — пример разворота словаря «группа → список значений» в словарь «значение → группа». Похожая задача есть в практикуме под номером 20.

Запусти `StreamApiCases.main` в IDE или выполни из корня проекта с JDK 17 или новее.

```sh
java src/main/java/ru/learn/stream/api/StreamApiCases.java
```

## 1. Почему `count` может не вызвать `peek`

У списка уже известен размер. `peek` его не меняет, поэтому Java вправе вернуть `3` напрямую. В таком запуске строки из `peek` не появятся. Если нужен вывод для каждого элемента, используй терминальную операцию `forEach`, а когда важен порядок источника — `forEachOrdered`. [Контракт `Stream.count`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html#count())

## 2. Почему один поток нельзя использовать дважды

После `count()` поток уже использован. В примере вызов `findFirst()` на том же объекте даёт `IllegalStateException`. Данные остались в списке, поэтому повторный вызов `words.stream()` создаёт новый поток для второго запроса. [Контракт `Stream`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html)

## 3. Почему `takeWhile` теряет последнее чётное число

На `[2, 4, 1, 6]` операция `takeWhile` возвращает `[2, 4]`. Она берёт непрерывное начало упорядоченного потока и останавливается на `1`. `filter` проверяет условие для всех элементов и возвращает `[2, 4, 6]`. [Контракт `Stream.takeWhile`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html#takeWhile(java.util.function.Predicate))

## 4. Как сохранить группу, в которой ничего не подошло

У магазина `B` есть заказ, но он не оплачен. `groupingBy` создаёт группу для `B`, а вложенный `filtering` передаёт в `counting` только оплаченные заказы. Поэтому у `B` остаётся счётчик `0`. Если отфильтровать заказы до группировки, ключ `B` исчезнет. [Контракт `Collectors.filtering`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Collectors.html#filtering(java.util.function.Predicate,java.util.stream.Collector))

## 5. Почему `distinct` и `TreeSet` по-разному убирают повторы

`distinct` использует `equals`, поэтому сохраняет `cat` и `dog`. `TreeSet` считает элементы одинаковыми, если компаратор возвращает `0`. При сравнении только по длине `dog` совпадает с уже добавленным `cat`. В результате в множестве остаются `[cat, bear]`. Такой компаратор считает равными строки, которые различаются по `equals`, поэтому множество ведёт себя иначе, чем обычный `Set`. [Контракт `Stream.distinct`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html#distinct()) · [Контракт `TreeSet`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/TreeSet.html)

## 6. Почему `findFirst` падает на `null`

Поток из `[null, 'ready']` выбирает `null` как первый элемент. `Optional` не может содержать `null`, поэтому `findFirst` выбрасывает `NullPointerException`. `flatMap(Stream::ofNullable)` заменяет каждый `null` пустым потоком, после чего первым остаётся `ready`. [Контракт `Stream.findFirst`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html#findFirst())

## 7. Кто закрывает потоки внутри `flatMap`

Из `left` и `right` создаются два внутренних потока по два элемента. `flatMap` закрывает каждый внутренний поток после использования. Обработчики `onClose` записывают имена, поэтому после `toList()` список `closed` содержит `[left, right]`. [Контракт `Stream.flatMap`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html#flatMap(java.util.function.Function))
