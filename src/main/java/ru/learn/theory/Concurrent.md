# Многопоточность и конкурентность

> Конспект-тренажёр для подготовки к собеседованиям.
> Примеры проверены на **OpenJDK 25** (машина с 2 ядрами); результаты в лабораторных получены запуском. Числа в гонках данных меняются от запуска к запуску — важен не конкретный результат, а то, что он неверный.

## Как пользоваться файлом

Каждая тема устроена одинаково:

1. **Короткий ответ** — то, что нужно сказать устно за 30–60 секунд.
2. **Подробности** — под спойлером `<details>`: механизм, ограничения, примеры.
3. **Уточняющие вопросы** — то, что интервьюер спросит следом.
4. **Практика** — лабораторная или задача.

Отмечайте уверенность прямо в файле:

- `[ ]` — не знаю;
- `[~]` — отвечаю с подсказкой;
- `[x]` — объясняю сам, с примером.

Повторяйте прежде всего то, что отмечено `[ ]` и `[~]`.

---

## Содержание

0. [Быстрое повторение](#0-быстрое-повторение)
1. [Процесс, поток, задача](#1-процесс-поток-задача)
2. [Java Memory Model и happens-before](#2-java-memory-model-и-happens-before)
3. [volatile, атомики, CAS](#3-volatile-атомики-cas)
4. [synchronized и Lock](#4-synchronized-и-lock)
5. [Проблемы: гонки, deadlock, livelock, starvation](#5-проблемы-гонки-deadlock-livelock-starvation)
6. [Синхронизаторы](#6-синхронизаторы)
7. [Потокобезопасные коллекции](#7-потокобезопасные-коллекции)
8. [Executors и пулы потоков](#8-executors-и-пулы-потоков)
9. [CompletableFuture](#9-completablefuture)
10. [Fork/Join](#10-forkjoin)
11. [Виртуальные потоки](#11-виртуальные-потоки)
12. [Structured Concurrency и Scoped Values](#12-structured-concurrency-и-scoped-values)
13. [ThreadLocal](#13-threadlocal)
14. [Пробное собеседование](#14-пробное-собеседование)
15. [Источники](#15-источники)

---

## 0. Быстрое повторение

Ответьте вслух, не открывая разделы. Ответы — по ссылкам.

| Уверенность | Вопрос | Раздел |
|---|---|---|
| `[ ]` | Чем процесс отличается от потока? Что общее у потоков? | [1](#1-процесс-поток-задача) |
| `[ ]` | Что такое happens-before? Назовите 4 правила. | [2](#2-java-memory-model-и-happens-before) |
| `[ ]` | Почему `count++` не атомарен даже с `volatile`? | [3.1](#31-volatile) |
| `[ ]` | Что такое CAS и проблема ABA? | [3.3](#33-cas-и-aba) |
| `[ ]` | Когда `LongAdder` лучше `AtomicLong`? | [3.2](#32-атомики) |
| `[ ]` | Зачем `volatile` в double-checked locking? | [4.3](#43-double-checked-locking) |
| `[ ]` | Чем `ReentrantLock` лучше `synchronized`? | [4.2](#42-reentrantlock-и-readwritelock) |
| `[ ]` | Race condition и data race — одно и то же? | [5.1](#51-race-condition-и-data-race) |
| `[ ]` | Как найти и предотвратить deadlock? | [5.2](#52-deadlock) |
| `[ ]` | Назовите состояния потока. Чем BLOCKED отличается от WAITING? | [1.2](#12-состояния-потока) |
| `[ ]` | CountDownLatch, CyclicBarrier, Semaphore — в чём разница? | [6](#6-синхронизаторы) |
| `[ ]` | Почему `get` + `put` на `ConcurrentHashMap` — ошибка? | [7.1](#71-concurrenthashmap) |
| `[ ]` | Почему `newFixedThreadPool` опасен на сервере? | [8.2](#82-threadpoolexecutor-и-очередь) |
| `[ ]` | Как подобрать размер пула? | [8.3](#83-размер-пула) |
| `[ ]` | Куда пропадает исключение из `submit`? | [8.4](#84-исключения-в-пуле) |
| `[ ]` | В каком потоке выполнится `thenApply`? | [9](#9-completablefuture) |
| `[ ]` | Что дают виртуальные потоки и чего не дают? | [11](#11-виртуальные-потоки) |
| `[ ]` | Пиннинг на `synchronized` — всё ещё проблема в JDK 25? | [11.2](#112-пиннинг) |
| `[ ]` | Чем опасен ThreadLocal в пуле? | [13](#13-threadlocal) |

---

## 1. Процесс, поток, задача

`[ ]` **Короткий ответ:** процесс — изолированная среда выполнения со своим адресным пространством. Поток — последовательность исполнения внутри процесса. Потоки одного процесса разделяют кучу (heap) и открытые файлы, но у каждого свой стек и программный счётчик. JVM — один процесс ОС, внутри которого есть платформенные потоки (1:1 с потоками ОС) и, начиная с JDK 21, виртуальные.

### 1.1. Поток, задача и исполнитель

- `Runnable` — задача без результата.
- `Callable<T>` — задача с результатом, может бросить проверяемое исключение.
- `Thread` — поток выполнения.
- В прикладном коде задачу отделяют от способа её выполнения и передают в `ExecutorService`. Наследоваться от `Thread` почти никогда не нужно.

### 1.2. Состояния потока

| Состояние | Когда |
|---|---|
| `NEW` | Создан, `start()` ещё не вызван |
| `RUNNABLE` | Выполняется или готов к выполнению. JVM не различает «работает на CPU» и «ждёт CPU»; поток в блокирующем вводе-выводе тоже обычно `RUNNABLE` |
| `BLOCKED` | Ждёт захвата монитора (`synchronized`) |
| `WAITING` | Ждёт без таймаута: `wait()`, `join()`, `LockSupport.park()` |
| `TIMED_WAITING` | Ждёт с таймаутом: `sleep()`, `wait(ms)`, `join(ms)`, `parkNanos()` |
| `TERMINATED` | Завершён |

**Уточнение интервьюера:** «Поток ждёт `ReentrantLock.lock()`. Какое у него состояние?» — `WAITING`, а не `BLOCKED`: `ReentrantLock` реализован через `LockSupport.park()`. `BLOCKED` бывает только на мониторе `synchronized`.

<details>
<summary>Планировщик, приоритеты, daemon-потоки</summary>

- Планировщик ОС и приоритеты потоков не гарантируют ни порядка, ни справедливости. `Thread.yield()` — только подсказка, на неё нельзя опираться для корректности.
- **Daemon-потоки** не удерживают JVM: процесс может завершиться, когда остались только демоны. При этом их код просто перестаёт выполняться — блоки `finally` не гарантированы. Не используйте daemon для задач, которые обязаны дописать данные или освободить ресурсы.

</details>

---

## 2. Java Memory Model и happens-before

`[ ]` **Короткий ответ:** без синхронизации один поток может не увидеть запись другого или увидеть записи в другом порядке — из-за кешей процессора, регистров и переупорядочивания компилятором. JMM определяет отношение happens-before: если A happens-before B, то все эффекты A видны в B. Это гарантия видимости и порядка, а не утверждение о физическом времени.

Основные правила:

| Правило | Гарантия |
|---|---|
| Порядок программы | Внутри одного потока действия упорядочены |
| Монитор | Выход из `synchronized` по монитору happens-before последующему входу по тому же монитору |
| `volatile` | Запись в `volatile`-поле happens-before последующему чтению этого поля |
| Запуск потока | `Thread.start()` happens-before любым действиям запущенного потока |
| Завершение потока | Все действия потока happens-before успешному возврату из `join()` |
| `java.util.concurrent` | Помещение в потокобезопасную коллекцию, `Future.get()`, `CountDownLatch` и др. дают свои гарантии |
| Транзитивность | A hb B и B hb C → A hb C |

**Безопасная публикация.** Объект безопасно опубликован, если ссылку на него записали в `volatile`-поле, `final`-поле, потокобезопасную коллекцию или под блокировкой. Неизменяемые объекты с `final`-полями видны корректно даже без синхронизации — это часто проще, чем любые блокировки.

### Лабораторная 1. Видимость без volatile

`[ ]` Предскажите: остановится ли цикл без `volatile`?

<details>
<summary>Код (запуск: java Vis.java)</summary>

```java
public class Vis {
    static boolean running = true;           // без volatile
    static volatile boolean vRunning = true;

    public static void main(String[] a) throws Exception {
        Thread t = new Thread(() -> { long n = 0; while (running) n++; System.out.println("plain stopped"); });
        t.setDaemon(true); t.start();
        Thread v = new Thread(() -> { long n = 0; while (vRunning) n++; System.out.println("volatile stopped"); });
        v.setDaemon(true); v.start();
        Thread.sleep(1000);
        running = false; vRunning = false;
        t.join(2000); v.join(2000);
        System.out.println("plain alive after 2s: " + t.isAlive() + ", volatile alive: " + v.isAlive());
    }
}
```

</details>

<details>
<summary>Результат запуска</summary>

```text
volatile stopped
plain alive after 2s: true, volatile alive: false
```

JIT-компилятор вынес чтение `running` из цикла (это законно: без happens-before он вправе считать, что поле не меняется). Поток без `volatile` крутится вечно. Поведение не гарантировано в обе стороны: на другой JVM или без JIT цикл может и остановиться — именно поэтому такие ошибки трудно воспроизвести.

</details>

---

## 3. volatile, атомики, CAS

### 3.1. volatile

`[ ]` **Короткий ответ:** `volatile` гарантирует видимость записи всем потокам и запрещает переупорядочивание вокруг обращений к этому полю. Но он не делает составные операции атомарными: `count++` — это чтение, увеличение и запись; два потока могут прочитать одно значение и потерять одно увеличение.

**Когда что использовать:**

| Механизм | Когда |
|---|---|
| `volatile` | Независимый флаг или ссылка, которую один поток пишет, а другие читают |
| Атомики | Атомарная операция над **одной** переменной: счётчик, CAS-обновление ссылки |
| Блокировка | Инвариант из нескольких полей, проверка-и-действие, критическая секция |

### 3.2. Атомики

`[ ]` **Короткий ответ:** классы `java.util.concurrent.atomic` дают атомарные операции над одной переменной (`incrementAndGet`, `compareAndSet`, `updateAndGet`). `LongAdder` при высокой конкуренции быстрее `AtomicLong`: он распределяет увеличения по нескольким ячейкам и суммирует их при чтении. Минус — `sum()` не моментальный снимок, и нет `compareAndSet`.

### Лабораторная 2. Потерянные увеличения

`[ ]` Четыре потока по миллиону раз увеличивают три счётчика. Что получится?

<details>
<summary>Код</summary>

```java
static int plain = 0;

AtomicInteger atomic = new AtomicInteger();
LongAdder adder = new LongAdder();
Thread[] ts = new Thread[4];
for (int i = 0; i < 4; i++) {
    ts[i] = new Thread(() -> {
        for (int j = 0; j < 1_000_000; j++) { plain++; atomic.incrementAndGet(); adder.increment(); }
    });
    ts[i].start();
}
for (Thread t : ts) t.join();
System.out.println("plain=" + plain + " atomic=" + atomic.get() + " adder=" + adder.sum());
```

</details>

<details>
<summary>Результат запуска</summary>

```text
plain=3825508 atomic=4000000 adder=4000000
```

Обычный `int` потерял около 175 тысяч увеличений. Число меняется от запуска к запуску; добавление `volatile` к `plain` не помогло бы.

</details>

### 3.3. CAS и ABA

`[ ]` **Короткий ответ:** CAS (compare-and-set) атомарно сравнивает текущее значение с ожидаемым и, если совпало, записывает новое. При неудаче цикл перечитывает значение и пробует снова. Проблема ABA: значение изменилось A → B → A, и CAS этого не заметит. Решение — версия рядом со значением: `AtomicStampedReference`.

<details>
<summary>Пример ABA и результат запуска</summary>

```java
AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);
int stamp = ref.getStamp();              // запомнили версию 0
ref.compareAndSet("A", "B", 0, 1);       // другой поток: A -> B
ref.compareAndSet("B", "A", 1, 2);       // и обратно: B -> A
boolean plainCas = new AtomicReference<>("A").compareAndSet("A", "C");
boolean stamped  = ref.compareAndSet("A", "C", stamp, stamp + 1);
```

```text
AtomicReference-like CAS A->C ok=true; stamped CAS with old stamp ok=false
```

Обычный CAS видит «A» и проходит. Версионированный видит, что версия уже 2, и отказывает.

ABA особенно опасна в lock-free структурах с переиспользованием узлов (стек, где узел удалили и вернули). В Java сборщик мусора частично защищает от неё: пока на узел есть ссылка, его память не переиспользуется.

</details>

Не стоит утверждать, что любой атомик lock-free на любой платформе: это зависит от типа, операции и процессора.

---

## 4. synchronized и Lock

### 4.1. synchronized

`[ ]` **Короткий ответ:** `synchronized` захватывает встроенный монитор объекта (или класса для `static`-методов). Монитор реентерабелен: поток, уже владеющий им, может войти снова. Освободить его может только владелец; при исключении освобождается автоматически. Даёт и взаимное исключение, и видимость (happens-before).

- `wait()` / `notify()` / `notifyAll()` вызываются только внутри `synchronized` по тому же объекту, иначе `IllegalMonitorStateException`.
- `wait()` вызывают в цикле `while (!условие)`: бывают ложные пробуждения, и условие могло измениться до того, как поток снова захватил монитор.
- Синхронизироваться на публичном объекте (`this`, строковом литерале, `Integer`) рискованно: чужой код может захватить тот же монитор. Лучше приватный `final Object lock`.

### 4.2. ReentrantLock и ReadWriteLock

`[ ]` **Короткий ответ:** `ReentrantLock` — явная блокировка с той же семантикой владения и видимости, но с дополнительными возможностями: `tryLock()` с таймаутом, прерываемое ожидание `lockInterruptibly()`, опциональная справедливость, несколько условий (`Condition`). Освобождать обязательно в `finally`.

```java
private final ReentrantLock lock = new ReentrantLock();

void transfer() throws InterruptedException {
    if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) throw new IllegalStateException("busy");
    try {
        // критическая секция
    } finally {
        lock.unlock();
    }
}
```

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Освобождение | Автоматически | Вручную в `finally` |
| Попытка с таймаутом | Нет | `tryLock(timeout)` |
| Прерывание ожидания | Нет | `lockInterruptibly()` |
| Справедливость | Нет | `new ReentrantLock(true)` |
| Условия | Одно (`wait`/`notify`) | Несколько `Condition` |
| Состояние ждущего потока | `BLOCKED` | `WAITING` / `TIMED_WAITING` |

- **ReadWriteLock** (`ReentrantReadWriteLock`) — много читателей одновременно или один писатель. Выгоден при редких записях и долгих чтениях; при коротких операциях накладные расходы съедают выигрыш.
- **StampedLock** — добавляет оптимистичное чтение: прочитать без блокировки, затем проверить `validate(stamp)`. Не реентерабелен.

### 4.3. Double-checked locking

`[ ]` **Короткий ответ:** ленивая инициализация «проверить без блокировки → захватить → проверить снова». Корректна только с `volatile` на поле: без него другой поток может увидеть ссылку на объект раньше, чем записи в его поля (переупорядочивание). Проще — holder idiom: класс загружается лениво и потокобезопасно.

```java
// Вариант 1: DCL с volatile
private static volatile Config instance;
static Config get() {
    Config local = instance;
    if (local == null) {
        synchronized (Config.class) {
            local = instance;
            if (local == null) instance = local = new Config();
        }
    }
    return local;
}

// Вариант 2: holder idiom — без volatile и synchronized
static final class Holder { static final Config INSTANCE = new Config(); }
static Config get2() { return Holder.INSTANCE; }
```

---

## 5. Проблемы: гонки, deadlock, livelock, starvation

### 5.1. Race condition и data race

`[ ]` **Короткий ответ:** это разные понятия. **Race condition** — результат зависит от порядка выполнения конкурентных операций (логическая ошибка). **Data race** по JMM — два потока обращаются к одной переменной без happens-before, и хотя бы одно обращение — запись. Можно иметь race condition без data race: `if (map.containsKey(k)) map.get(k)` на `ConcurrentHashMap` — каждое обращение синхронизировано, но между ними ключ может исчезнуть.

### 5.2. Deadlock

`[ ]` **Короткий ответ:** циклическое ожидание: поток 1 держит A и ждёт B, поток 2 держит B и ждёт A. Обнаружить — thread dump (`jstack <pid>`, `jcmd <pid> Thread.print`) или `ThreadMXBean.findDeadlockedThreads()`. Предотвратить — единый порядок захвата блокировок, меньше вложенных блокировок, `tryLock` с таймаутом.

### Лабораторная 3. Поймать deadlock программно

<details>
<summary>Код</summary>

```java
Object a = new Object(), b = new Object();
CountDownLatch both = new CountDownLatch(2);    // гарантируем, что оба взяли первую блокировку
Thread t1 = new Thread(() -> { synchronized (a) { both.countDown(); await(both); synchronized (b) {} } }, "worker-1");
Thread t2 = new Thread(() -> { synchronized (b) { both.countDown(); await(both); synchronized (a) {} } }, "worker-2");
t1.setDaemon(true); t2.setDaemon(true);
t1.start(); t2.start();
Thread.sleep(300);

ThreadMXBean mx = ManagementFactory.getThreadMXBean();
for (ThreadInfo info : mx.getThreadInfo(mx.findDeadlockedThreads()))
    System.out.println("DEADLOCK " + info.getThreadName() + " " + info.getThreadState()
        + " waits " + info.getLockName() + " held by " + info.getLockOwnerName());
```

</details>

<details>
<summary>Результат запуска</summary>

```text
DEADLOCK worker-1 BLOCKED waits java.lang.Object held by worker-2
DEADLOCK worker-2 BLOCKED waits java.lang.Object held by worker-1
```

Тот же вариант с `ReentrantLock.tryLock(100, MILLISECONDS)` вернул `false` вместо вечного ожидания. Но таймаут лишь предотвращает зависание; код всё равно должен корректно откатить начатое действие.

В thread dump из `jstack` в конце есть раздел `Found one Java-level deadlock` с цепочкой потоков и мониторов.

</details>

<details>
<summary>Единый порядок захвата: перевод денег</summary>

```java
void transfer(Account from, Account to, long amount) {
    Account first  = from.id() < to.id() ? from : to;   // всегда сначала меньший id
    Account second = from.id() < to.id() ? to : from;
    synchronized (first) {
        synchronized (second) {
            from.withdraw(amount);
            to.deposit(amount);
        }
    }
}
```

Два встречных перевода A → B и B → A захватят блокировки в одном порядке, цикла не будет.

</details>

### 5.3. Livelock и starvation

- **Livelock** — потоки активны, но мешают друг другу продвинуться: оба уступают дорогу и снова сталкиваются. Помогает случайная задержка перед повтором.
- **Starvation (голодание)** — поток долго не получает ресурс или CPU: несправедливая блокировка всегда достаётся другим, низкоприоритетные задачи вытесняются. Помогают справедливые блокировки и ограничение длительности критических секций.

---

## 6. Синхронизаторы

`[ ]` **Короткий ответ:** `CountDownLatch` — одноразово ждать N событий. `CyclicBarrier` — N потоков ждут друг друга, многоразовый. `Phaser` — гибкий барьер с меняющимся числом участников. `Semaphore` — ограничить число одновременных участников. `Exchanger` — два потока обмениваются значениями.

| Класс | Многоразовый | Кто ждёт | Пример |
|---|---|---|---|
| `CountDownLatch` | Нет | Один или несколько ждут, пока счётчик станет 0 | Дождаться запуска всех сервисов |
| `CyclicBarrier` | Да | Все участники ждут друг друга | Итерации параллельного вычисления |
| `Phaser` | Да | Участники регистрируются и уходят динамически | Многофазная обработка |
| `Semaphore` | Да | Ждут свободное разрешение | Не более 10 одновременных запросов к API |
| `Exchanger` | Да | Два потока в точке встречи | Обмен буферами производитель–потребитель |

**Semaphore не привязан к владельцу:** `release()` может вызвать любой поток, и разрешений может стать больше начального. В отличие от блокировки, он не реентерабелен.

<details>
<summary>Результат запуска: Semaphore(2)</summary>

20 виртуальных потоков проходят через `Semaphore(2)` с работой по 20 мс внутри. Максимальное одновременное число внутри — **2**.

```java
sem.acquire();
try { /* не более 2 потоков здесь */ }
finally { sem.release(); }
```

</details>

---

## 7. Потокобезопасные коллекции

### 7.1. ConcurrentHashMap

`[ ]` **Короткий ответ:** потокобезопасная хеш-таблица без глобальной блокировки: чтения почти не блокируются, записи блокируют только свою корзину. Отдельные операции атомарны, но последовательность `get` + `put` — нет. Для составных действий — атомарные методы `merge`, `compute`, `computeIfAbsent`, `putIfAbsent`. Ключи и значения `null` запрещены.

### Лабораторная 4. get + put против merge

<details>
<summary>Код и результат запуска</summary>

```java
// 8 потоков × 100 000 увеличений
Integer v = bad.get("k");
bad.put("k", v == null ? 1 : v + 1);   // гонка между get и put
good.merge("k", 1, Integer::sum);      // атомарно
```

```text
CHM get+put=520804 merge=800000
```

Потеряно около 35% увеличений, хотя каждая отдельная операция потокобезопасна.

</details>

**Уточнение интервьюера:** «Почему `null` запрещён?» — в многопоточном коде нельзя отличить «ключа нет» от «значение `null»`: между `get` и `containsKey` состояние могло измениться.

`computeIfAbsent` держит блокировку корзины, пока вычисляется значение. Долгая функция блокирует другие записи в ту же корзину, а изменение той же карты внутри функции запрещено.

### 7.2. Остальные коллекции

| Обычная | Потокобезопасная | Особенности |
|---|---|---|
| `ArrayList` | `CopyOnWriteArrayList` | Каждая запись копирует массив; итератор видит снимок. Для редких записей и частых обходов (слушатели) |
| `HashSet` | `ConcurrentHashMap.newKeySet()` | Множество на основе CHM |
| `Set` | `CopyOnWriteArraySet` | Как `CopyOnWriteArrayList` |
| `TreeMap` | `ConcurrentSkipListMap` | Сортированная, O(log n), без блокировок |
| `TreeSet` | `ConcurrentSkipListSet` | То же для множества |

`Collections.synchronizedList(...)` блокирует каждую операцию одним монитором, но обход итератором нужно синхронизировать вручную.

### 7.3. Очереди

`BlockingQueue` — труба между производителями и потребителями: `put` ждёт, если очередь полна, `take` — если пуста. Варианты с таймаутом: `offer(e, timeout)`, `poll(timeout)`.

| Очередь | Ограничена | Особенности |
|---|---|---|
| `ArrayBlockingQueue` | Да, обязательно | Массив, одна блокировка |
| `LinkedBlockingQueue` | Опционально (по умолчанию `Integer.MAX_VALUE`) | Связный список, две блокировки (голова и хвост) |
| `PriorityBlockingQueue` | Нет | Порядок по компаратору, а не FIFO |
| `DelayQueue` | Нет | Элемент доступен только после истечения задержки |
| `SynchronousQueue` | Ёмкость 0 | Передача из рук в руки: `put` ждёт `take` |
| `LinkedTransferQueue` | Нет | `transfer` ждёт, пока потребитель заберёт элемент |
| `ConcurrentLinkedQueue` | Нет | Неблокирующая (CAS), без ожидания |

---

## 8. Executors и пулы потоков

### 8.1. Зачем пул

`[ ]` **Короткий ответ:** пул переиспользует потоки (создание платформенного потока дорого), ограничивает их число и отделяет задачи от способа выполнения. Настраивать нужно не только число потоков, но и ёмкость очереди, политику отклонения, таймауты и метрики.

| Фабрика | Что внутри | Риск |
|---|---|---|
| `newFixedThreadPool(n)` | `n` потоков, **неограниченная** `LinkedBlockingQueue` | Очередь растёт до OOM, перегрузка скрыта |
| `newCachedThreadPool()` | 0..`Integer.MAX_VALUE` потоков, `SynchronousQueue` | Тысячи потоков при всплеске |
| `newSingleThreadExecutor()` | 1 поток, неограниченная очередь | Последовательное выполнение |
| `newScheduledThreadPool(n)` | Отложенные и периодические задачи | Исключение в периодической задаче отменяет её дальнейшие запуски |
| `newWorkStealingPool()` | `ForkJoinPool` | Для разбиваемых CPU-задач |
| `newVirtualThreadPerTaskExecutor()` | Новый виртуальный поток на задачу (JDK 21+) | Нет ограничения параллелизма — его ставят отдельно |

### 8.2. ThreadPoolExecutor и очередь

`[ ]` **Короткий ответ:** порядок приёма задачи: пока потоков меньше `corePoolSize` — создаётся поток; иначе задача ставится в очередь; если очередь полна и потоков меньше `maximumPoolSize` — создаётся ещё поток; иначе срабатывает политика отклонения. Следствие: при неограниченной очереди `maximumPoolSize` никогда не используется.

### Лабораторная 5. Очередь и отклонение

<details>
<summary>Код и результат запуска</summary>

```java
// core=1, max=2, очередь на 1 задачу, 5 задач по 200 мс
ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1));
// задача 1 -> core-поток, 2 -> очередь, 3 -> второй поток, 4 и 5 -> RejectedExecutionException

// core=1, max=10, неограниченная очередь
ThreadPoolExecutor wrong = new ThreadPoolExecutor(1, 10, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
```

```text
pool core=1 max=2 queue=1, 5 tasks -> rejected=2
unbounded queue core=1 max=10 -> poolSize=1 queued=4
```

Во втором пуле `max=10` не помог: все задачи ушли в очередь, работает один поток.

</details>

Политики отклонения: `AbortPolicy` (по умолчанию, исключение), `CallerRunsPolicy` (выполнить в вызывающем потоке — естественное обратное давление), `DiscardPolicy`, `DiscardOldestPolicy` (молча теряют задачи — опасно).

Пулы нужно закрывать: `shutdown()` + `awaitTermination()`, или `try-with-resources` — `ExecutorService` реализует `AutoCloseable` с JDK 19.

### 8.3. Размер пула

`[ ]` **Короткий ответ:** зависит от профиля задач. Для CPU-задач — около числа ядер (`Runtime.getRuntime().availableProcessors()`). Для задач с ожиданием — больше: оценка \(N_{threads} = N_{cpu} \times (1 + \frac{T_{wait}}{T_{compute}})\). Но верхний предел часто задаёт не CPU, а нижняя система: нет смысла в 200 потоках при пуле соединений к БД на 20.

Формулы — только отправная точка. Итоговый размер выбирают нагрузочным тестом по задержке и утилизации. В контейнере `availableProcessors()` учитывает CPU-лимиты cgroup.

### 8.4. Исключения в пуле

`[ ]` **Короткий ответ:** `execute(task)` — исключение уходит в `UncaughtExceptionHandler` потока и печатается. `submit(task)` — исключение сохраняется в `Future` и появится только при `get()` как `ExecutionException`. Если `get()` никто не вызвал, ошибка пропала молча.

```text
submit -> get(): java.lang.IllegalStateException: boom
```

---

## 9. CompletableFuture

`[ ]` **Короткий ответ:** `CompletableFuture` позволяет строить конвейер асинхронных шагов без блокировки: `thenApply` (преобразовать), `thenCompose` (следующий асинхронный шаг), `thenCombine` (объединить два), `exceptionally` / `handle` (ошибки), `allOf` / `anyOf`. Без явного исполнителя асинхронные методы используют `ForkJoinPool.commonPool()` — для блокирующего ввода-вывода передавайте свой.

### Лабораторная 6. Где выполняются колбэки и как идут ошибки

<details>
<summary>Результаты запуска</summary>

| Код | Результат |
|---|---|
| `completedFuture(1).thenApply(x -> threadName)` | `main` — уже завершённый future выполняет не-`Async` колбэк в вызывающем потоке |
| `completedFuture(1).thenApplyAsync(...)` | `ForkJoinPool.commonPool-worker-...` |
| `supplyAsync(throw IAE).thenApply(...).exceptionally(e -> ...)` | `e` — `CompletionException` с причиной `IllegalArgumentException`; `thenApply` пропущен |
| `failedFuture(ex).join()` | `CompletionException` (непроверяемое) |
| `failedFuture(ex).get()` | `ExecutionException` (проверяемое) |
| `allOf(f1, f2).join()` | `null` — результаты нужно брать из самих `f1`, `f2` |
| `new CompletableFuture<>().completeOnTimeout("default", 100, MILLISECONDS)` | `default` |

</details>

<details>
<summary>Типичный конвейер</summary>

```java
ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();

CompletableFuture<User> user = CompletableFuture.supplyAsync(() -> userClient.get(id), io);
CompletableFuture<List<Order>> orders = CompletableFuture.supplyAsync(() -> orderClient.list(id), io);

Profile profile = user
    .thenCombine(orders, Profile::new)                  // параллельно, затем объединить
    .orTimeout(2, TimeUnit.SECONDS)                     // TimeoutException по истечении
    .exceptionally(e -> Profile.empty())                // деградация
    .join();
```

Нюансы:

- `thenApply` с функцией, возвращающей `CompletableFuture`, даст `CompletableFuture<CompletableFuture<T>>`; нужен `thenCompose`.
- `cancel(true)` не прерывает уже выполняющуюся задачу — `CompletableFuture` не связан с потоком.
- Если одна из веток упала, вторая продолжит работать — автоматической отмены нет. Это решает Structured Concurrency (раздел 12).

</details>

---

## 10. Fork/Join

`[ ]` **Короткий ответ:** `ForkJoinPool` предназначен для рекурсивных CPU-задач «разделяй и властвуй». Задача делится (`fork`), подзадачи выполняются параллельно, результаты объединяются (`join`). Каждый поток имеет свою двустороннюю очередь; свободные потоки забирают работу у занятых (work stealing). Блокирующий ввод-вывод в нём занимает рабочие потоки и снижает параллелизм.

- `RecursiveTask<T>` — с результатом, `RecursiveAction` — без. Переопределяется `compute()`.
- Подзадачи должны быть независимы; маленькие куски считают последовательно (порог), иначе накладные расходы съедят выигрыш.
- `parallelStream()` и асинхронные методы `CompletableFuture` без исполнителя используют общий `commonPool` — одна блокирующая задача там влияет на всё приложение.

```java
final class SumTask extends RecursiveTask<Long> {
    private final long[] a; private final int from, to;
    SumTask(long[] a, int from, int to) { this.a = a; this.from = from; this.to = to; }
    @Override protected Long compute() {
        if (to - from <= 10_000) { long s = 0; for (int i = from; i < to; i++) s += a[i]; return s; }
        int mid = (from + to) >>> 1;
        SumTask left = new SumTask(a, from, mid);
        left.fork();                                        // в очередь
        return new SumTask(a, mid, to).compute() + left.join(); // правую считаем сами
    }
}
```

---

## 11. Виртуальные потоки

`[ ]` **Короткий ответ:** виртуальный поток — лёгкий поток, которым управляет JVM, а не ОС (финальная функция с JDK 21). Когда он блокируется на вводе-выводе или `sleep`, JVM снимает его с потока-носителя (carrier), и носитель выполняет другой. Можно держать сотни тысяч одновременных блокирующих задач и писать простой последовательный код. Виртуальные потоки не ускоряют CPU-работу и не отменяют ограничения нижних систем: пул соединений к БД всё равно на 20.

### Лабораторная 7. 10 000 задач по секунде сна

<details>
<summary>Код и результат запуска</summary>

```java
try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) exec.submit(() -> { Thread.sleep(1000); return 0; });
}   // close() ждёт завершения всех задач
```

```text
10000 virtual threads sleeping 1s: 1.1s
```

На машине с 2 ядрами. Пулу из 200 платформенных потоков понадобилось бы около 50 секунд.

</details>

### 11.1. Как применять

- Не объединять виртуальные потоки в пул: они дешёвые, создавайте новый на задачу.
- Ограничивать параллелизм к ресурсу семафором или пулом соединений, а не размером пула потоков.
- `ThreadLocal` работает, но при миллионе потоков миллион копий тяжёлых объектов дорог. Для передачи контекста — `ScopedValue`.
- Подходят для задач с ожиданием (HTTP, БД). Для CPU-задач выигрыша нет.
- В Spring Boot включаются свойством `spring.threads.virtual.enabled=true`.

### 11.2. Пиннинг

`[ ]` **Короткий ответ:** пиннинг — ситуация, когда виртуальный поток при блокировке не может освободить носитель и занимает его. До JDK 24 главная причина — блокировка внутри `synchronized`. JEP 491 в JDK 24 это исправил: виртуальные потоки теперь захватывают, освобождают мониторы и ждут на них независимо от носителя ([JEP 491](https://openjdk.org/jeps/491)). Пиннинг остаётся при вызове нативного кода и в некоторых редких случаях (например, блокировка при инициализации класса).

<details>
<summary>Результат запуска: sleep внутри synchronized на JDK 25</summary>

```java
try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1000; i++) exec.submit(() -> {
        Object own = new Object();
        synchronized (own) { Thread.sleep(500); }
        return 0;
    });
}
```

```text
1000 VT sleeping 0.5s inside synchronized: 0.5s (carriers=2)
```

На JDK 21 при двух носителях это заняло бы около 250 секунд (1000 / 2 × 0,5 с) — каждый виртуальный поток держал бы носитель полсекунды. Совет «заменить `synchronized` на `ReentrantLock` ради виртуальных потоков» для JDK 24+ устарел.

</details>

---

## 12. Structured Concurrency и Scoped Values

`[ ]` **Короткий ответ:** Structured Concurrency связывает время жизни подзадач с блоком кода: подзадачи создаются внутри области (`StructuredTaskScope`) и не могут её пережить. Если одна подзадача упала, остальные отменяются, а ошибка поднимается в родителя. В JDK 25 это всё ещё preview (пятый, JEP 505). Scoped Values (JEP 506) — финальная функция JDK 25: неизменяемое значение, привязанное к области выполнения, замена `ThreadLocal` для передачи контекста ([OpenJDK: JDK 25](https://openjdk.org/projects/jdk/25/)).

<details>
<summary>Код и результат запуска (java --enable-preview --source 25 Sv.java)</summary>

```java
static final ScopedValue<String> USER = ScopedValue.newInstance();

ScopedValue.where(USER, "alice").run(() -> System.out.println("inside: " + USER.get()));
System.out.println("outside bound: " + USER.isBound());

try (var scope = StructuredTaskScope.open()) {         // API JDK 25: open(), а не new ShutdownOnFailure()
    var u = scope.fork(() -> { Thread.sleep(100); return "user"; });
    var o = scope.fork(() -> { Thread.sleep(150); return "orders"; });
    scope.join();
    System.out.println("structured: " + u.get() + "+" + o.get());
}

try (var scope = StructuredTaskScope.open()) {
    var slow = scope.fork(() -> { Thread.sleep(5000); return "slow"; });
    scope.fork(() -> { throw new IllegalStateException("fail"); });
    scope.join();                                      // бросит FailedException
}
```

```text
inside: alice
outside bound: false
structured: user+orders
failed fast in 4 ms: java.lang.IllegalStateException: fail, slow state=UNAVAILABLE
```

Медленная подзадача на 5 секунд была отменена через 4 мс после ошибки соседней. С `CompletableFuture` она продолжила бы работать.

API preview менялся между версиями: в JDK 21–24 использовались `new StructuredTaskScope.ShutdownOnFailure()` и `throwIfFailed()`. Уточняйте версию, если речь заходит на интервью.

</details>

---

## 13. ThreadLocal

`[ ]` **Короткий ответ:** `ThreadLocal` хранит отдельное значение для каждого потока. Сам по себе он не делает общие данные потокобезопасными. В пуле потоки переиспользуются, поэтому значение, не очищенное через `remove()`, утечёт в следующую задачу — чужой пользователь, чужая транзакция. Очищать в `finally`.

<details>
<summary>Результат запуска: утечка между задачами</summary>

```java
ThreadLocal<String> user = new ThreadLocal<>();
ExecutorService single = Executors.newSingleThreadExecutor();
single.submit(() -> user.set("alice")).get();          // задача 1 забыла remove()
System.out.println(single.submit(() -> user.get()).get());   // задача 2
```

```text
ThreadLocal in next task: alice
```

Правильно:

```java
user.set(currentUser);
try { handle(request); }
finally { user.remove(); }
```

</details>

- Значение не передаётся в дочерние потоки и асинхронные задачи. `InheritableThreadLocal` копирует при создании потока, но в пуле потоки создаются заранее — копирование не срабатывает. Нужен явный перенос контекста (декоратор задач, `ScopedValue`, механизмы трассировки).
- Утечка памяти: в серверах приложений поток пула переживает приложение, и `ThreadLocal` удерживает загрузчик классов.

---

## 14. Пробное собеседование

Сначала ответьте вслух, потом откройте ответ.

<details>
<summary>1. Есть синглтон-кеш на HashMap, его читают и пишут 50 потоков. Что сломается и как исправить?</summary>

`HashMap` не потокобезопасен: потерянные записи, неконсистентные чтения при изменении размера, в старых версиях — зацикливание при resize. Исправление — `ConcurrentHashMap` и атомарные `computeIfAbsent`/`merge` для составных операций. Если нужны TTL и ограничение размера — готовый кеш (Caffeine).

</details>

<details>
<summary>2. Сервис под нагрузкой перестал отвечать, CPU 0%. Что смотреть?</summary>

Thread dump (`jcmd <pid> Thread.print`, несколько раз с интервалом): все потоки пула в `WAITING` на получении соединения из пула БД? В `BLOCKED` на одном мониторе? Deadlock в конце дампа? Потоки висят в сетевом вызове без таймаута? Далее — метрики пулов (активные, ожидающие), таймауты на внешние вызовы.

</details>

<details>
<summary>3. Нужно вызвать 3 внешних сервиса параллельно и вернуть ответ за 1 секунду. Как?</summary>

`CompletableFuture.supplyAsync(..., executor)` для каждого с отдельным исполнителем для ввода-вывода (не `commonPool`), `orTimeout` или `completeOnTimeout` для каждого, объединение через `thenCombine` или `allOf`, `exceptionally` для деградации. Таймауты также на уровне HTTP-клиента — иначе потоки продолжат ждать. На JDK 25 с preview — `StructuredTaskScope` с автоматической отменой остальных при ошибке.

</details>

<details>
<summary>4. Перешли на виртуальные потоки — и БД начала отказывать в соединениях. Почему?</summary>

Раньше пул на 200 потоков неявно ограничивал параллелизм. Теперь каждый запрос в своём виртуальном потоке, и 10 000 одновременных запросов пытаются получить соединение. Ограничивать нужно явно: пул соединений с таймаутом ожидания, `Semaphore` перед вызовом, rate limiting на входе.

</details>

<details>
<summary>5. Чем wait/notify хуже BlockingQueue для producer-consumer?</summary>

С `wait`/`notify` легко ошибиться: забыть цикл `while` (ложные пробуждения), вызвать вне `synchronized`, использовать `notify` вместо `notifyAll` и разбудить не того. `BlockingQueue` уже реализует ожидание, ограничение размера и видимость; код сводится к `put` и `take`.

</details>

<details>
<summary>6. Как правильно остановить поток?</summary>

Через прерывание: `thread.interrupt()` или `future.cancel(true)`. Задача проверяет `Thread.currentThread().isInterrupted()` в цикле, а блокирующие методы бросают `InterruptedException`. Поймав её, нужно завершиться или восстановить флаг (`Thread.currentThread().interrupt()`), а не проглатывать. `Thread.stop()` не использовать: в современных JDK он бросает `UnsupportedOperationException`.

</details>

---

## 15. Источники

- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)
- [OpenJDK: JDK 25 — список JEP (Structured Concurrency Fifth Preview, Scoped Values)](https://openjdk.org/projects/jdk/25/)
- [OpenJDK: JDK 24](https://openjdk.org/projects/jdk/24/)
- [Inside Java: Performance Improvements in JDK 24](https://inside.java/2025/03/19/performance-improvements-in-jdk24/)
