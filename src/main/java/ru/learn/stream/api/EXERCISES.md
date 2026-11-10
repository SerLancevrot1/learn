# 40 задач по Java Stream API

[Оглавление раздела](README.md)

Сначала реши задачу по условию и ожидаемому результату. Затем раскрой решение и сравни шаги. Каждый фрагмент можно вставить в `main` на Java 17 и вывести результат через `System.out.println(result)`. Для задачи 38 подготовь файл и добавь к `main` объявление `throws IOException`. Для всех задач достаточно следующих импортов.

```java
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
```

В примерах с `Map` сравнивай пары ключ–значение. Порядок ключей важен только там, где это явно сказано.

## 01–10. Основы

### 01. Оставить чётные числа

Из `[1, 2, 3, 4, 5, 6]` получи `[2, 4, 6]`.

<details>
<summary>Решение</summary>

```java
var result = List.of(1, 2, 3, 4, 5, 6).stream()
        .filter(n -> n % 2 == 0)
        .toList();
```

`filter` оставляет элементы, для которых условие истинно. `toList` собирает их в список в исходном порядке.

</details>

### 02. Заменить слова их длинами

Из `['кот', 'слон', 'лиса']` получи `[3, 4, 4]`.

<details>
<summary>Решение</summary>

```java
var result = List.of("кот", "слон", "лиса").stream()
        .map(String::length)
        .toList();
```

`map` заменяет каждое слово одним числом. Повторяющиеся длины остаются в списке.

</details>

### 03. Посчитать непустые строки

Среди `['Java', '', '  ', 'SQL']` посчитай строки, в которых есть непробельный символ. Ожидается `2`.

<details>
<summary>Решение</summary>

```java
var result = List.of("Java", "", "  ", "SQL").stream()
        .filter(text -> !text.isBlank())
        .count();
```

`isBlank` распознаёт пустую строку и строку из одних пробельных символов. `count` считает то, что прошло фильтр, и возвращает `long`.

</details>

### 04. Сложить суммы без переполнения `int`

Сложи `[1_500_000_000, 1_500_000_000]`. Ожидается `3_000_000_000L`.

<details>
<summary>Решение</summary>

```java
var result = List.of(1_500_000_000, 1_500_000_000).stream()
        .mapToLong(Integer::longValue)
        .sum();
```

`mapToLong` переводит числа в `long` до сложения. Сумма помещается в `long`, хотя диапазона `int` для неё не хватает.

</details>

### 05. Найти первое отрицательное число

Из `[3, 0, -7, -1]` верни `-7`. Если отрицательных чисел нет, верни `0`.

<details>
<summary>Решение</summary>

```java
var result = List.of(3, 0, -7, -1).stream()
        .filter(n -> n < 0)
        .findFirst()
        .orElse(0);
```

`findFirst` берёт первый элемент, прошедший фильтр, и оборачивает его в `Optional`. При пустом результате `orElse` подставляет `0`.

</details>

### 06. Отсортировать по убыванию

Преобразуй `[5, 1, 3, 5]` в `[5, 5, 3, 1]`.

<details>
<summary>Решение</summary>

```java
var result = List.of(5, 1, 3, 5).stream()
        .sorted(Comparator.reverseOrder())
        .toList();
```

`reverseOrder` разворачивает естественный порядок чисел. `sorted` меняет порядок и сохраняет оба значения `5`.

</details>

### 07. Убрать повторы, сохранив порядок

Из `['b', 'a', 'b', 'c', 'a']` получи `['b', 'a', 'c']`.

<details>
<summary>Решение</summary>

```java
var result = List.of("b", "a", "b", "c", "a").stream()
        .distinct()
        .toList();
```

`distinct` определяет равенство по `equals`. Для потока из списка он сохраняет первое вхождение каждого значения.

</details>

### 08. Взять вторую страницу

Из `['a', 'b', 'c', 'd', 'e']` выбери вторую страницу при размере страницы `2`. Ожидается `['c', 'd']`.

<details>
<summary>Решение</summary>

```java
var result = List.of("a", "b", "c", "d", "e").stream()
        .skip(2)
        .limit(2)
        .toList();
```

`skip` пропускает первую страницу. `limit` берёт максимум два элемента из оставшихся, поэтому последняя страница может быть короче.

</details>

### 09. Проверить наличие ошибки

Среди `['INFO ready', 'ERROR timeout', 'WARN retry']` проверь, есть ли строка, начинающаяся с `'ERROR '`. Ожидается `true`.

<details>
<summary>Решение</summary>

```java
var result = List.of("INFO ready", "ERROR timeout", "WARN retry").stream()
        .anyMatch(line -> line.startsWith("ERROR "));
```

`anyMatch` отвечает на вопрос «подходит ли хотя бы один элемент?». В последовательном потоке после найденной ошибки проверять остальные строки не требуется.

</details>

### 10. Собрать метки в одну строку

Из `['java', 'sql', 'git']` получи `[java | sql | git]`.

<details>
<summary>Решение</summary>

```java
var result = List.of("java", "sql", "git").stream()
        .collect(Collectors.joining(" | ", "[", "]"));
```

`joining` вставляет разделитель между соседними строками и обрамляет результат скобками.

</details>

## 11–20. Преобразования и коллекции

### 11. Объединить вложенные списки

Из `[[1, 2], [], [3, 4]]` получи `[1, 2, 3, 4]`.

<details>
<summary>Решение</summary>

```java
var groups = List.of(List.of(1, 2), List.<Integer>of(), List.of(3, 4));
var result = groups.stream()
        .flatMap(List::stream)
        .toList();
```

`flatMap` раскрывает каждый вложенный список и объединяет его элементы с остальными. Пустой список ничего не добавляет.

</details>

### 12. Извлечь значения из `Optional`

Из `[Optional.of('java'), Optional.empty(), Optional.of('sql')]` получи `['java', 'sql']`.

<details>
<summary>Решение</summary>

```java
var values = List.of(Optional.of("java"), Optional.<String>empty(), Optional.of("sql"));
var result = values.stream()
        .flatMap(Optional::stream)
        .toList();
```

`Optional.stream` даёт поток из одного значения или пустой поток. `flatMap` склеивает эти потоки в один.

</details>

### 13. Посчитать среднее из массива `int[]`

Найди среднее для `{2, 3, 5, 8}`. Для пустого массива верни `0.0`. Ожидается `4.5`.

<details>
<summary>Решение</summary>

```java
int[] values = {2, 3, 5, 8};
var result = Arrays.stream(values)
        .average()
        .orElse(0.0);
```

`Arrays.stream(int[])` создаёт `IntStream`. Его `average` возвращает `OptionalDouble`, который пуст для массива без элементов.

</details>

### 14. Нормализовать теги перед удалением дублей

Из `[' Java ', 'SQL', 'java', '  ', ' sql ']` получи `['java', 'sql']`. Убери пробелы по краям, приведи теги к нижнему регистру и пропусти пустые.

<details>
<summary>Решение</summary>

```java
var result = List.of(" Java ", "SQL", "java", "  ", " sql ").stream()
        .map(String::strip)
        .filter(tag -> !tag.isEmpty())
        .map(tag -> tag.toLowerCase(Locale.ROOT))
        .distinct()
        .toList();
```

После очистки разные записи одного тега становятся одинаковыми, и `distinct` оставляет по одной. `Locale.ROOT` делает регистр независимым от языка системы.

</details>

### 15. Отсортировать по двум признакам

Отсортируй `['pear', 'fig', 'plum', 'kiwi']` по длине, а при равной длине — по алфавиту. Ожидается `['fig', 'kiwi', 'pear', 'plum']`.

<details>
<summary>Решение</summary>

```java
var result = List.of("pear", "fig", "plum", "kiwi").stream()
        .sorted(Comparator.comparingInt(String::length)
                .thenComparing(Comparator.naturalOrder()))
        .toList();
```

Сначала сравниваются длины. `thenComparing` сравнивает сами строки только при равной длине.

</details>

### 16. Собрать изменяемый список

Преобразуй `[1, 2, 3]` в квадраты и добавь число `16` в конец результата. Ожидается `[1, 4, 9, 16]`.

<details>
<summary>Решение</summary>

```java
var result = List.of(1, 2, 3).stream()
        .map(n -> n * n)
        .collect(Collectors.toCollection(ArrayList::new));
result.add(16);
```

`toCollection` создаёт именно `ArrayList`, в который можно добавлять элементы. Список из `Stream.toList()` запрещает `add`.

</details>

### 17. Посчитать частоту статусов

Из `['NEW', 'PAID', 'NEW', 'PAID', 'PAID']` получи `{NEW=2, PAID=3}`.

<details>
<summary>Решение</summary>

```java
var result = List.of("NEW", "PAID", "NEW", "PAID", "PAID").stream()
        .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
```

`groupingBy` заводит группу для каждого статуса. `counting` сохраняет размер группы как `long`.

</details>

### 18. Разбить значения на две группы

Раздели `[60, 75, 90, 65]` на прошедших порог `70` и остальных. Ожидается `{true=[75, 90], false=[60, 65]}`.

<details>
<summary>Решение</summary>

```java
var result = List.of(60, 75, 90, 65).stream()
        .collect(Collectors.partitioningBy(score -> score >= 70));
```

`partitioningBy` кладёт элементы в одну из двух групп по результату условия. Оба ключа присутствуют, даже если одна группа пуста.

</details>

### 19. Разрешить конфликт одинаковых ключей

Построй словарь «длина → первое слово такой длины» из `['ant', 'dog', 'bear']`. Ожидается `{3=ant, 4=bear}`.

<details>
<summary>Решение</summary>

```java
var result = List.of("ant", "dog", "bear").stream()
        .collect(Collectors.toMap(String::length, word -> word,
                (first, second) -> first));
```

У `ant` и `dog` один ключ. Третий аргумент `toMap` задаёт правило слияния: оставляем первое значение. Без этого правила сборка завершится исключением.

</details>

### 20. Развернуть словарь со списками

Есть словарь «группа → ID»: `{1=[20, 30], 2=[40]}`. Получи `{20=1, 30=1, 40=2}`. Считай, что каждый ID встречается один раз.

<details>
<summary>Решение</summary>

```java
var groups = Map.of(1, List.of(20, 30), 2, List.of(40));
var result = groups.entrySet().stream()
        .flatMap(group -> group.getValue().stream()
                .map(id -> Map.entry(id, group.getKey())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

Из одной записи словаря получается несколько пар «ID → группа». `flatMap` собирает все пары в один поток, а `toMap` превращает их в словарь.

</details>

## 21–30. Задачи на обработку данных

### 21. Посчитать выручку по магазинам

Есть продажи `[('A', 120), ('B', 80), ('A', 50)]`. Получи `{A=170, B=80}`. Суммы заданы в целых денежных единицах.

<details>
<summary>Решение</summary>

```java
record Sale(String shop, long amount) {}

var sales = List.of(new Sale("A", 120), new Sale("B", 80), new Sale("A", 50));
var result = sales.stream()
        .collect(Collectors.groupingBy(Sale::shop, Collectors.summingLong(Sale::amount)));
```

`groupingBy` выбирает магазин, а `summingLong` складывает суммы продаж внутри него.

</details>

### 22. Найти два самых частых слова

Из `['sql', 'java', 'git', 'sql', 'java', 'sql', 'git']` верни два самых частых слова. При равной частоте бери то, что раньше по алфавиту. Ожидается `['sql', 'git']`.

<details>
<summary>Решение</summary>

```java
var counts = List.of("sql", "java", "git", "sql", "java", "sql", "git").stream()
        .collect(Collectors.groupingBy(word -> word, Collectors.counting()));
var result = counts.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(2)
        .map(Map.Entry::getKey)
        .toList();
```

Сначала считаем частоту каждого слова. Затем сортируем пары по частоте и по слову. `reversed()` разворачивает только порядок частот, потому что вызывается до `thenComparing`.

</details>

### 23. Найти самую высокую оценку в каждой команде

Из результатов `[('red', 7), ('blue', 9), ('red', 10)]` получи `{red=10, blue=9}`.

<details>
<summary>Решение</summary>

```java
record Score(String team, int points) {}

var scores = List.of(new Score("red", 7), new Score("blue", 9), new Score("red", 10));
var result = scores.stream()
        .collect(Collectors.toMap(Score::team, Score::points, Integer::max));
```

Первый балл сохраняется под именем команды. При следующем балле той же команды `Integer::max` оставляет больший из двух.

</details>

### 24. Построить индекс «тег → статьи»

У статьи `1` теги `['java', 'sql']`, у статьи `2` — `['java', 'java']`, у статьи `3` тегов нет. Получи `{java=[1, 2], sql=[1]}`, где значения имеют тип `Set<Integer>`.

<details>
<summary>Решение</summary>

```java
record Article(int id, List<String> tags) {}

var articles = List.of(new Article(1, List.of("java", "sql")),
        new Article(2, List.of("java", "java")), new Article(3, List.of()));
var result = articles.stream()
        .flatMap(article -> article.tags().stream().map(tag -> Map.entry(tag, article.id())))
        .collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toSet())));
```

`flatMap` создаёт по паре «тег → ID» на каждый тег. Внутри группы `mapping` берёт из пары только ID, а множество убирает повтор статьи.

</details>

### 25. Сохранить клиентов без оплаченных заказов

Есть заказы `[('A', 1, true), ('B', 2, false), ('A', 3, false)]` в формате «клиент, ID, оплачен». Для каждого клиента собери ID оплаченных заказов. Ожидается `{A=[1], B=[]}`.

<details>
<summary>Решение</summary>

```java
record Order(String customer, int id, boolean paid) {}

var orders = List.of(new Order("A", 1, true), new Order("B", 2, false),
        new Order("A", 3, false));
var result = orders.stream()
        .collect(Collectors.groupingBy(Order::customer,
                Collectors.filtering(Order::paid,
                        Collectors.mapping(Order::id, Collectors.toList()))));
```

Группа создаётся для каждого клиента из входных данных. Фильтр работает внутри группы, поэтому клиент `B` остаётся с пустым списком.

</details>

### 26. Найти дубли в порядке первого появления

Из `[4, 1, 2, 2, 4, 3]` выбери значения, которые встретились больше одного раза. Ожидается `[4, 2]`.

<details>
<summary>Решение</summary>

```java
var counts = List.of(4, 1, 2, 2, 4, 3).stream()
        .collect(Collectors.groupingBy(n -> n, LinkedHashMap::new, Collectors.counting()));
var result = counts.entrySet().stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .toList();
```

`LinkedHashMap` обходит ключи в порядке добавления. `4` стоит первым, хотя второй раз оно встречается позже, чем второй раз встречается `2`.

</details>

### 27. Вычислить разницы между соседями

Из показаний `[10, 13, 11, 18]` получи изменения `[3, -2, 7]`. Для входа с нулём или одним элементом верни пустой список.

<details>
<summary>Решение</summary>

```java
var values = List.of(10, 13, 11, 18);
var result = IntStream.range(1, values.size())
        .map(i -> values.get(i) - values.get(i - 1))
        .boxed()
        .toList();
```

Начинаем с индекса `1`, потому что у него есть левый сосед. `boxed` превращает значения `int` в `Integer`, чтобы собрать их в список.

</details>

### 28. Выбрать последнее событие по каждому ID

События приходят в произвольном порядке. Из `[('a', 30, 'paid'), ('a', 10, 'new'), ('b', 20, 'new')]` в формате «ID, время, статус» получи `{a=paid, b=new}`. Время для одного ID всегда различается.

<details>
<summary>Решение</summary>

```java
record Event(String id, long time, String status) {}

var events = List.of(new Event("a", 30, "paid"), new Event("a", 10, "new"),
        new Event("b", 20, "new"));
var latest = events.stream()
        .collect(Collectors.toMap(Event::id, event -> event,
                java.util.function.BinaryOperator.maxBy(Comparator.comparingLong(Event::time))));
var result = latest.values().stream()
        .collect(Collectors.toMap(Event::id, Event::status));
```

При конфликте оставляем событие с большим временем. Превращаем событие в статус уже после слияния, когда время для сравнения больше не нужно.

</details>

### 29. Построить все сочетания настроек

Для окружений `['dev', 'prod']` и баз `['pg', 'mysql']` собери `['dev/pg', 'dev/mysql', 'prod/pg', 'prod/mysql']`.

<details>
<summary>Решение</summary>

```java
var environments = List.of("dev", "prod");
var databases = List.of("pg", "mysql");
var result = environments.stream()
        .flatMap(env -> databases.stream().map(db -> env + "/" + db))
        .toList();
```

Для каждого окружения создаём новый поток из всех баз. `flatMap` добавляет в результат все сочетания в порядке окружений и баз.

</details>

### 30. Посчитать две суммы за один проход

В списке операций `[100, -30, 20, -15, 0]` отдельно посчитай поступления и абсолютную сумму списаний. Ожидается `Totals[income=120, expense=45]`.

<details>
<summary>Решение</summary>

```java
record Totals(long income, long expense) {}

var result = List.of(100, -30, 20, -15, 0).stream()
        .collect(Collectors.teeing(
                Collectors.filtering(n -> n > 0, Collectors.summingLong(Integer::longValue)),
                Collectors.filtering(n -> n < 0, Collectors.summingLong(n -> -(long) n)),
                Totals::new));
```

`teeing` передаёт каждую операцию в оба коллектора. Один складывает поступления, другой — модули списаний. Приведение к `long` перед сменой знака работает и для `Integer.MIN_VALUE`.

</details>

## 31–40. Граничные случаи и продвинутые приёмы

### 31. Выдать несколько значений из одного через `mapMulti`

Из `['a,b', '', 'c,,d', ' e ']` получи `['a', 'b', 'c', 'd', 'e']`. Раздели строки по запятой, убери пробелы по краям и пропусти пустые фрагменты. Используй `mapMulti`.

<details>
<summary>Решение</summary>

```java
var result = List.of("a,b", "", "c,,d", " e ").stream()
        .<String>mapMulti((line, emit) -> {
            for (String part : line.split(",")) {
                String token = part.strip();
                if (!token.isEmpty()) {
                    emit.accept(token);
                }
            }
        })
        .toList();
```

Для каждой строки `emit.accept` вызывается столько раз, сколько токенов нужно передать дальше. Если токенов нет, ничего не вызываем. `<String>` задаёт тип элементов выходного потока.

</details>

### 32. Предсказать, что успеет сделать `map`

Поток из `[1, 2, 3, 4]` возводит числа в квадрат и берёт первый квадрат больше `4`. Какой ответ получится и какие числа обработает `map`?

<details>
<summary>Решение</summary>

```java
var visited = new ArrayList<Integer>();
var result = List.of(1, 2, 3, 4).stream()
        .map(n -> {
            visited.add(n);
            return n * n;
        })
        .filter(square -> square > 4)
        .findFirst()
        .orElseThrow();
```

Результат — `9`, а при таком последовательном обходе `visited` содержит `[1, 2, 3]`. Каждый квадрат сразу проверяется фильтром. После первого совпадения `findFirst` завершает обход. Список здесь нужен, чтобы увидеть порядок вычислений.

</details>

### 33. Ограничить бесконечный поток до сортировки

Из последовательности `10, 9, 8, 7, …` возьми первые четыре элемента и отсортируй их. Ожидается `[7, 8, 9, 10]`. Что случится, если поставить `sorted()` перед `limit(4)`?

<details>
<summary>Решение</summary>

```java
var result = Stream.iterate(10, n -> n - 1)
        .limit(4)
        .sorted()
        .toList();
```

После `limit` у сортировки всего четыре числа. Если поставить сортировку первой, она будет пытаться прочитать весь бесконечный поток и результат не получится.

</details>

### 34. Проверить предикаты на пустом потоке

Для пустого списка вычисли `anyMatch(n > 0)`, `allMatch(n > 0)` и `noneMatch(n > 0)`. Предскажи три значения.

<details>
<summary>Решение</summary>

```java
var values = List.<Integer>of();
var result = List.of(
        values.stream().anyMatch(n -> n > 0),
        values.stream().allMatch(n -> n > 0),
        values.stream().noneMatch(n -> n > 0));
```

Ответ — `[false, true, true]`. Подходящего элемента нет, поэтому `anyMatch` даёт `false`. Нарушителя условия тоже нет, поэтому `allMatch` истинно. `noneMatch` истинно, потому что ни один элемент не подходит. Для каждой проверки создаётся отдельный поток.

</details>

### 35. Исправить начальное значение `reduce`

Выражение `List.of(1, 2, 3, 4).parallelStream().reduce(10, Integer::sum)` должно дать сумму с прибавкой `10`. Исправь его так, чтобы результат всегда был `20`.

<details>
<summary>Решение</summary>

```java
var values = List.of(1, 2, 3, 4);
var result = 10 + values.parallelStream()
        .reduce(0, Integer::sum);
```

При параллельной обработке каждая часть может начать подсчёт со стартового значения. `0` подходит для каждой части, потому что прибавление нуля ничего не меняет. Прибавку `10` делаем один раз после объединения частей.

</details>

### 36. Отделить заголовок от тела по разделителю

Раздели строки `['Title', 'Author', '---', 'First', '---', 'Last']` по первому `'---'`. Ожидаются заголовок `['Title', 'Author']` и тело `['First', '---', 'Last']`.

<details>
<summary>Решение</summary>

```java
var lines = List.of("Title", "Author", "---", "First", "---", "Last");
var header = lines.stream().takeWhile(line -> !line.equals("---")).toList();
var body = lines.stream().dropWhile(line -> !line.equals("---")).skip(1).toList();
var result = List.of(header, body);
```

`takeWhile` берёт префикс до первого разделителя. `dropWhile` пропускает тот же префикс и останавливается на разделителе, поэтому `skip(1)` удаляет ровно его. Следующий `'---'` остаётся в теле.

</details>

### 37. Найти пропущенные ID

В диапазоне от `100` до `106` включительно получены ID `[100, 103, 103, 106]`. Найди пропуски по возрастанию. Ожидается `[101, 102, 104, 105]`.

<details>
<summary>Решение</summary>

```java
var received = new HashSet<>(List.of(100, 103, 103, 106));
var result = IntStream.rangeClosed(100, 106)
        .filter(id -> !received.contains(id))
        .boxed()
        .toList();
```

`rangeClosed` включает обе границы и выдаёт числа по порядку. `HashSet` позволяет быстро проверять, пришёл ли ID. Дубликат `103` не влияет на проверку.

</details>

### 38. Посчитать ошибки в файле и закрыть поток

Запиши в `app.log` три строки `'INFO ready'`, `'ERROR timeout'` и `'ERROR retry'`. Прочитай файл с помощью `Files.lines` и посчитай строки, начинающиеся с `'ERROR '`. Ожидается `2L`.

<details>
<summary>Решение</summary>

```java
long result;
try (var lines = Files.lines(Path.of("app.log"))) {
    result = lines.filter(line -> line.startsWith("ERROR ")).count();
}
```

`Files.lines` читает строки по мере обхода. `try-with-resources` закрывает поток вместе с файлом, даже если при чтении возникнет ошибка.

</details>

### 39. Разбить список на пакеты

Из `['a', 'b', 'c', 'd', 'e']` собери пакеты по два элемента. Ожидается `[['a', 'b'], ['c', 'd'], ['e']]`. Последний пакет может быть короче.

<details>
<summary>Решение</summary>

```java
var values = List.of("a", "b", "c", "d", "e");
int batchSize = 2;
var result = IntStream.iterate(0, start -> start < values.size(), start -> start + batchSize)
        .mapToObj(start -> {
            int length = Math.min(batchSize, values.size() - start);
            return List.copyOf(values.subList(start, start + length));
        })
        .toList();
```

`iterate` выдаёт начала пакетов: `0`, `2` и `4`. Ограничиваем длину остатком списка, чтобы последний пакет не вышел за границу. `List.copyOf` сохраняет каждый пакет отдельным списком.

</details>

### 40. Написать коллектор с объединением частей

Собери количество и сумму положительных значений из `[7, -1, 0, 5, 3]` в `PositiveStats[count=3, sum=15]`. Используй `Collector.of` и проверь результат на параллельном потоке.

<details>
<summary>Решение</summary>

```java
record PositiveStats(long count, long sum) {}

class Accumulator {
    private long count;
    private long sum;

    void add(int number) {
        if (number > 0) {
            count++;
            sum += number;
        }
    }

    Accumulator merge(Accumulator other) {
        count += other.count;
        sum += other.sum;
        return this;
    }

    PositiveStats finish() {
        return new PositiveStats(count, sum);
    }
}

Collector<Integer, Accumulator, PositiveStats> collector = Collector.of(
        Accumulator::new, Accumulator::add, Accumulator::merge, Accumulator::finish);
var result = List.of(7, -1, 0, 5, 3).parallelStream().collect(collector);
```

У каждой части свой накопитель. `add` учитывает только положительные числа, `merge` складывает частичные счётчики и суммы, а `finish` превращает итог в запись. Для пустого потока оба поля равны нулю.

</details>
