# Java: executors, CompletableFuture и виртуальные потоки

[Карта конспектов](README.md)

## Главное

- **Пул** ограничивает число активных задач. Ограничивай также очередь и выбирай политику перегрузки.
- **CompletableFuture:** `thenApply` преобразует результат, `thenCompose` соединяет зависимые асинхронные шаги.
- **Виртуальные потоки** полезны при множестве ожидающих I/O-задач. Ограничивай обращения к конечным ресурсам отдельно.

**Код.** [Цепочка CompletableFuture](../cuncurrent/async/CompletableFutureDemo.java) · [Single, fixed и cached пулы](../cuncurrent/executor/PoolFactoriesDemo.java) · [Fork/Join](../cuncurrent/forkjoin/ForkJoinSumDemo.java) · [Виртуальные потоки](../cuncurrent/threads/VirtualThreadsDemo.jsh). [JMM и блокировки](Concurrent.md)

> Примеры проверены на **OpenJDK 25** (машина с 2 ядрами); результаты в лабораторных получены запуском. Числа в гонках данных меняются от запуска к запуску — важен не конкретный результат, а то, что он неверный.

## Навигация

- [Executors и пулы потоков](#8-executors-и-пулы-потоков)
- [CompletableFuture](#9-completablefuture)
- [Fork/Join](#10-forkjoin)
- [Виртуальные потоки](#11-виртуальные-потоки)
- [Structured Concurrency и Scoped Values](#12-structured-concurrency-и-scoped-values)
- [ThreadLocal](#13-threadlocal)
- [Источники в основном конспекте](Concurrent.md#15-источники)

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
