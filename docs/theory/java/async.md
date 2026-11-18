# Java: executors, CompletableFuture и виртуальные потоки

[Java](README.md)

## Главное

- **Пул** ограничивает число активных задач. Ограничивай также очередь и выбирай политику перегрузки.
- **CompletableFuture:** `thenApply` преобразует результат, `thenCompose` соединяет зависимые асинхронные шаги.
- **Виртуальные потоки** полезны при множестве ожидающих I/O-задач. Ограничивай обращения к конечным ресурсам отдельно.

**Код.** [Цепочка CompletableFuture](../../../src/main/java/ru/learn/cuncurrent/async/CompletableFutureDemo.java) · [Single, fixed и cached пулы](../../../src/main/java/ru/learn/cuncurrent/executor/PoolFactoriesDemo.java) · [Fork/Join](../../../src/main/java/ru/learn/cuncurrent/forkjoin/ForkJoinSumDemo.java) · [Виртуальные потоки](../../../src/main/java/ru/learn/cuncurrent/threads/VirtualThreadsDemo.jsh). [JMM и блокировки](concurrency.md)

> Результаты с замерами в лабораторных получены на **OpenJDK 25** (машина с 2 ядрами). Числа зависят от нагрузки и окружения. У примеров с API новее Java 17 указана версия.

## Навигация

- [Executors и пулы потоков](#8-executors-и-пулы-потоков)
- [CompletableFuture](#9-completablefuture)
- [Fork/Join](#10-forkjoin)
- [Виртуальные потоки](#11-виртуальные-потоки)
- [Structured Concurrency и Scoped Values](#12-structured-concurrency-и-scoped-values)
- [ThreadLocal](#13-threadlocal)
- [Самопроверка](#самопроверка)
- [Источники в основном конспекте](concurrency.md#15-источники)

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

Политики отклонения: `AbortPolicy` (по умолчанию, исключение), `CallerRunsPolicy` (выполнить в вызывающем потоке, пока executor работает), `DiscardPolicy` и `DiscardOldestPolicy` (могут молча отбросить задачу).

Пулы нужно закрывать: `shutdown()` + `awaitTermination()`, или `try-with-resources` — `ExecutorService` реализует `AutoCloseable` с JDK 19.

**Что происходит в перегрузку.** Пусть HTTP-сервис принимает 300 задач/с, а его зависимости позволяют завершать только 200 задач/с. Если каждая принятая задача ждёт исполнения, очередь растёт примерно на 100 задач каждую секунду. Дополнительные 10 000 мест отложат отказ, а задержка растёт по мере накопления работы. Добавление worker-потоков не увеличит пропускную способность занятой БД.

Измеряй число активных worker, длину очереди, время от submit до старта, время исполнения и число отказов. Пусть в среднем выполняются или ждут 60 задач, а через систему стабильно проходит 200 задач/с. По закону Литтла среднее время внутри этой границы равно `60 / 200 = 0.3` с. Эта оценка относится к устойчивому режиму и средним значениям. Для выполнения дедлайнов смотри ещё и p95/p99.

**Нюанс CallerRunsPolicy.** Когда очередь заполнена, он может выполнить работу прямо в принимающем потоке. Для отдельного producer это обратное давление. Для event loop или потока Kafka consumer долгий блокирующий вызов задержит остальные операции и может нарушить их дедлайны. Для веб-пути обычно проще обработать явный отказ и вернуть оговорённый статус, для фоновой работы — оставить её в устойчивом хранилище для следующей попытки.

**Ловушка с вложенными задачами.** В пуле из двух потоков обе задачи отправили в него дочернюю задачу и вызвали `get()`. Дочерние задачи ждут в очереди, потому что оба worker заняты родителями. Увеличение пула перемещает границу проблемы. Объедини шаги через continuation без блокировки worker либо вынеси зависимые задачи в ресурс с независимой ёмкостью. После изменения проверь сценарий при заполненном пуле. [Java 17, ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html)

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

**Короткий ответ.** `CompletableFuture` представляет будущий результат и умеет запускать зависимые шаги. `thenApply` преобразует результат, `thenCompose` запускает следующий шаг, возвращающий future, `thenCombine` объединяет два результата. Async-методы без явного исполнителя обычно используют `ForkJoinPool.commonPool()`. Если параллелизм общего пула меньше двух, реализация создаёт отдельный поток на задачу. Для управления ресурсами блокирующего I/O передавай своего исполнителя.

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
// io — исполнитель, принадлежащий сервису. Сервис закрывает его при остановке.

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

### Кто выполняет продолжение и кто обрабатывает ошибку

Пусть `loadOrder(id)` возвращает future результата сетевого запроса. Если SDK завершает его на I/O-потоке, `thenApply(this::renderLargeReport)` может выполнить дорогой рендеринг на том же потоке. Если future уже завершён, продолжение может выполнить тот, кто его добавляет. Когда есть требования к ресурсу или задержке, укажи явный исполнитель через `thenApplyAsync(..., cpuExecutor)`. Лёгкой конвертации достаточно `thenApply`.

| Продолжение | Когда вызывается | Что делает с итогом |
| --- | --- | --- |
| `thenApply` | При успехе | Преобразует значение |
| `exceptionally` | При исключительном завершении | Может подставить запасной результат |
| `handle` | При успехе и при ошибке | Вычисляет новый результат из значения или исключения |
| `whenComplete` | При успехе и при ошибке | Удобен для метрик. Если действие прошло успешно, исходный итог сохраняется |

Помещай fallback рядом с той веткой, где он допустим. Например, отсутствие рекомендаций даёт пустой список, но обязательный запрос цены должен сигнализировать об ошибке. Если повесить `exceptionally` на общую цепочку и всегда возвращать пустой ответ, неисправность цены станет похожа на нормальный бизнес-результат.

### Три разных тайм-аута

1. `future.get(150, MILLISECONDS)` ограничивает ожидание вызывающего. После `TimeoutException` future по-прежнему может завершиться успехом.
2. `future.orTimeout(150, MILLISECONDS)` завершает **этот** `CompletableFuture` исключением, если время истекло первым. `completeOnTimeout` завершит его указанным значением. Оба метода меняют тот же объект future. Для индивидуального дедлайна не меняй объект, который используют несколько клиентов.
3. Тайм-аут HTTP-клиента, SQL или RPC ограничивает работу соответствующей зависимости по её контракту. Если удалённая операция совершила эффект до получения отмены, проверяй статус по ключу операции.

`allOf` завершается, когда завершились все входы, и даёт `Void`. Ошибка одной ветки не отменяет другие и не заставляет `allOf` закончиться сразу. `anyOf` принимает первый завершившийся исход, включая ошибку. Если нужен первый **успешный** ответ, заранее опиши условия, при которых игнорируешь ошибки и возвращаешь общий отказ.

`cancel(true)` у `CompletableFuture` помечает его отменённым, но параметр прерывания не управляет лежащим в основе вычислением. Для управления реальной работой нужен поддерживаемый сигнал отмены в исполнителе, HTTP-клиенте или коде задачи. `join()` удобно использовать при завершении конвейера, но оно блокирует вызывающего до результата. Если поток обязан реагировать на interrupt при ожидании, используй `get()` и обработай `InterruptedException`. [Java 17, CompletableFuture](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html)

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

### Разбор: меняем executor, а производительность БД та же

У сервиса пул JDBC на 20 соединений. При среднем времени удержания соединения 40 мс грубый верхний предел без других ограничений около `20 / 0.04 = 500` задач/с. После перехода на виртуальные потоки 5 000 запросов могут ждать соединения одновременно, но число одновременных SQL по-прежнему ограничено двадцатью. Измеряй ожидание соединения, длину очереди на входе и отмены по дедлайну. Ограничитель конкуренции имеет смысл ставить до дорогих шагов: запроса к БД, создания больших буферов, разветвления по нескольким зависимостям.

Виртуальные потоки упрощают императивный код с I/O, но завершение отменённых задач и ресурсный бюджет остаются частью дизайна. Например, один запрос может запустить десять зависимых вызовов. Лимит в 100 одновременных HTTP-запросов на входе в этом случае допускает до 1000 вызовов. Отдельный лимит на зависимость предотвращает её перегрузку. При выборе JDK проверь версию, потому что поведение мониторов при пиннинге изменилось в JDK 24.

[Oracle, виртуальные потоки в JDK 21](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)

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

## Самопроверка

<details>
<summary>Будет ли пул core=4, max=32 запускать 32 задачи при неограниченной очереди?</summary>

В типичной конфигурации после создания четырёх core-потоков новые задачи встанут в очередь. До роста к max дело доходит после отказа очереди принять задачу. Проверь число worker и время ожидания работы в очереди.

</details>

<details>
<summary>Почему p99 увеличился после перехода на CompletableFuture?</summary>

Проверь, где запущен каждый шаг, блокирует ли I/O общий пул и какая часть задержки приходится на очередь. Уточни, на какой ветке добавили fallback, что ограничивает попытки и завершается ли работа после тайм-аута клиента.

</details>

<details>
<summary>Вызов платежа обёрнут в orTimeout. Можно ли при TimeoutException повторять его с новым UUID?</summary>

Новый UUID может обозначить новую команду и повторить эффект. Future перестал ждать, но платёж мог пройти. Повторяй с исходным ключом согласно контракту провайдера и при необходимости сверяй состояние.

</details>

[Банк вопросов по конкурентности](../../interview/advanced/concurrency.md)

---
