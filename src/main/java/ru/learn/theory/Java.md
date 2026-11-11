# Java и JVM

[Оглавление](README.md)

## На интервью

**HashMap:** найди корзину по хешу, проверь равенство ключей через equals. **PECS:** чтение через extends, запись через super. **Stream:** промежуточные операции ленивые.

**Практика.** Сравни порядок обхода в [LinkedHashMapDemo](../collections/LinkedHashMapDemo.java), разбери границы wildcard в [PecsCopyDemo](../generic/variance/PecsCopyDemo.java) и найди терминальную операцию в [MapTask](../stream/api/MapTask.java). [JVM и GC](JVM.md)

> Конспект-тренажёр для подготовки к собеседованиям.
> Примеры проверены на **OpenJDK 25** (25.0.4); результаты в лабораторных получены запуском. Многопоточность вынесена в Concurrent.md.

## Как пользоваться файлом

Каждая тема устроена одинаково:

1. **Короткий ответ** — то, что нужно сказать устно за 30–60 секунд.
2. **Подробности** — под спойлером `<details>`: механизм, ограничения, примеры.
3. **Уточняющие вопросы** — то, что интервьюер спросит следом.
4. **Практика** — код с реальным выводом или задача.

Отмечайте уверенность прямо в файле:

- `[ ]` — не знаю;
- `[~]` — отвечаю с подсказкой;
- `[x]` — объясняю сам, с примером.

Повторяйте прежде всего то, что отмечено `[ ]` и `[~]`.

---

## Содержание

0. [Быстрое повторение](#0-быстрое-повторение)
1. [JVM: запуск, загрузка классов, JIT](JVM.md#1-jvm-запуск-загрузка-классов-jit)
2. [Области памяти](JVM.md#2-области-памяти)
3. [Сборка мусора](JVM.md#3-сборка-мусора)
4. [Ссылки: strong, soft, weak, phantom](JVM.md#4-ссылки-strong-soft-weak-phantom)
5. [Диагностика и профилирование](JVM.md#5-диагностика-и-профилирование)
6. [equals, hashCode и HashMap](#6-equals-hashcode-и-hashmap)
7. [Строки, обёртки, числа](#7-строки-обёртки-числа)
8. [Коллекции](#8-коллекции)
9. [Generics и PECS](#9-generics-и-pecs)
10. [Исключения](#10-исключения)
11. [Лямбды, Stream API, Optional](#11-лямбды-stream-api-optional)
12. [Records, sealed, pattern matching](#12-records-sealed-pattern-matching)
13. [Что нового по версиям](#13-что-нового-по-версиям)
14. [Пробное собеседование](#14-пробное-собеседование)
15. [Источники](#15-источники)

---

## 0. Быстрое повторение

Ответьте вслух, не открывая разделы. Ответы — по ссылкам.

| Уверенность | Вопрос | Раздел |
|---|---|---|
| `[ ]` | JDK, JRE, JVM — чем отличаются? | [1](JVM.md#1-jvm-запуск-загрузка-классов-jit) |
| `[ ]` | Как работает делегирование загрузчиков классов? | [1.2](JVM.md#12-загрузка-классов) |
| `[ ]` | Что хранится в heap, stack, metaspace? Где статические поля? | [2](JVM.md#2-области-памяти) |
| `[ ]` | Throughput и latency в GC — почему конфликтуют? | [3.1](JVM.md#31-цели-throughput-и-latency) |
| `[ ]` | Какой GC по умолчанию и когда ZGC? | [3.2](JVM.md#32-сборщики) |
| `[ ]` | Чем WeakReference отличается от SoftReference? | [4](JVM.md#4-ссылки-strong-soft-weak-phantom) |
| `[ ]` | Sampling и instrumenting профилирование — разница? | [5.2](JVM.md#52-профилирование) |
| `[ ]` | Контракт equals/hashCode. Что будет, если изменить ключ HashMap? | [6.1](#61-контракт) |
| `[ ]` | Как устроен HashMap? Когда корзина становится деревом? | [6.2](#62-устройство-hashmap) |
| `[ ]` | Почему `Integer 128 == 128` даёт false? | [7.2](#72-обёртки-и-кеш-integer) |
| `[ ]` | Почему деньги не хранят в double? Чем опасен `new BigDecimal(0.1)`? | [7.3](#73-числа-с-плавающей-точкой-и-bigdecimal) |
| `[ ]` | ArrayList или LinkedList? | [8.1](#81-arraylist-или-linkedlist) |
| `[ ]` | `List.of`, `unmodifiableList`, `copyOf` — в чём разница? | [8.2](#82-неизменяемые-коллекции) |
| `[ ]` | Когда бросается ConcurrentModificationException? | [8.3](#83-concurrentmodificationexception) |
| `[ ]` | Что такое стирание типов и PECS? | [9](#9-generics-и-pecs) |
| `[ ]` | Checked и unchecked исключения? Что вернёт finally с return? | [10](#10-исключения) |
| `[ ]` | `orElse` и `orElseGet` — в чём разница? | [11.3](#113-optional) |
| `[ ]` | `Stream.toList()` и `Collectors.toList()` — одно и то же? | [11.2](#112-stream-api) |
| `[ ]` | Record неизменяем? | [12.1](#121-records) |
| `[ ]` | Что нового в Java 21 и 25? | [13](#13-что-нового-по-версиям) |

---

## 6. equals, hashCode и HashMap

### 6.1. Контракт

`[ ]` **Короткий ответ:** `==` для ссылок сравнивает идентичность, `equals` — логическое равенство. `equals` должен быть рефлексивным, симметричным, транзитивным, стабильным и возвращать `false` для `null`. Если объекты равны по `equals`, их `hashCode` обязан совпадать (обратное не нужно — коллизии допустимы). Переопределили `equals` — переопределите `hashCode`, иначе `HashMap` не найдёт равный ключ.

### Лабораторная 1. Изменённый ключ

`[ ]` Предскажите вывод.

<details>
<summary>Код</summary>

```java
static class Key {
    int id;
    Key(int id) { this.id = id; }
    public boolean equals(Object o) { return o instanceof Key k && k.id == id; }
    public int hashCode() { return id; }
}

Map<Key, String> m = new HashMap<>();
Key k = new Key(1);
m.put(k, "v");
k.id = 2;                                  // меняем поле, участвующее в hashCode
System.out.println("get=" + m.get(k) + " containsKey(new Key(1))=" + m.containsKey(new Key(1)) + " size=" + m.size());
```

</details>

<details>
<summary>Результат запуска</summary>

```text
mutated key: get=null containsKey(new Key(1))=false size=1
```

`get(k)` ищет в корзине для хеша 2 — там пусто. `new Key(1)` попадает в правильную корзину, но `equals` сравнивает с объектом, у которого теперь `id = 2`. Запись есть (`size=1`), но недостижима. Вывод: ключи должны быть неизменяемыми — `String`, `record` с неизменяемыми полями.

</details>

### 6.2. Устройство HashMap

`[ ]` **Короткий ответ:** массив корзин. Индекс корзины — `(n - 1) & hash`, где хеш дополнительно перемешан (`h ^ (h >>> 16)`), чтобы старшие биты влияли на младшие. Внутри корзины — список, а при 8+ элементах и таблице от 64 корзин — красно-чёрное дерево (O(log n) вместо O(n) при плохих хешах). При заполнении больше `loadFactor` (0,75) таблица удваивается. Средний поиск — O(1).

- Допускает один `null`-ключ и `null`-значения. Не потокобезопасен.
- Порядок обхода не гарантирован; нужен порядок вставки — `LinkedHashMap`, сортировка — `TreeMap`.
- Если заранее известен размер, задайте ёмкость (`HashMap.newHashMap(expected)` с JDK 19), чтобы избежать повторных расширений.

| | `HashMap` | `LinkedHashMap` | `TreeMap` | `ConcurrentHashMap` |
|---|---|---|---|---|
| Порядок | Нет | Вставки (или доступа — для LRU) | Сортировка | Нет |
| Сложность | O(1) | O(1) | O(log n) | O(1) |
| `null`-ключ | Да | Да | Нет (NPE) | Нет (NPE) |
| Потокобезопасность | Нет | Нет | Нет | Да |

Проверено запуском: `HashMap.put(null, 1)` работает; `ConcurrentHashMap.put(null, 1)` и `TreeMap.put(null, 1)` бросают `NullPointerException`.

---

## 7. Строки, обёртки, числа

### 7.1. Строки

`[ ]` **Короткий ответ:** `String` неизменяема — поэтому её безопасно разделять между потоками, кешировать хеш и использовать как ключ. Литералы хранятся в пуле строк: одинаковые литералы — один объект. `new String("hi")` создаёт новый объект. Сравнивать строки — только через `equals`.

```text
"hi" == new String("hi"): false
"hi" == new String("hi").intern(): true
```

- Конкатенация в цикле создаёт много промежуточных объектов — используйте `StringBuilder`. Одиночное `a + b + c` компилятор оптимизирует сам.
- С JDK 9 строки из символов Latin-1 хранятся по байту на символ (compact strings).
- Коллизии хешей строк легко подобрать: `"Aa".hashCode()` и `"BB".hashCode()` оба равны 2112.

### 7.2. Обёртки и кеш Integer

`[ ]` **Короткий ответ:** автоупаковка `Integer i = 127` вызывает `Integer.valueOf`, который кеширует значения от -128 до 127. Поэтому `==` для 127 даёт `true` (один объект), а для 128 — `false` (два разных). Обёртки сравнивают через `equals`.

```text
Integer 127==127 true, 128==128 false, equals true
```

Другие ловушки: распаковка `null` в `int` даёт `NullPointerException`; `list.remove(1)` на `List<Integer>` удаляет **по индексу**, а `list.remove(Integer.valueOf(1))` — **по значению**:

```text
[10, 20, 30].remove(1)                -> [10, 30]
[10, 30].remove(Integer.valueOf(10))  -> [30]
```

### 7.3. Числа с плавающей точкой и BigDecimal

`[ ]` **Короткий ответ:** `double` хранит двоичную дробь, а 0,1 в двоичной системе — бесконечная дробь, поэтому `0.1 + 0.2 != 0.3`. Деньги хранят в `BigDecimal` (создавая из строки) или в целых минимальных единицах (копейках) в `long`.

```text
0.1 + 0.2                    = 0.30000000000000004
new BigDecimal("0.1").add(new BigDecimal("0.2")) = 0.3
new BigDecimal(0.1)          = 0.1000000000000000055511151231257827021181583404541015625
```

- `new BigDecimal(0.1)` переносит ошибку `double`. Правильно — `new BigDecimal("0.1")` или `BigDecimal.valueOf(0.1)`.
- `BigDecimal.equals` учитывает масштаб: `2.0` не равно `2.00`. Сравнивать значения — `compareTo`.
- Деление без указания округления может бросить `ArithmeticException` (1/3).

---

## 8. Коллекции

### 8.1. ArrayList или LinkedList

`[ ]` **Короткий ответ:** почти всегда `ArrayList`: доступ по индексу O(1), добавление в конец — амортизированное O(1), элементы лежат подряд в памяти, и обход дружит с кешем процессора. У `LinkedList` доступ по индексу O(n), а вставка за O(1) только если позиция уже найдена. Каждый узел — отдельный объект с двумя ссылками: больше памяти, промахи кеша. Для очереди и стека — `ArrayDeque`.

| Операция | `ArrayList` | `LinkedList` |
|---|---|---|
| `get(i)` | O(1) | O(n) |
| Добавить в конец | O(1) амортизированно | O(1) |
| Вставить в середину | O(n) (сдвиг через `System.arraycopy`, очень быстрый) | O(n) поиск + O(1) вставка |
| Память на элемент | Ссылка (4–8 байт) | Узел: заголовок + 3 ссылки (~24–40 байт) |

### 8.2. Неизменяемые коллекции

`[ ]` **Короткий ответ:** `List.of(...)` — неизменяемая коллекция, не принимает `null`. `Collections.unmodifiableList(list)` — только представление: изменения исходного списка видны через него. `List.copyOf(list)` — независимый неизменяемый снимок.

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

`[ ]` **Короткий ответ:** итераторы `ArrayList`, `HashMap` и других не-потокобезопасных коллекций — fail-fast: если коллекцию изменили не через итератор во время обхода, следующий `next()` бросит `ConcurrentModificationException`. Это происходит и в одном потоке. Удалять при обходе — `iterator.remove()` или `removeIf`.

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

---

## 9. Generics и PECS

`[ ]` **Короткий ответ.** `List<String>` и `List<Integer>` различаются при компиляции и стираются до `List`. Поэтому нельзя создать `new T[]`, проверить произвольный `Object` через `instanceof List<String>` или перегрузить метод по `List<String>` и `List<Integer>`. Из-за инвариантности `List<Integer>` нельзя присвоить переменной `List<Number>`. Wildcard даёт гибкость по правилу PECS (Producer Extends, Consumer Super).

```java
static <T> void copy(List<? extends T> source, List<? super T> target) {
    for (T value : source) target.add(value);
}
```

- Из `List<? extends Number>` можно **читать** `Number`. Компилятор запрещает добавить произвольное ненулевое число, поскольку список может принимать только `Double`.
- В `List<? super Integer>` можно **добавлять** `Integer`, а читать — только как `Object`.

**Уточнение интервьюера:** «Почему `List<Integer>` нельзя передать как `List<Number>`?» — иначе метод добавил бы туда `Double`, сломав исходный список. Массивы ковариантны (`Integer[]` — это `Number[]`), и такая ошибка ловится только в рантайме: `ArrayStoreException`.

[Все запускаемые примеры по дженерикам](../generic/README.md)

---

## 10. Исключения

`[ ]` **Короткий ответ:** `Throwable` делится на `Error` (проблемы JVM: `OutOfMemoryError`, `StackOverflowError` — не перехватывают) и `Exception`. Checked-исключения (наследники `Exception`, кроме `RuntimeException`) компилятор требует обработать или объявить. Unchecked (`RuntimeException`) — ошибки программирования и ситуации, которые вызывающий не может исправить.

- **try-with-resources** закрывает ресурсы (`AutoCloseable`) в обратном порядке, даже при исключении. Исключение из `close()` не теряет основное: оно добавляется в `getSuppressed()`. С JDK 9 можно использовать уже объявленную переменную: `try (resource) { ... }`.
- **finally** выполняется всегда, кроме `System.exit` и смерти процесса. `return` в `finally` перекрывает и `return` из `try`, и выброшенное исключение — исключение теряется молча.
- Не перехватывать `Exception` без необходимости, не глотать исключения, при оборачивании передавать причину (`new ServiceException("...", e)`).
- Spring по умолчанию откатывает транзакцию только на unchecked-исключениях. См. Spring.md.

**Helpful NullPointerException** (JDK 14, по умолчанию с JDK 15) — сообщение указывает, что именно было `null`:

```text
Cannot invoke "Jv$Point.x()" because "<local21>" is null
```

Имя локальной переменной видно, если класс скомпилирован с `-g`; иначе — номер слота, как здесь.

---

## 11. Лямбды, Stream API, Optional

### 11.1. Функциональные интерфейсы

| Интерфейс | Сигнатура | Пример |
|---|---|---|
| `Predicate<T>` | `T -> boolean` | `s -> s.isBlank()` |
| `Function<T, R>` | `T -> R` | `String::length` |
| `Consumer<T>` | `T -> void` | `System.out::println` |
| `Supplier<T>` | `() -> T` | `ArrayList::new` |
| `UnaryOperator<T>` | `T -> T` | `String::trim` |
| `BinaryOperator<T>` | `(T, T) -> T` | `Integer::sum` |
| `BiFunction<T, U, R>` | `(T, U) -> R` | `Map::get` |

Лямбда может захватывать только effectively final переменные: значение копируется в момент создания, и разрешение менять его создало бы гонки.

### 11.2. Stream API

`[ ]` **Короткий ответ:** стрим — ленивый конвейер: промежуточные операции (`filter`, `map`, `sorted`) ничего не делают до терминальной (`toList`, `collect`, `count`). Стрим одноразовый. `parallelStream` использует общий `ForkJoinPool` — выигрыш только для больших CPU-задач без блокировок и общего изменяемого состояния.

**`Stream.toList()` и `Collectors.toList()`:** `Stream.toList()` (JDK 16) возвращает неизменяемый список и допускает `null`. `Collectors.toList()` по контракту не гарантирует ни тип, ни изменяемость (на практике — `ArrayList`). Нужен изменяемый — явно: `Collectors.toCollection(ArrayList::new)`.

```text
Stream.toList().add -> UnsupportedOperationException
Collectors.toList() class=ArrayList
Stream.toList allows null: [1, null]
```

<details>
<summary>Полезные коллекторы и Gatherers (JDK 24)</summary>

```java
Map<Status, List<Order>> byStatus = orders.stream().collect(Collectors.groupingBy(Order::status));
Map<Status, Long> counts = orders.stream().collect(Collectors.groupingBy(Order::status, Collectors.counting()));
Map<Boolean, List<Order>> split = orders.stream().collect(Collectors.partitioningBy(o -> o.total() > 1000));
Map<Long, Order> byId = orders.stream().collect(Collectors.toMap(Order::id, o -> o));   // дубликат ключа -> IllegalStateException
var minMax = nums.stream().collect(Collectors.teeing(
        Collectors.minBy(Integer::compare), Collectors.maxBy(Integer::compare), Pair::new)); // JDK 12
```

**Stream Gatherers** (JEP 485, финально в JDK 24) — пользовательские промежуточные операции ([OpenJDK: JDK 24](https://openjdk.org/projects/jdk/24/)):

```java
Stream.of(1, 2, 3, 4, 5, 6, 7).gather(Gatherers.windowFixed(3)).toList();  // [[1, 2, 3], [4, 5, 6], [7]]
Stream.of(1, 2, 3, 4).gather(Gatherers.windowSliding(2)).toList();          // [[1, 2], [2, 3], [3, 4]]
```

</details>

### 11.3. Optional

`[ ]` **Короткий ответ:** `Optional` — возвращаемое значение «может отсутствовать». Аргумент `orElse(x)` вычисляется всегда, даже если значение есть; `orElseGet(() -> x)` — только при пустом `Optional`. Если запасное значение дорогое (запрос в БД), нужен `orElseGet`.

```text
orElse:
  expensive() called          -- вызван, хотя Optional не пуст
orElseGet:
                              -- не вызван
```

Не использовать `Optional` для полей, параметров и коллекций (пустая коллекция лучше). `get()` без проверки — тот же NPE; предпочитайте `orElseThrow()`, `map`, `ifPresentOrElse`.

---

## 12. Records, sealed, pattern matching

### 12.1. Records

`[ ]` **Короткий ответ:** `record` (JDK 16) — компактный класс-носитель данных: поля `private final`, конструктор, методы доступа, `equals`, `hashCode`, `toString` по компонентам. Неизменяемость не глубокая: если компонент — изменяемый список, его можно менять. Нужна защитная копия в компактном конструкторе.

```java
record Point(int x, List<Integer> tags) {}
Point p = new Point(1, new ArrayList<>(List.of(1)));
p.tags().add(2);
System.out.println(p);    // Point[x=1, tags=[1, 2]]

// Защитная копия
record SafePoint(int x, List<Integer> tags) {
    SafePoint { tags = List.copyOf(tags); }
}
```

Records не могут наследовать классы (уже наследуют `Record`), но могут реализовывать интерфейсы. Хороши для DTO, ключей карт, значений-объектов. Как JPA-сущности не подходят (нужны изменяемость и конструктор без аргументов).

### 12.2. Sealed и pattern matching

`[ ]` **Короткий ответ:** `sealed`-тип (JDK 17) явно перечисляет допустимых наследников. Вместе с pattern matching для `switch` (JDK 21) компилятор проверяет полноту: если обработаны все подтипы, `default` не нужен, а при добавлении нового подтипа код перестанет компилироваться — это защищает от забытых веток.

```java
sealed interface Shape permits Circle, Square {}
record Circle(double r) implements Shape {}
record Square(double side) implements Shape {}

static String describe(Shape s) {
    return switch (s) {                       // без default: компилятор знает все варианты
        case Circle c -> "circle " + c.r();
        case Square sq -> "square " + sq.side();
    };
}
```

```text
circle 1.0 square 2.0
```

Охранные условия и деконструкция: `case Integer i when i > 10 -> ...`, `case Point(int x, var tags) -> ...`.

---

## 13. Что нового по версиям

`[ ]` **Короткий ответ:** LTS-версии: 8, 11, 17, 21, 25. Ключевое: 8 — лямбды и стримы; 11 — `var` в лямбдах, HTTP Client, новые методы `String`; 17 — records, sealed, текстовые блоки; 21 — виртуальные потоки, pattern matching для `switch`, Sequenced Collections; 25 — Scoped Values, компактные исходные файлы, гибкие конструкторы, импорт модулей.

| Версия | Что важно для собеседования |
|---|---|
| 9 | Модули, `List.of`/`Set.of`/`Map.of`, приватные методы в интерфейсах, `try (resource)` с уже объявленной переменной |
| 10 | `var` для локальных переменных |
| 11 (LTS) | `String`: `isBlank`, `strip`, `lines`, `repeat`; `HttpClient`; запуск `java File.java` без компиляции |
| 12 | `Collectors.teeing`, `String.transform` |
| 14 | `switch`-выражения со стрелками (финально); helpful NPE (JEP 358) |
| 15 | Текстовые блоки `"""`; helpful NPE включены по умолчанию; ZGC готов для продакшена |
| 16 | `record`, pattern matching для `instanceof`, `Stream.toList()` |
| 17 (LTS) | `sealed`-классы |
| 21 (LTS) | Виртуальные потоки, pattern matching для `switch`, record patterns, Sequenced Collections, ZGC с поколениями |
| 22 | Безымянные переменные `_`: `catch (NumberFormatException _)` |
| 24 | Stream Gatherers, виртуальные потоки без пиннинга на `synchronized` (JEP 491), удалён ZGC без поколений, Security Manager отключён навсегда |
| 25 (LTS) | Scoped Values, компактные исходные файлы и `main` без `static`, импорт модулей, гибкие тела конструкторов, компактные заголовки объектов, Shenandoah с поколениями |

Источники по 24 и 25 — списки JEP на [OpenJDK: JDK 24](https://openjdk.org/projects/jdk/24/) и [OpenJDK: JDK 25](https://openjdk.org/projects/jdk/25/). JDK 25 вышел 16 сентября 2025 года ([OpenJDK: JDK 25](https://openjdk.org/projects/jdk/25/)).

<details>
<summary>Новое в JDK 25: код и результат запуска</summary>

**Компактный исходный файл (JEP 512)** — без класса и `static`:

```java
void main() {
    IO.println("compact source file: " + List.of(1, 2, 3));
}
```

```text
$ java Hello.java
compact source file: [1, 2, 3]
```

**Импорт модуля (JEP 511):** `import module java.base;` импортирует все экспортируемые пакеты модуля.

**Гибкие тела конструкторов (JEP 513)** — код до `super(...)`: проверка аргументов и присваивание полей.

```java
static class Base {
    Base(int v) { System.out.println("Base sees " + describe()); }
    String describe() { return "base"; }
}
static class Positive extends Base {
    final int value;
    Positive(int v) {
        if (v <= 0) throw new IllegalArgumentException("v must be > 0");
        this.value = v;        // поле присвоено до вызова конструктора родителя
        super(v);
    }
    @Override String describe() { return "positive " + value; }
}
```

```text
Base sees positive 5
validated before super: v must be > 0
```

Раньше переопределённый метод, вызванный из конструктора родителя, видел `value = 0`. Теперь поле можно инициализировать заранее.

</details>

---

## 14. Пробное собеседование

Сначала ответьте вслух, потом откройте ответ.

<details>
<summary>1. Сервис упал с OutOfMemoryError: Java heap space. Ваши действия?</summary>

Заранее включить `-XX:+HeapDumpOnOutOfMemoryError` и `-XX:HeapDumpPath`. Открыть дамп в Eclipse MAT: Dominator tree и Leak Suspects — кто удерживает больше всего памяти. Частые причины: неограниченный кеш (`HashMap` в статическом поле), накопление в `ThreadLocal`, слушатели, которых не удаляют, загрузка большой выборки из БД целиком. Затем — GC-логи: растёт ли старое поколение после каждой полной сборки (утечка) или куча просто мала для нагрузки.

</details>

<details>
<summary>2. Что выведет код и почему?</summary>

```java
List<Integer> list = new ArrayList<>(List.of(10, 20, 30));
list.remove(1);
System.out.println(list);
```

`[10, 30]` — вызывается `remove(int index)`, а не `remove(Object)`: при выборе перегрузки точное совпадение с примитивом предпочтительнее автоупаковки. Для удаления по значению — `list.remove(Integer.valueOf(20))`.

</details>

<details>
<summary>3. Можно ли использовать изменяемый объект как ключ HashMap?</summary>

Можно, пока поля, участвующие в `hashCode` и `equals`, не меняются. Если поменять — запись останется в старой корзине и станет недостижимой ([лабораторная 1](#лабораторная-1-изменённый-ключ)). Надёжнее — неизменяемые ключи: `String`, `record`, `Long`.

</details>

<details>
<summary>4. Чем abstract class отличается от interface в современной Java?</summary>

Интерфейсы с JDK 8 могут иметь `default`- и `static`-методы, с JDK 9 — приватные, но не состояние экземпляра (только константы) и не конструкторы. Класс может реализовать много интерфейсов, но наследовать один класс. Абстрактный класс — когда есть общее состояние и защищённые методы-помощники; интерфейс — для контракта и множественной «роли».

</details>

<details>
<summary>5. Почему метод работает медленно в первые секунды после старта?</summary>

Интерпретация и JIT: сначала байт-код интерпретируется, горячие методы компилируются C1, затем C2 с профилем. Плюс загрузка классов и инициализация. Решения: прогрев перед включением в балансировку (readiness), AppCDS и AOT-кеш (JEP 483, 514, 515), достаточный CPU при старте. Для замеров — JMH с итерациями прогрева.

</details>

<details>
<summary>6. Что не так в этом коде?</summary>

```java
BigDecimal price = new BigDecimal(19.99);
if (price.equals(new BigDecimal("19.990"))) { ... }
```

Две ошибки: конструктор из `double` переносит двоичную погрешность (`19.989999...`); `equals` учитывает масштаб, и `19.99` не равно `19.990`. Правильно: `new BigDecimal("19.99")` и `compareTo(...) == 0`.

</details>

---

## 15. Источники

- [OpenJDK: JDK 25 — список JEP и дата выхода](https://openjdk.org/projects/jdk/25/)
- [OpenJDK: JDK 24 — список JEP](https://openjdk.org/projects/jdk/24/)
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)
- [Oracle: ArrayDeque (Java SE 25)](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ArrayDeque.html)
- [Inside Java: New in JDK 25](https://inside.java/2025/10/17/new-in-jdk-25-2-mins/)
- [Proselyte: нагрузочное тестирование и профилирование JVM](https://proselyte.net/load-testing-and-jvm-profiling/#java-app-profiling)
