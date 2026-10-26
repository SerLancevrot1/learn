# Spring Framework: контейнер, proxy и транзакции

> Конспект-тренажёр для подготовки к собеседованиям.
> Примеры проверены на **Spring Framework 7.0.10**, **OpenJDK 25** и H2 2.5 (без Spring Boot, чистый `AnnotationConfigApplicationContext`): жизненный цикл bean, CGLIB-proxy, self-invocation, правила rollback, propagation, `readOnly`, `@Async`, scope prototype, `@Retryable` и `@ConcurrencyLimit`. Вывод в разделах реальный, полный код — в [приложении](#приложение-код-лабораторных). Конфигурация Spring Boot не запускалась и помечена.

## Как пользоваться файлом

Каждая тема устроена одинаково:

1. **Короткий ответ** — то, что нужно сказать устно за 30–60 секунд.
2. **Подробности** — под спойлером `<details>`: механизм, ограничения, примеры.
3. **Уточняющие вопросы** — то, что интервьюер спросит следом.
4. **Практика** — код и реальный вывод.

Отмечайте уверенность прямо в файле:

- `[ ]` — не знаю;
- `[~]` — отвечаю с подсказкой;
- `[x]` — объясняю сам, с примером.

Повторяйте прежде всего то, что отмечено `[ ]` и `[~]`.

---

## Содержание

0. [Быстрое повторение](#0-быстрое-повторение)
1. [IoC-контейнер](#1-ioc-контейнер)
2. [Жизненный цикл bean](#2-жизненный-цикл-bean)
3. [Внедрение зависимостей и scope](#3-внедрение-зависимостей-и-scope)
4. [AOP и proxy](#4-aop-и-proxy)
5. [@Transactional: основы](#5-transactional-основы)
6. [Propagation и isolation](#6-propagation-и-isolation)
7. [@Async](#7-async)
8. [Resilience в Spring Framework 7](#8-resilience-в-spring-framework-7)
9. [Spring Boot: автоконфигурация](#9-spring-boot-автоконфигурация)
10. [Пробное собеседование](#10-пробное-собеседование)
11. [Источники](#11-источники)
- [Приложение: код лабораторных](#приложение-код-лабораторных)

---

## 0. Быстрое повторение

Ответьте вслух, не открывая разделы. Ответы — по ссылкам.

| Уверенность | Вопрос | Раздел |
|---|---|---|
| `[ ]` | Чем `ApplicationContext` отличается от `BeanFactory`? | [1](#1-ioc-контейнер) |
| `[ ]` | В каком порядке вызываются конструктор, `@PostConstruct`, `afterPropertiesSet`, `BeanPostProcessor`? | [2](#2-жизненный-цикл-bean) |
| `[ ]` | На каком шаге появляется proxy? | [2](#2-жизненный-цикл-bean) |
| `[ ]` | Почему constructor injection лучше field injection? | [3.1](#31-способы-внедрения) |
| `[ ]` | Что будет с prototype-bean, внедрённым в singleton? | [3.2](#32-scope) |
| `[ ]` | Почему singleton должен быть потокобезопасным? | [3.2](#32-scope) |
| `[ ]` | JDK proxy или CGLIB — когда что? | [4](#4-aop-и-proxy) |
| `[ ]` | Почему не сработал `@Transactional` при вызове из того же класса? | [4.2](#42-self-invocation) |
| `[ ]` | Работает ли `@Transactional` на `final`, `protected`, package-private методе? | [4.3](#43-какие-методы-проксируются) |
| `[ ]` | Откатится ли транзакция на checked exception? | [5.1](#51-правила-rollback) |
| `[ ]` | Запрещает ли `readOnly = true` запись? | [5.2](#52-readonly) |
| `[ ]` | `REQUIRED` и `REQUIRES_NEW` — разница? Откуда `UnexpectedRollbackException`? | [6.1](#61-propagation) |
| `[ ]` | Почему `@Async`-метод выполнился в вызывающем потоке? | [7](#7-async) |
| `[ ]` | Что нового в Spring 7 для ретраев? | [8](#8-resilience-в-spring-framework-7) |
| `[ ]` | Как работает автоконфигурация Spring Boot? | [9](#9-spring-boot-автоконфигурация) |

---

## 1. IoC-контейнер

`[ ]` **Короткий ответ:** Inversion of Control — объекты не создают свои зависимости сами, их создаёт и связывает контейнер. `BeanFactory` — базовый контейнер: создаёт bean по определениям и внедряет зависимости, по умолчанию лениво. `ApplicationContext` построен поверх `BeanFactory` и добавляет события, i18n, доступ к ресурсам, автоматическую регистрацию `BeanPostProcessor` и `BeanFactoryPostProcessor`, а singleton-bean создаёт заранее при `refresh()` — ошибки конфигурации видны при старте. В приложениях используют `ApplicationContext`.

<details>
<summary>Что такое BeanDefinition и BeanFactoryPostProcessor</summary>

- `BeanDefinition` — метаданные bean: класс, scope, способ создания (конструктор или фабричный метод), зависимости, lazy, init/destroy-методы. Создаются при чтении конфигурации — сканирование `@Component`, методы `@Bean`, программная регистрация (в Spring 7 появился новый API программной регистрации bean ([анонс Spring Framework 7.0](https://spring.io/blog/2025/11/13/spring-framework-7-0-general-availability/))).
- `BeanFactoryPostProcessor` работает с определениями до создания экземпляров. Пример — `PropertySourcesPlaceholderConfigurer`, подставляющий `${...}`, и `ConfigurationClassPostProcessor`, обрабатывающий `@Configuration`.
- `BeanPostProcessor` работает с уже созданными экземплярами — см. жизненный цикл.

</details>

---

## 2. Жизненный цикл bean

`[ ]` **Короткий ответ:** для singleton-bean: (1) конструктор, (2) внедрение зависимостей через сеттеры и поля, (3) `Aware`-интерфейсы (`BeanNameAware`, `ApplicationContextAware`), (4) `BeanPostProcessor#postProcessBeforeInitialization` — в нём стандартный процессор вызывает `@PostConstruct`, (5) `InitializingBean#afterPropertiesSet`, (6) `initMethod`, (7) `postProcessAfterInitialization` — здесь AOP-инфраструктура подменяет объект на proxy. При закрытии контекста — `@PreDestroy`, `DisposableBean#destroy`, `destroyMethod`.

**Практика.** Bean реализует все callback'и, отдельный `BeanPostProcessor` печатает свои шаги (реальный вывод Spring 7.0.10):

```text
[life] 1 constructor
[life] 2 dependency injection
[life] 3 BeanNameAware lifeBean
[life] 4 BPP before init
[life] 5 @PostConstruct
[life] 6 afterPropertiesSet
[life] 7 BPP after init (here proxies are created)
...
[life] 8 @PreDestroy
[life] 9 DisposableBean.destroy
```

Обратите внимание: «BPP before init» напечатан раньше `@PostConstruct`. Пользовательский процессор отработал первым, а `@PostConstruct` вызывается процессором `CommonAnnotationBeanPostProcessor` в той же фазе.

<details>
<summary>Оговорки к схеме</summary>

- Lazy-bean проходит цикл при первом запросе, а не при `refresh()`.
- Prototype-bean контейнер создаёт и инициализирует, но не уничтожает: destroy-callback'и для него не вызываются, освобождать ресурсы должен владелец.
- В `@PostConstruct` proxy ещё не создан: вызов `this.transactionalMethod()` из него идёт без транзакции. Для логики после полной готовности — `ApplicationRunner`, `SmartInitializingSingleton` или событие `ContextRefreshedEvent` (в Boot — `ApplicationReadyEvent`).
- Циклические зависимости через конструкторы невозможны и дают ошибку при старте; в Spring Boot циклы запрещены по умолчанию и для сеттеров и полей.

</details>

---

## 3. Внедрение зависимостей и scope

### 3.1. Способы внедрения

`[ ]` **Короткий ответ:** предпочтительно внедрение через конструктор: обязательные зависимости явны, поля можно сделать `final`, объект нельзя создать частично сконфигурированным, тест создаёт его обычным `new` без контейнера, цикл зависимостей обнаруживается при старте. Если конструктор у класса один, `@Autowired` не нужен. Сеттер — для необязательных зависимостей. Field injection прячет зависимости и требует рефлексии в тестах.

```java
@Service
class OrderService {
    private final OrderRepository repository;
    private final PaymentClient payments;

    OrderService(OrderRepository repository, PaymentClient payments) {
        this.repository = repository;
        this.payments = payments;
    }
}
```

Несколько кандидатов одного типа — `@Primary`, `@Qualifier("name")`, или внедрить `List<PaymentClient>` / `Map<String, PaymentClient>` со всеми реализациями.

### 3.2. Scope

| Scope | Экземпляр | Замечания |
|---|---|---|
| `singleton` (по умолчанию) | Один на `ApplicationContext` | Не GoF Singleton на JVM: два контекста — два экземпляра. Используется всеми потоками |
| `prototype` | Новый при каждом запросе из контейнера | Контейнер не вызывает destroy |
| `request`, `session` | На HTTP-запрос, на сессию | Внедрение в singleton — через scoped proxy |
| `application`, `websocket` | На `ServletContext`, на WebSocket-сессию | Редко |

**Практика.** Prototype внедрён в singleton через конструктор и через `ObjectProvider` (реальный вывод):

```text
== prototype injected into singleton same? true; via ObjectProvider same? false
```

Prototype, внедрённый в singleton, создаётся один раз — при создании singleton — и дальше это всё тот же объект. Новый экземпляр на каждое обращение дают `ObjectProvider<T>#getObject()`, `@Lookup`-метод или scoped proxy.

`[ ]` **Потокобезопасность singleton:** один экземпляр обслуживает параллельные запросы. Неизменяемые поля-зависимости безопасны; изменяемое состояние запроса (текущий пользователь, промежуточный результат) в поле singleton — гонка данных. Состояние запроса держат в локальных переменных, параметрах или request-scope.

---

## 4. AOP и proxy

### 4.1. JDK proxy и CGLIB

`[ ]` **Короткий ответ:** Spring AOP работает через proxy-объект, который контейнер подставляет вместо bean: вызов идёт в proxy, тот выполняет advice (открыть транзакцию, проверить права, отправить в executor) и вызывает настоящий объект. JDK dynamic proxy реализует интерфейсы bean; CGLIB создаёт подкласс. В Spring Boot по умолчанию используется CGLIB (`proxyTargetClass = true`), даже если интерфейс есть. На proxy построены `@Transactional`, `@Async`, `@Cacheable`, method security, `@Retryable`.

**Практика:**

```text
== proxy: true class=OrderService$$SpringCGLIB
```

<details>
<summary>Поле через proxy: неочевидная ловушка</summary>

CGLIB-proxy — подкласс со своими, неинициализированными полями. Advice перехватывает только вызовы методов, обращение к полю — нет:

```java
@Component
class FlakyClient {
    final AtomicInteger attempts = new AtomicInteger();
    public int attempts() { return attempts.get(); }
    @Retryable(maxRetries = 3, delay = 50) public String call() { ... }
}

FlakyClient flaky = ctx.getBean(FlakyClient.class);   // это proxy
flaky.attempts      // null — поле самого proxy
flaky.attempts()    // 3   — метод делегирован настоящему объекту
```

Реальный вывод: `== field through proxy: null`, затем `attempts=3`. Вывод: к bean обращаемся только через методы.

</details>

### 4.2. Self-invocation

`[ ]` **Короткий ответ:** вызов `this.method()` внутри bean идёт напрямую в объект, минуя proxy, поэтому advice не срабатывает: нет транзакции, нет асинхронности, нет кеша. Решение — вынести метод в отдельный bean, перенести аннотацию на внешний метод, либо в крайнем случае внедрить себя (self-injection) или использовать `TransactionTemplate` для явного управления.

**Практика:**

```java
@Transactional public boolean txActiveDirect() { return TransactionSynchronizationManager.isActualTransactionActive(); }
public boolean callInternally() { return txActiveDirect(); }    // this.txActiveDirect()
```

```text
== tx via proxy: true
== self-invocation: false
```

### 4.3. Какие методы проксируются

**Практика.** `@Transactional` на методах разной видимости в CGLIB-proxy, Spring 7.0.10 (реальный вывод):

```text
WARNING: Public final method [public final boolean demo2.Svc.fin()] cannot get proxied via CGLIB,
         consider removing the final marker or using interface-based JDK proxies.
public          tx=true
package-private tx=true
protected       tx=true
final public    tx=false
```

- `final` метод нельзя переопределить в подклассе — advice молча не применяется, есть только предупреждение в логе при старте. Это важно для Kotlin, где классы и методы `final` по умолчанию (плагин `kotlin-spring` их открывает).
- `protected` и package-private методы с CGLIB-proxy транзакционны начиная с Spring 6.0. Утверждение «`@Transactional` работает только на `public`» устарело; для JDK-proxy это по-прежнему так, потому что он видит только методы интерфейса.
- `private` методы не проксируются никогда.

**Уточняющие вопросы:**

- Если advice не сработал, что проверить? Создан ли объект контейнером (а не через `new`); вызывается ли через proxy (не self-invocation, не из `@PostConstruct`); не `final` или `private` ли метод; включена ли инфраструктура (`@EnableTransactionManagement`, `@EnableAsync` — в Boot часть включена автоматически).
- Spring AOP или AspectJ? Spring AOP — proxy во время выполнения, только вызовы методов bean извне. AspectJ — вплетение в байткод на этапе компиляции или загрузки, работает и для self-invocation, и для полей, и для объектов вне контейнера; сложнее в сборке.

---

## 5. @Transactional: основы

`[ ]` **Короткий ответ:** proxy перед вызовом метода получает транзакцию у `PlatformTransactionManager` (для JDBC — берёт соединение из пула и выключает auto-commit) и привязывает её к текущему потоку через `ThreadLocal`. Весь код в этом потоке, использующий тот же `DataSource` через Spring (`JdbcTemplate`, JPA), работает в этой транзакции. При нормальном выходе — commit, при исключении — rollback по правилам ниже. Поскольку транзакция привязана к потоку, код в другом потоке (`@Async`, `CompletableFuture.supplyAsync`, parallel stream) в неё не входит. В реактивном стеке контекст транзакции передаётся через Reactor Context, а не `ThreadLocal`.

### 5.1. Правила rollback

`[ ]` **Короткий ответ:** по умолчанию rollback выполняется на `RuntimeException` и `Error`; checked exception приводит к commit. Это наследие EJB: checked exception считалось ожидаемым бизнес-результатом. Поменять — `rollbackFor = Exception.class` или `noRollbackFor`. Исключение должно выйти за пределы proxy: если поймать его внутри метода и не пробросить, proxy увидит нормальный выход и сделает commit.

**Практика.** Метод вставляет строку и бросает исключение (реальный вывод, 0 — откат, 1 — строка сохранилась):

```java
@Transactional public void insertThenRuntime(int id) { insert(id); throw new IllegalStateException(); }
@Transactional public void insertThenChecked(int id) throws Exception { insert(id); throw new Exception("checked"); }
@Transactional(rollbackFor = Exception.class)
public void insertThenCheckedRollbackFor(int id) throws Exception { insert(id); throw new Exception("checked"); }
```

```text
== RuntimeException -> rows id=1: 0
== checked Exception -> rows id=2: 1      ← исключение вылетело, а данные закоммичены
== checked + rollbackFor -> rows id=3: 0
```

### 5.2. readOnly

`[ ]` **Короткий ответ:** `readOnly = true` — подсказка, а не запрет. Hibernate не делает dirty checking и flush, драйвер может направить запрос на реплику, некоторые БД включают режим только чтения. Но гарантии запрета записи нет — поведение зависит от transaction manager, драйвера и БД.

**Практика.** `DataSourceTransactionManager` + H2, `INSERT` внутри `readOnly`-транзакции (реальный вывод):

```text
readOnly        readOnlyFlag=true
readOnly + INSERT: прошло, строк=1
```

Флаг выставлен, но H2 запись выполнил. С PostgreSQL драйвер по умолчанию открывает такую транзакцию как `READ ONLY`, и `INSERT` завершится ошибкой `cannot execute INSERT in a read-only transaction` (с PostgreSQL не запускалось). Одна и та же аннотация ведёт себя по-разному в зависимости от БД и драйвера — поэтому на `readOnly` как на защиту полагаться нельзя.

### 5.3. Типичные ошибки

- Долгий сетевой вызов внутри транзакции: соединение из пула и блокировки строк удерживаются всё время вызова. При медленном внешнем API пул исчерпывается, и падает весь сервис. Внешние вызовы — вне транзакции.
- Ловить исключение внутри `@Transactional`-метода и «глотать» его — commit частично выполненной работы.
- Отправлять событие в Kafka или письмо внутри транзакции: транзакция может откатиться после отправки. Нужен outbox или `@TransactionalEventListener(phase = AFTER_COMMIT)` с пониманием, что падение после commit событие потеряет.
- Локальная `@Transactional` не охватывает несколько сервисов: для распределённой операции — outbox, идемпотентность, saga.

---

## 6. Propagation и isolation

### 6.1. Propagation

`[ ]` **Короткий ответ:** propagation определяет, что делать, если транзакция уже есть. `REQUIRED` (по умолчанию) — присоединиться к существующей или создать новую. `REQUIRES_NEW` — приостановить текущую и открыть независимую: её commit не зависит от внешней (нужно второе соединение из пула). `NESTED` — savepoint внутри текущей. Остальные: `SUPPORTS`, `NOT_SUPPORTED`, `MANDATORY`, `NEVER`.

| Propagation | Есть транзакция | Нет транзакции |
|---|---|---|
| `REQUIRED` | Присоединиться | Создать |
| `REQUIRES_NEW` | Приостановить и создать новую | Создать |
| `NESTED` | Savepoint (JDBC) | Создать |
| `SUPPORTS` | Присоединиться | Без транзакции |
| `NOT_SUPPORTED` | Приостановить, выполнить без | Без транзакции |
| `MANDATORY` | Присоединиться | Исключение |
| `NEVER` | Исключение | Без транзакции |

**Практика.** Два сценария (реальный вывод):

```java
// 1. внешний метод падает, аудит записан в REQUIRES_NEW
@Transactional public void outerWithInnerRequiresNew(int id) {
    insert(id);
    audit.logNewTx(1000 + id);          // @Transactional(propagation = REQUIRES_NEW)
    throw new IllegalStateException();
}

// 2. внутренний REQUIRED падает, внешний ловит исключение и пытается продолжить
@Transactional public void outerCatchesInnerRequired(int id) {
    insert(id);
    try { audit.failRequired(); } catch (RuntimeException ignored) {}   // @Transactional (REQUIRED)
}
```

```text
== outer rolled back, REQUIRES_NEW audit kept: orders=0 audit=1
== inner REQUIRED failed, outer caught -> UnexpectedRollbackException
```

Второй случай — частый вопрос. Внутренний метод участвует в той же физической транзакции; когда исключение проходит через его proxy, транзакция помечается rollback-only. Внешний метод поймал исключение и завершился нормально, пытается commit — и получает `UnexpectedRollbackException`: Spring сообщает, что «успешный» метод на самом деле откатился. Если нужно, чтобы ошибка внутренней части не влияла на внешнюю, — `REQUIRES_NEW` или `NESTED`, либо не ставить `@Transactional` на внутренний метод.

<details>
<summary>Цена REQUIRES_NEW</summary>

- Второе соединение из пула на время внутреннего вызова. При пуле 10 и 10 параллельных запросах, каждый из которых держит внешнее соединение и ждёт второе, — взаимная блокировка на пуле (pool deadlock).
- Внутренняя транзакция не видит незакоммиченных изменений внешней и может заблокироваться на строках, которые держит внешняя.
- Подходит для аудита и журналирования попыток, которые должны сохраниться даже при откате основной операции.

</details>

### 6.2. Isolation

`isolation` в `@Transactional` выставляет уровень изоляции БД на время транзакции (`READ_COMMITTED`, `REPEATABLE_READ`, `SERIALIZABLE`); по умолчанию — уровень БД (в PostgreSQL — Read Committed). Аномалии и поведение уровней — в DataBases.md. При присоединении к существующей транзакции уровень не меняется.

---

## 7. @Async

`[ ]` **Короткий ответ:** `@Async` через proxy отправляет вызов в `TaskExecutor` и сразу возвращает управление. Возвращаемый тип — `void` или `CompletableFuture<T>`/`Future<T>`. Self-invocation выполняет метод синхронно в вызывающем потоке. Транзакция и `SecurityContext` вызывающего потока в асинхронный метод не переходят. Executor нужно настроить явно: ограниченный пул, ограниченная очередь, политика отказа, имена потоков.

**Практика.** Без явно объявленного executor (реальный вывод, чистый Spring без Boot):

```text
INFO: No task executor bean found for async processing: no bean of type TaskExecutor and no bean named 'taskExecutor' either
== @Async via proxy thread: SimpleAsyncTaskExecutor-N
== @Async self-invocation thread: main
```

Без своего executor чистый Spring использует `SimpleAsyncTaskExecutor`, который создаёт новый поток на каждую задачу без ограничения — под нагрузкой это исчерпание памяти и потоков. Spring Boot автоматически создаёт `ThreadPoolTaskExecutor` (или executor на виртуальных потоках при `spring.threads.virtual.enabled=true`), но очередь по умолчанию у него неограниченная — настройте `spring.task.execution.pool.queue-capacity`.

```java
@Bean(name = "taskExecutor")
ThreadPoolTaskExecutor taskExecutor() {
    var ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(8);
    ex.setMaxPoolSize(16);
    ex.setQueueCapacity(500);
    ex.setThreadNamePrefix("async-");
    ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    ex.setTaskDecorator(r -> r);   // сюда — перенос MDC и контекста трассировки
    return ex;
}
```

- Исключение из `@Async void` не попадает к вызывающему: настройте `AsyncUncaughtExceptionHandler`, иначе ошибка будет только в логе.
- Результат `CompletableFuture` нужно наблюдать: незавершённые или упавшие future без обработки — тихие ошибки.
- Асинхронность не отменяет лимиты БД и внешних сервисов.

---

## 8. Resilience в Spring Framework 7

`[ ]` **Короткий ответ:** начиная с Spring Framework 7.0 (GA ноябрь 2025) в ядре есть `@Retryable` и `@ConcurrencyLimit` для вызовов методов, а также программный `RetryTemplate` ([Spring Framework: Resilience Features](https://docs.spring.io/spring-framework/reference/core/resilience.html), [анонс Spring Framework 7.0](https://spring.io/blog/2025/11/13/spring-framework-7-0-general-availability/)). Включаются через `@EnableResilientMethods`, без него аннотации игнорируются ([Baeldung](https://www.baeldung.com/spring-boot-4-spring-framework-7)). По умолчанию `@Retryable` повторяет при любом исключении до 3 раз после первой неудачи с задержкой 1 секунда; если попытки исчерпаны, вызывающий получает последнее исключение. Отдельная зависимость Spring Retry для простых случаев больше не нужна; circuit breaker в ядре нет — для него остаётся Resilience4j.

**Практика.** Метод падает дважды, затем успешен; `@ConcurrencyLimit(2)` вызывают из 10 виртуальных потоков (реальный вывод):

```java
@Retryable(maxRetries = 3, delay = 50)
public String call() { if (attempts.incrementAndGet() < 3) throw new IllegalStateException("temporary"); return "ok"; }

@ConcurrencyLimit(2)
public void work() { /* считаем одновременно выполняющих */ }
```

```text
== @Retryable result: ok attempts=3
== @ConcurrencyLimit(2) max parallel: 2
```

<details>
<summary>Что спросят про ретраи</summary>

- Повторять можно только идемпотентные операции или операции с ключом идемпотентности: повтор `POST /payments` после таймаута может списать деньги дважды.
- Нужны backoff и jitter (`multiplier`, `jitter` в аннотации): синхронные повторы от всех клиентов одновременно добивают восстанавливающийся сервис.
- Ограничивайте, на каких исключениях повторять (`includes`, `excludes`): ошибка валидации не исправится от повтора.
- `@Retryable` вместе с `@Transactional`: порядок proxy определяет, повторяется ли вся транзакция или только вызов внутри неё. Повтор внутри откатившейся транзакции бесполезен — retry должен быть снаружи транзакции.
- `@ConcurrencyLimit` — bulkhead: ограничивает параллельный доступ к ресурсу внутри одного экземпляра; это не rate limit по времени и не распределённое ограничение.

</details>

---

## 9. Spring Boot: автоконфигурация

> Раздел не запускался: лаборатория собрана на чистом Spring Framework.

`[ ]` **Короткий ответ:** `@SpringBootApplication` = `@Configuration` + `@ComponentScan` + `@EnableAutoConfiguration`. Автоконфигурация — набор `@AutoConfiguration`-классов, перечисленных в `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` внутри стартеров. Каждый класс защищён условиями: `@ConditionalOnClass` (есть ли библиотека на classpath), `@ConditionalOnMissingBean` (не объявил ли пользователь свой bean), `@ConditionalOnProperty`. Поэтому объявленный вами bean побеждает автоматический. Посмотреть, что применилось и почему, — `--debug` (condition evaluation report) или эндпоинт `/actuator/conditions`.

- Порядок источников свойств: аргументы командной строки и переменные окружения перекрывают `application.yml`; профили (`application-prod.yml`) перекрывают базовый файл.
- `@ConfigurationProperties` — типизированная привязка группы свойств с валидацией; лучше разрозненных `@Value`.
- Spring Boot 4 построен на Spring Framework 7 и Jackson 3 ([Baeldung](https://www.baeldung.com/spring-boot-4-spring-framework-7)).

---

## 10. Пробное собеседование

<details>
<summary>1. Метод сервиса помечен @Transactional, но данные сохраняются частично. Почему?</summary>

Варианты: вызов из того же класса (self-invocation) — транзакции нет вовсе; метод `final` или `private`; исключение checked и без `rollbackFor` — commit; исключение поймано внутри метода и не проброшено; работа выполнялась в другом потоке; использован другой `DataSource` или клиент вне Spring-транзакции. Проверка: `TransactionSynchronizationManager.isActualTransactionActive()` в нужной точке, логи `org.springframework.transaction` на DEBUG.

</details>

<details>
<summary>2. Метод сохраняет заказ и вызывает платёжный API 5 секунд в одной транзакции. Что плохо?</summary>

Соединение из пула и блокировки строк удерживаются 5 секунд. При пуле 10 соединений сервис обрабатывает 2 таких запроса в секунду, остальные ждут соединение и падают по таймауту. Плюс несогласованность: платёж прошёл, а транзакция откатилась. Решение: короткая транзакция «заказ создан, статус PENDING» + outbox → асинхронный платёж с ключом идемпотентности → обновление статуса.

</details>

<details>
<summary>3. Внешний метод поймал исключение внутреннего и всё равно получил UnexpectedRollbackException. Почему?</summary>

Оба метода в одной физической транзакции (`REQUIRED`). Исключение прошло через proxy внутреннего метода, и транзакция помечена rollback-only. При commit внешнего Spring обнаруживает пометку, откатывает и бросает `UnexpectedRollbackException`. Проверено в лаборатории выше.

</details>

<details>
<summary>4. Почему prototype-bean в singleton всегда один и тот же?</summary>

Зависимости внедряются один раз — при создании singleton. Контейнер создал один экземпляр prototype и больше его не трогает. Новый экземпляр на каждое использование — `ObjectProvider<T>`, `@Lookup` или scoped proxy.

</details>

<details>
<summary>5. В чём разница между @Component и @Bean?</summary>

`@Component` (и `@Service`, `@Repository`, `@Controller`) ставится на свой класс и находится сканированием. `@Bean` — метод в `@Configuration`, который создаёт объект вручную: для чужих классов, которые нельзя аннотировать, или когда создание требует логики. `@Repository` дополнительно включает трансляцию исключений персистентности в `DataAccessException`.

</details>

<details>
<summary>6. Как вызвать @Transactional-метод из @PostConstruct?</summary>

Никак надёжно: в `@PostConstruct` вызывается собственный метод через `this`, а proxy создаётся позже, в `postProcessAfterInitialization`. Нужно выполнить работу после готовности контекста — `ApplicationRunner`, `@EventListener(ApplicationReadyEvent.class)` в другом bean или `TransactionTemplate` с явным управлением.

</details>

---

## 11. Источники

- [Spring Framework 7.0 General Availability](https://spring.io/blog/2025/11/13/spring-framework-7-0-general-availability/).
- [Spring Framework: Resilience Features](https://docs.spring.io/spring-framework/reference/core/resilience.html) и [Javadoc пакета resilience.annotation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/resilience/annotation/package-summary.html).
- [Spring Framework 7.0 Release Notes](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes/32293be0c96de5abcc833c04c6c7d8036f2fe2c9).
- [Baeldung: Spring Boot 4 и Spring Framework 7](https://www.baeldung.com/spring-boot-4-spring-framework-7).
- [Версии spring-context в Maven Central](https://repo.maven.apache.org/maven2/org/springframework/spring-context/maven-metadata.xml) — 7.0.10 использована в лаборатории.

---

## Приложение: код лабораторных

Зависимости: `spring-context`, `spring-jdbc`, `spring-aspects` 7.0.10, `jakarta.annotation-api` 3.0.0, `h2` 2.5.252. Запуск через `mvn exec:java` или `java -cp`.

<details>
<summary>Main.java — жизненный цикл, proxy, транзакции, @Async, scope, resilience</summary>

```java
package demo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import jakarta.annotation.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.resilience.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.aop.support.AopUtils;

public class Main {
    @Configuration
    @EnableTransactionManagement
    @EnableAsync
    @EnableResilientMethods
    @ComponentScan("demo")
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:lab;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean PlatformTransactionManager txManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean JdbcTemplate jdbc(DataSource ds) { var j = new JdbcTemplate(ds); j.execute("create table orders(id int primary key, status varchar(20))"); return j; }
    }

    public static void main(String[] args) throws Exception {
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            var svc = ctx.getBean(OrderService.class);
            var jdbc = ctx.getBean(JdbcTemplate.class);
            System.out.println("== proxy: " + AopUtils.isCglibProxy(svc));
            System.out.println("== tx via proxy: " + svc.txActiveDirect());
            System.out.println("== self-invocation: " + svc.callInternally());
            try { svc.insertThenRuntime(1); } catch (RuntimeException e) {}
            System.out.println("== RuntimeException -> rows id=1: " + count(jdbc, 1));
            try { svc.insertThenChecked(2); } catch (Exception e) {}
            System.out.println("== checked Exception -> rows id=2: " + count(jdbc, 2));
            try { svc.insertThenCheckedRollbackFor(3); } catch (Exception e) {}
            System.out.println("== checked + rollbackFor -> rows id=3: " + count(jdbc, 3));
            try { svc.outerWithInnerRequiresNew(4); } catch (RuntimeException e) {}
            System.out.println("== outer rolled back, REQUIRES_NEW audit kept: orders=" + count(jdbc, 4) + " audit=" + count(jdbc, 1004));
            try { svc.outerCatchesInnerRequired(5); }
            catch (UnexpectedRollbackException e) { System.out.println("== inner REQUIRED failed, outer caught -> " + e.getClass().getSimpleName()); }
            var async = ctx.getBean(AsyncService.class);
            System.out.println("== @Async via proxy thread: " + async.whoRuns().get());
            System.out.println("== @Async self-invocation thread: " + async.selfCall());
            var holder = ctx.getBean(SingletonHolder.class);
            System.out.println("== prototype injected into singleton same? " + (holder.injected() == holder.injected())
                + "; via ObjectProvider same? " + (holder.fresh() == holder.fresh()));
            var flaky = ctx.getBean(FlakyClient.class);
            System.out.println("== field through proxy: " + flaky.attempts);
            System.out.println("== @Retryable result: " + flaky.call() + " attempts=" + flaky.attempts());
            var limited = ctx.getBean(LimitedService.class);
            try (var ex = Executors.newVirtualThreadPerTaskExecutor()) { for (int i = 0; i < 10; i++) ex.submit(limited::work); }
            System.out.println("== @ConcurrencyLimit(2) max parallel: " + limited.maxSeen());
        }
    }

    static int count(JdbcTemplate jdbc, int id) {
        return jdbc.queryForObject("select count(*) from orders where id=?", Integer.class, id);
    }
}

@Component
class OrderService {
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    OrderService(JdbcTemplate jdbc, AuditService audit) { this.jdbc = jdbc; this.audit = audit; }
    void insert(int id) { jdbc.update("insert into orders values(?, 'NEW')", id); }
    @Transactional public boolean txActiveDirect() { return TransactionSynchronizationManager.isActualTransactionActive(); }
    public boolean callInternally() { return txActiveDirect(); }
    @Transactional public void insertThenRuntime(int id) { insert(id); throw new IllegalStateException(); }
    @Transactional public void insertThenChecked(int id) throws Exception { insert(id); throw new Exception("checked"); }
    @Transactional(rollbackFor = Exception.class)
    public void insertThenCheckedRollbackFor(int id) throws Exception { insert(id); throw new Exception("checked"); }
    @Transactional public void outerWithInnerRequiresNew(int id) { insert(id); audit.logNewTx(1000 + id); throw new IllegalStateException(); }
    @Transactional public void outerCatchesInnerRequired(int id) { insert(id); try { audit.failRequired(); } catch (RuntimeException ignored) {} }
}

@Component
class AuditService {
    private final JdbcTemplate jdbc;
    AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logNewTx(int id) { jdbc.update("insert into orders values(?, 'AUDIT')", id); }
    @Transactional public void failRequired() { throw new IllegalStateException("inner"); }
}

@Component
class AsyncService {
    @Async public CompletableFuture<String> whoRuns() { return CompletableFuture.completedFuture(Thread.currentThread().getName()); }
    public String selfCall() throws Exception { return whoRuns().get(); }
}

@Component @Scope("prototype") class Proto {}

@Component
class SingletonHolder {
    private final Proto proto; private final ObjectProvider<Proto> provider;
    SingletonHolder(Proto proto, ObjectProvider<Proto> provider) { this.proto = proto; this.provider = provider; }
    Proto injected() { return proto; }
    Proto fresh() { return provider.getObject(); }
}

@Component
class FlakyClient {
    final AtomicInteger attempts = new AtomicInteger();
    public int attempts() { return attempts.get(); }
    @Retryable(maxRetries = 3, delay = 50)
    public String call() { if (attempts.incrementAndGet() < 3) throw new IllegalStateException("temporary"); return "ok"; }
}

@Component
class LimitedService {
    final AtomicInteger inside = new AtomicInteger(), max = new AtomicInteger();
    public int maxSeen() { return max.get(); }
    @ConcurrencyLimit(2)
    public void work() {
        int n = inside.incrementAndGet(); max.accumulateAndGet(n, Math::max);
        try { Thread.sleep(50); } catch (InterruptedException e) {}
        inside.decrementAndGet();
    }
}

@Component
class LifeBean implements BeanNameAware, InitializingBean, DisposableBean {
    LifeBean() { System.out.println("[life] 1 constructor"); }
    @jakarta.annotation.Resource void setDep(AuditService a) { System.out.println("[life] 2 dependency injection"); }
    public void setBeanName(String n) { System.out.println("[life] 3 BeanNameAware " + n); }
    @PostConstruct void pc() { System.out.println("[life] 5 @PostConstruct"); }
    public void afterPropertiesSet() { System.out.println("[life] 6 afterPropertiesSet"); }
    @PreDestroy void pd() { System.out.println("[life] 8 @PreDestroy"); }
    public void destroy() { System.out.println("[life] 9 DisposableBean.destroy"); }
}

@Component
class LoggingBpp implements BeanPostProcessor {
    public Object postProcessBeforeInitialization(Object b, String n) { if (b instanceof LifeBean) System.out.println("[life] 4 BPP before init"); return b; }
    public Object postProcessAfterInitialization(Object b, String n) { if (b instanceof LifeBean) System.out.println("[life] 7 BPP after init (here proxies are created)"); return b; }
}
```

</details>

<details>
<summary>Main2.java — видимость методов и readOnly</summary>

```java
package demo2;

import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class Main2 {
    @Configuration @EnableTransactionManagement @ComponentScan("demo2")
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:lab2;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean PlatformTransactionManager txManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean JdbcTemplate jdbc(DataSource ds) { var j = new JdbcTemplate(ds); j.execute("create table t(id int)"); return j; }
    }
    public static void main(String[] a) {
        try (var ctx = new AnnotationConfigApplicationContext(Config.class)) {
            var s = ctx.getBean(Svc.class);
            System.out.println("public          tx=" + s.pub());
            System.out.println("package-private tx=" + s.pkg());
            System.out.println("protected       tx=" + s.prot());
            System.out.println("final public    tx=" + s.fin());
            System.out.println("readOnly        readOnlyFlag=" + s.ro());
            try { s.roWrite(); System.out.println("readOnly + INSERT: прошло, строк="
                    + ctx.getBean(JdbcTemplate.class).queryForObject("select count(*) from t", Integer.class)); }
            catch (Exception e) { System.out.println("readOnly + INSERT: " + e.getClass().getSimpleName()); }
        }
    }
}

@Component
class Svc {
    private final JdbcTemplate jdbc;
    Svc(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    static boolean tx() { return TransactionSynchronizationManager.isActualTransactionActive(); }
    @Transactional public boolean pub() { return tx(); }
    @Transactional boolean pkg() { return tx(); }
    @Transactional protected boolean prot() { return tx(); }
    @Transactional public final boolean fin() { return tx(); }
    @Transactional(readOnly = true) public boolean ro() { return TransactionSynchronizationManager.isCurrentTransactionReadOnly(); }
    @Transactional(readOnly = true) public void roWrite() { jdbc.update("insert into t values(1)"); }
}
```

</details>
