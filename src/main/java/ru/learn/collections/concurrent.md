# Конкурентные коллекции в Java

[Практикум](README.md) · [Очереди](queues.md) · [Код примеров](ConcurrentCollectionsDemo.java)

## Что выбрать для разделяемых данных

| Требование | Реализация | Цена и гарантия |
| --- | --- | --- |
| Поиск и обновление по ключу без сортировки | `ConcurrentHashMap<K, V>` | Поиск обычно O(1) при хорошем распределении хешей. Атомарные операции над одной записью, чтение обычно не блокируется. |
| Проверка и регистрация уникального ID | `ConcurrentHashMap.newKeySet()` | `add(id)` сообщает, впервые ли добавлен ID, без отдельной проверки `contains`. |
| Сортированные ключи, диапазоны и соседи | `ConcurrentSkipListMap<K, V>`, `ConcurrentSkipListSet<E>` | Список с пропусками. Поиск, добавление и удаление в среднем O(log n). Представления диапазонов связаны с исходником. |
| Часто обходить список подписчиков, редко менять его состав | `CopyOnWriteArrayList<E>` | Чтение по индексу O(1). Изменение состава копирует массив за O(n). Итератор читает снимок массива. |
| Обход небольшого множества подписок при редких изменениях | `CopyOnWriteArraySet<E>` | Снимок при обходе, поиск элемента линейный, изменение с копированием. |
| Защитить обычную коллекцию одним монитором | `Collections.synchronizedList/Set/Map` | Доступ через обёртку сериализуется. Один монитор также можно удерживать при обходе и составных операциях. |
| Поток заданий, ожидание, ограничение буфера | `ConcurrentLinkedQueue`, `BlockingQueue`, `BlockingDeque`, `TransferQueue` | Выбираются по порядку, ёмкости и правилам ожидания. [Таблица очередей](queues.md#выбор-реализации) |

`ConcurrentHashMap`, его `newKeySet()`, `ConcurrentSkipListMap` и `ConcurrentSkipListSet` запрещают `null`. `CopyOnWriteArrayList` и `CopyOnWriteArraySet` допускают его. [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) · [ConcurrentSkipListMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html) · [ConcurrentSkipListSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentSkipListSet.html) · [CopyOnWriteArrayList](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html) · [CopyOnWriteArraySet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CopyOnWriteArraySet.html)

## Атомарные операции над записью

Два потока могут прочитать один и тот же счётчик, оба прибавить единицу и записать одинаковый результат. `ConcurrentHashMap` защищает отдельные операции `get` и `put`. Для обновления счётчика одним шагом используется `merge`. [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

```java
ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
counts.merge("order-created", 1, Integer::sum);
```

| Метод `ConcurrentHashMap` | Что даёт в одном вызове |
| --- | --- |
| `putIfAbsent(key, value)` | Регистрирует только отсутствующий ключ, возвращает прежнее значение или `null`. |
| `computeIfAbsent(key, factory)` | Вычисляет значение при отсутствии ключа. Если функция вернула `null`, запись не создаётся. |
| `compute(key, update)` | Пересчитывает значение по ключу, передавая `null` при отсутствии. Результат `null` удаляет существующую запись. |
| `merge(key, value, combine)` | Создаёт запись с `value` или объединяет его с имеющимся значением. Результат `null` удаляет существующую запись. |
| `replace(key, expected, update)` | Заменяет запись, только если значение совпало с ожидаемым. |
| `remove(key, expected)` | Удаляет запись, только если её значение совпало с ожидаемым. |

Функции для `compute` и `merge` держи короткими. Пока функция работает, некоторые обновления других потоков могут ждать. Внутри функции избегай обновлений той же карты. Для `ConcurrentSkipListMap` проверяй контракт вычисляющих методов отдельно. В частности, `computeIfAbsent` может вызвать функцию повторно при конкурирующих вызовах. [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) · [ConcurrentSkipListMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentSkipListMap.html)

Для интенсивных счётчиков Javadoc показывает `ConcurrentHashMap<String, LongAdder>` и `computeIfAbsent(key, k -> new LongAdder()).increment()`. Это подходит, когда счётчики сохраняются в карте, а чтение суммы во время обновлений допускает приблизительный результат. [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) · [LongAdder](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/atomic/LongAdder.html)

### Значения, инварианты и видимость

- `map.computeIfAbsent(city, k -> new ArrayList<>()).add(order)` выполняет `add` за пределами операции над картой. Если несколько потоков меняют список, нужен собственный механизм синхронизации, конкурентная коллекция в значении или замена значения целиком внутри атомарной операции над ключом.
- Изменение двух ключей или двух коллекций остаётся двумя операциями. Например, обновление счётчика доставки и добавление ID в множество видны по отдельности. Если инвариант должен сохраняться между двумя записями, защищай его общей синхронизацией и используй её на всех путях чтения и записи.
- Между действиями до помещения элемента в конкурентную коллекцию и действиями другого потока после получения этого элемента есть отношение *happens-before*. Если объект продолжает меняться после помещения, эти изменения требуют своей синхронизации. У `ConcurrentHashMap` гарантия видимости сформулирована для обновления по ключу и чтения, вернувшего обновлённое значение. [Согласованность памяти в java.util.concurrent](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/package-summary.html) · [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

## Обход при изменениях

| Как устроен итератор | Где встречается | Что произойдёт при изменении исходной коллекции |
| --- | --- | --- |
| Fail-fast | `ArrayList`, `HashMap`, `TreeSet` | При структурном изменении в обход итератора может бросить `ConcurrentModificationException` даже в одном потоке. Проверка выполняется по мере обхода и не годится для синхронизации. |
| Слабо согласованный | `ConcurrentHashMap`, `ConcurrentSkipListSet`, `ConcurrentLinkedQueue` | Обход продолжится. Он может увидеть часть параллельных изменений, без снимка всей коллекции на один момент. |
| Снимок | `CopyOnWriteArrayList`, `CopyOnWriteArraySet` | Итератор видит состав на момент создания. Изменения состава появятся в следующем обходе. Удаление через итератор запрещено. Сами объекты остаются общими по ссылкам. |
| Итератор синхронизированной обёртки | `Collections.synchronizedList/Set/Map` | Удерживай монитор обёртки от создания итератора до завершения обхода. Для `map.keySet()` и других представлений захватывай монитор самой обёрнутой карты. |

При параллельных обновлениях `size()` и `isEmpty()` у `ConcurrentHashMap` подходят для оценки состояния, которое может сразу измениться. Для управления обновлениями используй атомарную операцию над нужным ключом. У `Vector` и `Hashtable`, как и у синхронизированных обёрток, последовательность из нескольких вызовов требует общего монитора, если её результат должен быть атомарным. [Коллекции в java.util.concurrent](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/package-summary.html) · [Синхронизированные обёртки](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collections.html#synchronizedMap(java.util.Map))

## Проверь на примерах

1. В [ConcurrentCollectionsDemo](ConcurrentCollectionsDemo.java) два потока регистрируют доставку одного события. Результат показывает две попытки и ровно одну первую регистрацию. Объясни, когда можно наблюдать счётчик и множество в разных состояниях.
2. Там же итератор рассылки создаётся до добавления подписчика. Сравни вывод текущего и следующего обхода. Что изменится, если взять итератор сразу после добавления?
3. В [BlockingQueuesDemo](BlockingQueuesDemo.java) ограниченный буфер отказывает без потребителя. Найди, какой вызов ждёт освобождения места и какой ждёт следующего отчёта.
