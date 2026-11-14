# Java Collections для собеседования

[Java](README.md) · [Практикум с решениями](../../../src/main/java/ru/learn/collections/README.md) · [Вопросы Java Core](../../interview/base/java.md#коллекции-и-stream-api)

## С чего начать

Коллекции хранят группы объектов и дают готовые операции: добавить, найти, удалить, перебрать. Массив пригодится, когда размер фиксирован или нужны примитивы, например `int[]`. Коллекции работают со ссылочными типами, поэтому для целых чисел используют `List<Integer>`.

| Что важно для задачи | Интерфейс | С чего начать |
| --- | --- | --- |
| Сохранить последовательность, допустить повторы, читать по индексу | `List<E>` | `ArrayList`. Например, позиции заказа. |
| Хранить только уникальные элементы | `Set<E>` | `HashSet`. Например, множество уже обработанных ID. |
| Находить значение по ключу | `Map<K, V>` | `HashMap`. Например, заказ по ID. |
| Забирать элементы по очереди | `Queue<E>` | `ArrayDeque` для FIFO. `PriorityQueue`, если первым идёт самый приоритетный. |
| Добавлять и забирать с обоих концов, в том числе использовать как стек | `Deque<E>` | `ArrayDeque`. Например, стек открытых скобок. |

`List`, `Set` и `Queue` расширяют `Collection`. `Deque` расширяет `Queue`. `Map` образует отдельную ветку и даёт представления ключей, значений и записей. `Collections` — класс со статическими методами, например `reverse`, `sort` и `unmodifiableList`. [Руководство Dev.java](https://dev.java/learn/api/collections-and-streams/collections-framework/) · [API Collections](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collections.html)

В объявлении выбирай интерфейс, который выражает нужные операции. `Map<Long, Order>` подходит для поиска по ID. `NavigableMap<Instant, Order>` говорит, что понадобятся соседние ключи или диапазоны.

## List. ArrayList или LinkedList

**`ArrayList`** хранит ссылки во внутреннем массиве. `get(i)` и `set(i, x)` работают за O(1). `contains(x)` проверяет элементы по очереди за O(n) в худшем случае. Вставка или удаление в середине сдвигает хвост за O(n). При заполнении внутренний массив увеличивается, и ссылки копируются. Поэтому `add(x)` в конец стоит O(1) **амортизированно**. Много добавлений подряд стоят в среднем O(1) каждое, хотя добавление с расширением может занять O(n). У `new ArrayList<>(100)` ёмкость 100, а `size()` равен 0. [API ArrayList](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayList.html)

**`LinkedList`** — цепочка узлов со ссылками на соседей. `get(i)` идёт с ближайшего конца и в худшем случае стоит O(n). Вставка O(1) возможна на конце или в уже найденном месте, например через `ListIterator`. Поиск места по индексу вернёт O(n) на всю операцию. Каждый узел — дополнительный объект. Цикл `get(i)` по всем индексам `LinkedList` стоит O(n²), а один проход итератором — O(n). [API LinkedList](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedList.html)

Для обычного списка бери `ArrayList`. Для очереди или стека бери `ArrayDeque`. `Vector` и `Stack` встречаются в старом коде. Синхронизация отдельных методов `Vector` оставляет составные операции вроде «проверить, затем добавить» отдельными шагами. [API ArrayDeque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayDeque.html)

## Set и Map. Какой порядок нужен

Пусть числа `3, 1, 3, 2` приходят по одному. Множество на хеше уберёт повтор `3`. `LinkedHashSet` отдаст `[3, 1, 2]` в порядке первого появления. `TreeSet` отдаст `[1, 2, 3]`, потому что числа сортируются по естественному порядку.

| Требование | Множество | Карта | Что внутри и сколько стоит основная операция |
| --- | --- | --- | --- |
| Быстрая проверка наличия или поиск по ключу. Порядок произвольный | `HashSet` | `HashMap` | Хеш-таблица. `HashSet` использует `HashMap` внутри. Поиск и изменение обычно O(1) при хорошем распределении хешей. |
| Перебирать в порядке вставки | `LinkedHashSet` | `LinkedHashMap` | Хеш-таблица и двусвязный список для порядка. Основные операции обычно O(1) при хорошем распределении, нужны дополнительные ссылки. |
| Перебирать по возрастанию, запрашивать соседей и диапазоны | `TreeSet` | `TreeMap` | Сбалансированное красно-чёрное дерево, в котором ветви выбираются сравнением. Поиск, добавление и удаление O(log n). |
| Элементы или ключи из одного `enum` | `EnumSet` | `EnumMap` | Биты в `EnumSet`, массив в `EnumMap`. Порядок соответствует объявлению констант. Например, набор разрешений или счётчик заказов по статусу. |

[API HashSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashSet.html) · [API LinkedHashSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedHashSet.html) · [API LinkedHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedHashMap.html) · [API TreeSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/TreeSet.html) · [API TreeMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/TreeMap.html) · [API EnumSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/EnumSet.html) · [API EnumMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/EnumMap.html)

- `LinkedHashMap` умеет хранить записи в порядке **доступа** при `accessOrder = true`. Успешное чтение через `get` перемещает запись в конец. Первая запись — кандидат на вытеснение из LRU-кеша. Полный обход `LinkedHashMap` стоит O(n), а у `HashMap` зависит ещё и от ёмкости таблицы. [API LinkedHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedHashMap.html) · [API HashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html)
- Для `TreeMap` метод `floorKey(25)` найдёт наибольший ключ `<= 25`, `ceilingKey(25)` — наименьший `>= 25`. У `lowerKey` и `higherKey` границы строгие. `subMap` даёт представление диапазона. Например, в тарифах по дате `floorEntry(дата)` найдёт последний тариф, который уже вступил в силу. [API NavigableMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/NavigableMap.html)
- Для `TreeSet` и `TreeMap` результат сравнения `0` означает один элемент или ключ. Если сравнивать строки только по длине, `"cat"` и `"dog"` попадут в одну позицию. Чтобы хранить обе, после длины сравни сами строки через `thenComparing`. Естественный порядок задаёт `Comparable`, внешний порядок задаёт `Comparator`. Для соблюдения общего контракта `Set` и `Map` результат `0` должен совпадать с равенством по `equals`. [API TreeSet](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/TreeSet.html) · [API TreeMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/TreeMap.html)

### HashMap. Что происходит при put и get

1. Карта получает `hashCode()` ключа и вычисляет индекс корзины — ячейки внутреннего массива. Несколько ключей могут попасть в одну корзину. Это **коллизия**.
2. В корзине карта проверяет сохранённый хеш, затем совпадение ссылки или `equals`. При `put` равный ключ обновляет значение, а новый ключ добавляет запись. При `get` совпадение даёт искомое значение.
3. Когда число записей превышает порог, таблица расширяется, а записи распределяются по новой таблице. Порог зависит от количества корзин и коэффициента заполнения, по умолчанию `0.75`. Типичный `get` занимает O(1) при хорошем распределении, а `put` иногда платит за расширение.

**Контракт ключа.** Равные по `equals` объекты обязаны иметь одинаковый `hashCode`. Одинаковый хеш допускается у разных объектов. Пока объект служит ключом, оставляй поля, участвующие в сравнении и хешировании, стабильными. Например, если хеш зависит от имени пользователя и имя поменялось, поиск может пойти в другую корзину. Выбирай стабильный `Long` ID, `String` или `record` со стабильными компонентами. [API HashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html) · [API Map](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Map.html) · [Опыт с изменяемым ключом](language.md#лабораторная-1-изменённый-ключ)

<details>
<summary>Уточнение для интервью про 8, 64 и O(log n)</summary>

В OpenJDK 17 размер таблицы — степень двойки. Для вычисления корзины `HashMap` смешивает биты хеша (`h ^ (h >>> 16)`), затем берёт `(capacity - 1) & hash`. При добавлении новой записи через `put` в корзину, где уже 8 записей, реализация пробует заменить цепочку красно-чёрным деревом. Если у таблицы меньше 64 корзин, сначала происходит расширение. У дерева логарифмическая высота. Для ключей с одинаковым хешем, у которых сравнение не помогает выбрать ветку, поиск может просматривать обе ветки. Гарантии O(log n) для всех ключей у `HashMap` нет. Пороги относятся к реализации OpenJDK 17. [Исходный код OpenJDK 17, методы putVal, treeifyBin и TreeNode.find](https://github.com/openjdk/jdk17u/blob/master/src/java.base/share/classes/java/util/HashMap.java)

</details>

Если заранее известен размер, задай ёмкость (`HashMap.newHashMap(expected)` с JDK 19), чтобы избежать повторных расширений.

| | `HashMap` | `LinkedHashMap` | `TreeMap` | `ConcurrentHashMap` |
|---|---|---|---|---|
| Порядок | Нет | Вставки (или доступа — для LRU) | Сортировка | Нет |
| Поиск по ключу | Обычно O(1) | Обычно O(1) | O(log n) | Обычно O(1) |
| `null`-ключ | Да | Да | С компаратором, который допускает `null` | Нет |
| Потокобезопасность | Нет | Нет | Нет | Да |

Проверено запуском: `HashMap.put(null, 1)` работает; `ConcurrentHashMap.put(null, 1)` и `TreeMap.put(null, 1)` бросают `NullPointerException`.

### Методы Map, которые полезны в задачах

| Метод | Когда использовать |
| --- | --- |
| `containsKey(key)` | Проверить наличие ключа, когда `get(key)` дал `null`. `HashMap` допускает один `null`-ключ и `null`-значения. |
| `entrySet()` | Перебрать сразу ключи и значения. `keySet()`, `values()` и `entrySet()` — представления исходной карты. Например, `keySet().remove(key)` удаляет всю пару. |
| `counts.merge(word, 1, Integer::sum)` | Посчитать частоты. Для нового ключа записывает `1`, для существующего счётчика прибавляет `1`. |
| `groups.computeIfAbsent(city, key -> new ArrayList<>()).add(order)` | Добавить заказ в группу по городу. Для отсутствующего ключа или значения `null` создаётся список. Результат функции `null` оставит ключ без нового значения. |

[API Map](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Map.html) · [API HashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html)

## Queue и Deque. Очередь, стек и приоритет

- **FIFO** — первый поступил, первый ушёл. Например, `ArrayDeque`: добавляй через `offerLast`, забирай через `pollFirst`. Внутри — растущий кольцевой массив: индексы головы и хвоста могут переходить с конца массива в начало. Операции на концах стоят амортизированно O(1). `ArrayDeque` запрещает `null`. [API ArrayDeque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayDeque.html)
- **LIFO** — последний поступил, первый ушёл. Например, стек для отмены действий. `push` и `pop` работают с началом `Deque`. `pop` на пустом стеке бросает `NoSuchElementException`. [API Deque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Deque.html)
- **Приоритет.** `PriorityQueue` хранит двоичную кучу. В голове лежит наименьший элемент по текущему сравнению. `peek()` даёт его за O(1). Вставка и извлечение восстанавливают порядок кучи за O(log n). При росте массива вставка дополнительно копирует элементы. Поиск произвольного элемента O(n). Итератор выдаёт элементы в произвольном порядке, а при одинаковом приоритете очередь выбирает голову произвольно. Чтобы получить максимум первым, используй обратный `Comparator`. [API PriorityQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/PriorityQueue.html)

| Операция `Queue` | Вариант с особым результатом | Вариант с исключением |
| --- | --- | --- |
| Добавить | `offer(e)` возвращает `false`, если нет места | `add(e)` бросает `IllegalStateException`, если нет места |
| Забрать голову | `poll()` возвращает `null` для пустой очереди | `remove()` бросает `NoSuchElementException` |
| Посмотреть голову | `peek()` возвращает `null` для пустой очереди | `element()` бросает `NoSuchElementException` |

[API Queue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Queue.html)

## Копия, представление и изменение при обходе

| Выражение | Что получится |
| --- | --- |
| `new ArrayList<>(source)` | Самостоятельный изменяемый список с теми же ссылками на элементы. |
| `Arrays.asList(array)` | Список фиксированного размера, связанный с массивом. `set` меняет массив, `add` и `remove` запрещены. |
| `source.subList(from, to)` | Представление диапазона `[from, to)`. После структурного изменения исходника в обход представления его поведение не определено. Для отдельного списка используй `new ArrayList<>(source.subList(from, to))`. |
| `Collections.unmodifiableList(source)` | Живое представление. Менять через него нельзя, изменения исходника видны. |
| `List.copyOf(source)` | Снимок состава элементов, который нельзя менять. Элементы `null` запрещены. Реализация может переиспользовать подходящий исходный список. |
| `List.of(a, b)` | Список с заданными элементами без операций изменения. Принимает повторы. `Set.of` отвергает повторы, `Map.of` отвергает повторные ключи. Все три фабрики запрещают `null`. |
| `stream.toList()` | Список без операций изменения. `Collectors.toList()` оставляет изменяемость без гарантии. Если нужен изменяемый список, используй `Collectors.toCollection(ArrayList::new)`. |

Копирование списка поверхностное. Если в нём лежит изменяемый `Order`, и оригинал, и копия ссылаются на тот же `Order`. [API List](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/List.html) · [API Arrays](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Arrays.html) · [API Collections](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collections.html) · [Фабрики Java 17](https://docs.oracle.com/en/java/javase/17/core/creating-immutable-lists-sets-and-maps.html) · [API Stream](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Stream.html) · [API Collectors](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Collectors.html)

Когда меняется структура `ArrayList` или `HashMap` во время обхода, fail-fast итератор может выбросить `ConcurrentModificationException` даже в одном потоке. Он обнаруживает ошибку по возможности. Для удаления используй `Iterator.remove()` после `next()`, если итератор поддерживает удаление. Для фильтрации изменяемого списка можно вызвать `removeIf` отдельно от обхода. [API ArrayList](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayList.html) · [API HashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/HashMap.html)

## Коллекции для нескольких потоков

| Задача | Коллекция | Что сказать на интервью |
| --- | --- | --- |
| Общая карта, например счётчики по ID | `ConcurrentHashMap` | `merge`, `compute` и `putIfAbsent` позволяют обновлять запись атомарно. `get` и `put` по отдельности оставляют окно для гонки. `null` запрещён и для ключей, и для значений. При обходе часть изменений может быть видна, общего снимка нет. |
| Частый обход, редкие правки, например список подписчиков | `CopyOnWriteArrayList` | Изменение содержимого копирует массив. Итератор читает массив, который был при его создании. Частые записи дороги. |
| Передать задачи рабочим потокам и ограничить очередь | `ArrayBlockingQueue` | Ограниченная FIFO-очередь. `put` ждёт место, `take` ждёт элемент. Есть `offer` и `poll` с таймаутом. |
| Общая FIFO-очередь с немедленным результатом операций | `ConcurrentLinkedQueue` | Неблокирующая очередь без заданного лимита. `poll()` сразу вернёт `null`, если очередь пуста. |

`Collections.synchronizedList` синхронизирует отдельные вызовы через обёртку. Для обхода `Iterator` удерживай монитор обёртки на протяжении всего цикла. В `ConcurrentHashMap` атомарность обновления по одному ключу сама по себе не защищает изменения нескольких ключей или содержимое изменяемого значения. Функция для `computeIfAbsent` должна быть короткой, потому что другие обновления могут ждать её завершения. [API Collections](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collections.html) · [API ConcurrentHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) · [API CopyOnWriteArrayList](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CopyOnWriteArrayList.html) · [API ArrayBlockingQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ArrayBlockingQueue.html) · [API ConcurrentLinkedQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentLinkedQueue.html)

## Что добавила Java 21

`SequencedCollection`, `SequencedSet` и `SequencedMap` описывают коллекции и карты с определённым порядком обхода. Например, у `List` есть `getFirst()` и `getLast()`, а у `LinkedHashMap` — `firstEntry()` и `lastEntry()`. `reversed()` возвращает представление для обратного обхода. Операции явной вставки в начало или конец могут быть недоступны, например `TreeSet.addFirst()` бросает `UnsupportedOperationException`, потому что позицию определяет сравнение. Эти интерфейсы появились в Java 21, упражнения в проекте используют Java 17. [Руководство Oracle по Sequenced Collections](https://docs.oracle.com/en/java/javase/21/core/creating-sequenced-collections-sets-and-maps.html)

## Опыты с коллекциями

[Задачи с решениями и тестами](../../../src/main/java/ru/learn/collections/README.md)

### 8.1. ArrayList или LinkedList

[Выбор реализации и стоимость операций](#list-arraylist-или-linkedlist). Для очереди и стека используй [Deque](#queue-и-deque-очередь-стек-и-приоритет).

### 8.2. Неизменяемые коллекции

[Копия и представление](#копия-представление-и-изменение-при-обходе).

<details>
<summary>Результаты запуска</summary>

```text
unmodifiableList view=[a, b] copyOf=[a]     -- после src.add("b")
List.of(1, null)          -> NullPointerException
List.of(1).contains(null) -> NullPointerException   -- даже проверка на null!
Map.of("a", 1, "a", 2)    -> IllegalArgumentException (дубликат ключа)
Stream.toList().add(3)    -> UnsupportedOperationException
```

Неизменяемость не глубокая: элементы-объекты можно менять.

</details>

### 8.3. ConcurrentModificationException

[Как безопасно менять коллекцию при обходе](#копия-представление-и-изменение-при-обходе).

<details>
<summary>Результаты запуска и подвох</summary>

```text
[1, 2, 3, 4]: удалить 2 в for-each        -> ConcurrentModificationException
[1, 2, 3, 4]: removeIf(n -> n % 2 == 0)   -> [1, 3]
[1, 2, 3, 4]: удалить 3 в for-each        -> исключения нет, результат [1, 2, 4]
```

Удаление предпоследнего элемента не бросает исключение: размер стал 3, курсор равен 3, `hasNext()` возвращает `false`, и проверка в `next()` не выполняется. Fail-fast — механизм «лучшей попытки», а не гарантия.

</details>

### 8.4. Sequenced Collections (JDK 21)

Общий интерфейс для коллекций с определённым порядком: `getFirst()`, `getLast()`, `addFirst()`, `removeLast()`, `reversed()`. Для карт — `SequencedMap` с `firstEntry()`, `lastEntry()`.

```text
first=1 last=3 reversed=[3, 2, 1]
firstEntry=a=1 lastEntry=b=2      -- LinkedHashMap
```
