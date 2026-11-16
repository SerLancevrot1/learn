# Коллекции Java

[Теория и таблица выбора](../../../../../../docs/theory/java/collections.md) · [Банк вопросов по коллекциям и Stream API](../../../../../../docs/interview/advanced/collections.md)

## Как выбрать коллекцию

| Что нужно | С чего начать | Пример |
| --- | --- | --- |
| Хранить последовательность с повторами и читать по индексу | `ArrayList` | [Маршрут и редактирование через ListIterator](ListDemo.java) |
| Проверять уникальность | `HashSet`. Для порядка вставки `LinkedHashSet`, для диапазонов `TreeSet` | [Номера заказов и EnumSet](SetDemo.java) |
| Искать или группировать по ключу | `HashMap`. Для порядка вставки `LinkedHashMap`, для сортировки и диапазонов `TreeMap` | [Группы заказов и EnumMap](MapDemo.java) · [Два порядка обхода карты](LinkedHashMapDemo.java) |
| Обслуживать по порядку поступления, с обоих концов или в порядке отмены | `ArrayDeque` | [Заявки FIFO и дека](QueueDemo.java) · [Стек отмены](StackDemo.java) |
| Первой брать самую срочную задачу | `PriorityQueue` | [Срочные заявки с одинаковым приоритетом](QueueDemo.java) |
| Передавать задачи между потоками | `ArrayBlockingQueue` для ограниченного буфера. `ConcurrentLinkedQueue` для FIFO без ожидания данных | [Очереди и таблица реализаций](queues.md) |
| Разделять счётчики, подписчиков или отсортированные ключи между потоками | `ConcurrentHashMap`, `CopyOnWriteArrayList` или `ConcurrentSkipListMap` в зависимости от контракта | [Конкурентные коллекции и атомарные операции](concurrent.md) |
| Передать наружу доступ на чтение или сохранить состав коллекции | `Collections.unmodifiableList` для живого представления, `List.copyOf` для самостоятельного состава | [Корзина, выписка и представление карты](CollectionViewsDemo.java) |

`List`, `Set` и `Queue` расширяют `Collection`, а `Deque` расширяет `Queue`. `Map` образует отдельную ветку. `SortedSet` и `SortedMap` добавляют порядок по сравнению, `NavigableSet` и `NavigableMap` дают поиск соседей и представления диапазонов. `BlockingQueue` добавляет ожидание, `BlockingDeque` переносит его на оба конца, `TransferQueue` даёт дождаться получения элемента. [Иерархия Java Collections Framework](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/package-summary.html) · [java.util.concurrent](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/package-summary.html)

### Контракты, которые меняют выбор

- Для хеш-коллекций `equals` и `hashCode` должны быть согласованы. Поля, по которым ключ сравнивается и хешируется, должны оставаться стабильными. Для `TreeSet` и `TreeMap` нулевой результат сравнения означает тот же элемент или ключ. [Контракты Set, Map и HashMap](../../../../../../docs/theory/java/collections.md#set-и-map-какой-порядок-нужен)
- `subList`, `keySet` и `subMap` дают связанные представления. `List.copyOf`, `Set.copyOf` и `Map.copyOf` фиксируют состав и запрещают изменения через результат, сохраняя ссылки на сами элементы. Фабрики `List.of`, `Set.of`, `Map.of` запрещают `null`. `Set.of` отвергает дубликаты, `Map.of` отвергает повторяющиеся ключи. [Копии, представления и итераторы](../../../../../../docs/theory/java/collections.md#копия-представление-и-изменение-при-обходе)
- `Collections.sort`, `reverse`, `shuffle` и `binarySearch` работают с коллекциями через статические методы. Перед `binarySearch` отсортируй список по тому же сравнению, а для повторных поисков по индексу используй список с быстрым произвольным доступом. [API Collections](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collections.html)
- `WeakHashMap` хранит ключи через слабые ссылки. Запись может исчезнуть после сборки ключа, если он больше недостижим вне слабых ссылок. `IdentityHashMap` сравнивает ссылки по `==`, что полезно при обходе графа объектов. `Hashtable`, `Vector` и `Stack` встречаются в старых API. [WeakHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/WeakHashMap.html) · [IdentityHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/IdentityHashMap.html) · [VectorDemo](VectorDemo.java)
- Примеры компилируются для Java 17. С Java 21 последовательный порядок выражают `SequencedCollection`, `SequencedSet` и `SequencedMap`. Эти интерфейсы дают доступ к обоим концам и обратное представление `reversed()`. [Sequenced Collections в Java 21](https://docs.oracle.com/en/java/javase/21/core/creating-sequenced-collections-sets-and-maps.html)

## Запуск

Из корня проекта с JDK 17 или новее. База данных и Docker для этих тестов не нужны.

```powershell
.\mvnw.cmd "-Dtest=CollectionsExercisesTest,CollectionsContractsTest" test
java -cp target/classes ru.learn.collections.QueueDemo
java -cp target/classes ru.learn.collections.BlockingQueuesDemo
java -cp target/classes ru.learn.collections.ConcurrentCollectionsDemo
```

В IntelliJ IDEA запускай `main` из любого класса `*Demo` или отдельно [CollectionsExercisesTest](../../../../../../src/test/java/ru/learn/collections/CollectionsExercisesTest.java) и [CollectionsContractsTest](../../../../../../src/test/java/ru/learn/collections/CollectionsContractsTest.java).

## Задачи для live coding

Для каждой задачи сначала запиши ожидаемый результат и выбери коллекцию. Затем сравни своё решение с кодом и запусти тесты. Объясни, какая операция определяет сложность, какой нужен порядок и сколько памяти занимает решение.

### 1. Убрать повторы, сохранив первое появление

Из `["b", "a", "b", "c", "a"]` получи `["b", "a", "c"]`. Верни новый список. Что случится с порядком, если взять обычный `HashSet`?

<details>
<summary>Подсказка и разбор</summary>

Построй `LinkedHashSet` из входа, а затем `ArrayList` из множества. Множество отвечает за уникальность и порядок первой вставки, список даёт доступ к результату по индексу. Ожидаемое время O(n) при хорошем распределении хешей, дополнительная память O(u), где `u` — число уникальных элементов. `HashSet` не гарантирует порядок обхода. [Решение distinctInOrder](CollectionsExercises.java)

</details>

### 2. Посчитать частоты

Для `["b", "a", "b", "c"]` верни `{b=2, a=1, c=1}`. Порядок ключей соответствует первому появлению. Как проверить этот порядок в тесте?

<details>
<summary>Подсказка и разбор</summary>

Используй `LinkedHashMap` и `merge(word, 1, Integer::sum)`. Для пустого списка карта тоже пуста. `Map.equals` сравнивает пары, поэтому порядок в тесте отдельно проверяет `new ArrayList<>(result.keySet())`. Ожидаемое время O(n), память O(u). Если порядок не нужен, хватит `HashMap`. [Решение frequencies](CollectionsExercises.java)

</details>

### 3. Проверить вложенность скобок

Строка `{x([1])}` корректна, `([)]` некорректна. Проверь `()`, `[]`, `{}`. Остальные символы пропускай. Проверь отдельно пустую строку, лишнее закрытие и незакрытую скобку.

<details>
<summary>Подсказка и разбор</summary>

В `ArrayDeque` храни закрывающие скобки, которые ещё нужны. Для `[` положи `]` на вершину. При закрытии сравни символ с вершиной. Пустой стек при закрытии означает ошибку. В конце стек должен быть пуст. Проход стоит O(n) по времени и до O(n) по памяти. [Решение hasBalancedBrackets](CollectionsExercises.java)

</details>

### 4. Найти k наибольших чисел

Для `[-2, 7, 1, 7, 3, -5]` и `k = 3` верни `[7, 7, 3]`. Повторы сохраняются. При `k = 0` верни пустой список, при `k` больше размера верни все числа по убыванию. Отрицательный `k` считается ошибкой. Входные числа заданы без `null`.

<details>
<summary>Подсказка и разбор</summary>

Держи в `PriorityQueue` максимум `k` элементов. В голове лежит минимальное из выбранных чисел. Когда очередь заполнена, вытесняй голову только числом больше неё. Для сортировки ответа извлеки числа через `poll()` и разверни список. Дополнительная память O(m), где `m = min(k, n)`. При `m >= 2` время O(n log m) с учётом извлечения ответа. При `m = 1` достаточно линейного прохода, при `k = 0` вход обходить не нужно. [Решение topKLargest](CollectionsExercises.java)

</details>

### 5. Реализовать LRU-кеш

Кеш вмещает две записи. Выполни `put(a, 1)`, `put(b, 2)`, `get(a)`, `put(c, 3)`. Какой ключ пропадёт? Реализуй `get` и `put` для любого положительного лимита. `null` от `get` означает промах. Запрети `null` у ключей и значений.

<details>
<summary>Подсказка и разбор</summary>

У `LinkedHashMap` с `accessOrder = true` чтение `a` даёт порядок `[b, a]`. После добавления `c` порядок станет `[b, a, c]`. Удали первую запись `b`. Обновление существующего ключа тоже учитывается как обращение. Типичное время `get` и `put` — O(1) при хорошем распределении хешей, память — O(лимит). Пример рассчитан на один поток. [Решение LruCache](LruCache.java)

</details>

### 6. Найти кратчайший путь в графе без весов

С Вокзала можно доехать до Музея или Парка. Из Музея есть путь в Школу, оттуда в Аэропорт. Из Парка есть прямой маршрут в Аэропорт. Найди минимальное число переездов из Вокзала в Аэропорт. Рёбра ориентированные. Отсутствие маршрута обозначь `-1`, путь из вершины в саму себя имеет длину `0`. Входные вершины и списки соседей заданы без `null`, отсутствующий ключ означает отсутствие исходящих рёбер.

<details>
<summary>Подсказка и разбор</summary>

`ArrayDeque` хранит вершины в FIFO-порядке. Первая встреча вершины при таком обходе даёт её минимальное расстояние. Записывай расстояние при постановке в очередь, чтобы циклы и несколько путей к одной вершине не добавляли её повторно. В примере ответ `2`. Для обхода достижимой части графа время O(V + E), память O(V). [Решение shortestDistance](CollectionsExercises.java)

</details>

## Предскажи результат и проверь

Предскажи результат для каждого случая. Затем открой [CollectionsContractsTest](../../../../../../src/test/java/ru/learn/collections/CollectionsContractsTest.java) и сверь ответ с проверками и комментариями.

| Что происходит | Что объяснить вслух |
| --- | --- |
| У двух ключей `HashMap` одинаковый `hashCode()` | Почему сохраняются обе пары и как карта найдёт нужную новым объектом с таким же ID. |
| `TreeSet` сравнивает строки только по длине | Сколько останется элементов после добавления `"cat"` и `"dog"`. Что поменяет `thenComparing`. |
| Из `keySet()` удаляют `"known"`, которому в карте соответствует `null` | Как отличить эту запись от отсутствующего ключа и что произойдёт с картой после удаления из представления. |
| Список создают через `Arrays.asList(array)` | Что произойдёт с массивом после вызова `set`, и почему вызов `add` выбросит исключение. |
| Из списка сделали `unmodifiableList` и `List.copyOf`, затем вызвали `source.add("b")` | Какое изменение увидит каждый список и что произойдёт при попытке `set` через каждый результат. |
| Итератор удаляет два чётных числа подряд | Почему после удаления первого числа следующий элемент не пропускается. |
| В `CopyOnWriteArrayList` добавили подписчика после создания итератора | Кто получит уведомление в текущем обходе и кто попадёт в следующий. |

Для очередей предскажи, примется ли третья задача в буфер на две записи и почему `DelayQueue.poll()` может вернуть `null` при `size() == 1`. Затем запусти [BlockingQueuesDemo](BlockingQueuesDemo.java) и [PriorityQueuesDemo](PriorityQueuesDemo.java). В [ConcurrentQueuesDemo](ConcurrentQueuesDemo.java) сравни передачу без буфера с постановкой задания, пока получателя нет.

## Мини-собеседование на 20 минут

1. **5 минут.** Сравни `ArrayList`, `LinkedList` и `ArrayDeque`. Уточнение: сколько стоит пройти `LinkedList` через `get(i)`?
2. **5 минут.** Объясни `HashMap.put` и `HashMap.get`. Уточнение: что случится, если хеши двух ключей совпадают, а `equals` возвращает `false`?
3. **5 минут.** Разбери ограниченный буфер заданий. Сравни `offer`, `put` и `offer` с таймаутом. Объясни, почему проверка `isEmpty()` не гарантирует результат следующего `poll`.
4. **5 минут.** Покажи, как два потока могут потерять обновление в последовательности `get` → `put`. Выбери метод `ConcurrentHashMap` для счётчика и сравни с требованиями к обходу списка подписчиков.

Дополнительные задачи на быстрый поиск и скользящее окно есть в [TwoSums](../leet/code/easy/TwoSums.java) и [LongestSubstringWithoutRepeatingCharacters](../leet/code/easy/LongestSubstringWithoutRepeatingCharacters.java).
