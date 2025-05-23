# Многопоточность 
- Процесс - это экземпляр выполненной программы, а также текущие значения счетчика команд, регистров и переменных. С точки зрения операционной системы JVM - процесс. Внутри рабочего процесса есть "рабочие юниты", потоки. Каждый процесс может содержать много потоков.  
- Поток - это основная единица, которой операционная система выделяет время процессора. Каждый поток использует память процессора и его ресурсы.

В чем разница между потоком и процессом?

Процесс является более высокоуровневой абстракцией ОС для выполнения программы, чем поток. Он, в свою очередь, может запускать в себе несколько потоков. Процесс всегда имеет хотя бы один (главный) поток.

«Выполняется прежде» (англ. **happens before**) — отношение строгого частичного порядка введённое между атомарными командами. Оно значит, что вторая команда будет «в курсе» изменений, проведённых первой. Одно выполняется прежде другого для таких операций (список не полный):

### Состояния потока
 - **New** (создан, работа не начата)
 - **Active** (начал работу)
 - **Waiting** (ожидание выполнения определенного действия другим потоком (`notify`/`notifyAll`, `LockSupport#unpark`).) 
 - **TimedWaiting** (то же, что и WAITING, но с таймаутом.) 
 - **Blocked** (ожидание на мониторе — он пытается получить блокировку и войти в критическую секцию.)
 - **Terminated** (работа завершена)

За приоритет потоков отвечает планировщик потоков JVM. Установка приоритетов для ОС носит рекомендательный характер, выполнение не гарантируется 

Потоки демоны имеют низкий приоритет, и прерываются когда программа завершила свою работу. Стандартные потоки завершают работу планово. Используются для back ground процессов или health check, когда не важно чтобы поток завершил свою работу

У каждого потока есть своя Stack память(стек вызовов методов, локальные переменные и аргументы методов)
Все потоки имеют общую Heap память

Как меняется по умолчанию значение объекта для процессора
1) чтение переменной из памяти
2) изменение значения переменной
3) запись нового значения в память

Метод yield() в Java — это статический метод класса Thread, используемый для временного добровольного освобождения процессорного времени активным потоком исполнения. Суть метода проста: вызвав yield(), активный поток добровольно уступает свое право на выполнение другим готовым к исполнению потокам, позволяя операционной системе выбрать следующий поток для продолжения работы.

Double Check Locking (DCL) — это шаблон проектирования, применяемый в многопоточном программировании для ленивой инициализации ресурса (чаще всего одиночки — Singleton) безопасным способом относительно многопоточности. Цель DCL состоит в снижении накладных расходов на получение блокировок (локов), применяя двойной контроль существования ресурса перед его созданием.

### Синхронизация данных
- **Mutex**, монитор (Mutual Exclusion Objects) 
- Механизм блокировки
- Любой поток должен "захватить" Мьютекс для доступа к ресурсу, который находится под его "защитой"
- Контролирует доступ только к одному ресурсу
- Каждый объект в java имеет встроенный монитор
- Когда поток захватывает этот монитор, второй поток ожидает
- Монитор связан именно с ОБЪЕКТОМ
- В java существует концепция внешних мониторов. В отличие от внутренних мониторов, которое могут быть освобождены ЛЮБЫМ потоком, внешний монитор может быть освобожден ТОЛЬКО тем потоком, что его захватил (**ReentrantLock**) 
- **Семафоры** - переменные, которые используются для контроля доступа к общему ресурсу 
- Существуют бинарные семафоры и семафоры со счетчиком
- Бинарные семафоры впускают только один поток (synchronize)
- Контролируют ТОЛЬКО количество входов
- **Exchanger** - используется для передачи данных между потоками
- **RaceCondition** - ситуация, когда несколько потоков параллельно получают доступ к общим данным и пытаются их изменить. Итоговое состояние данных зависит от того, в каком порядке потоки получили доступ к данным. Пример - увеличение счетчика i++. Решается с помощью синхронизации доступов к общим данным (блокировки, атомарные операции)
- **Data race** - это частный случай race condition, связанный именно с одновременным доступом к одной переменной несколькими потоками/процессами, причем хотя бы один из потоков записывает данные.
- **DeadLock** - ситуация, когда 2 и более потока бесконечно "ждут" ресурс, который занят другим потоком. Решение проблемы: самое каноничное - взятие блокировок в строгом порядке, уменьшение количества вложенных блокировок, добавлять время действия блокировки (таймаут), алгоритмы без блокировок (???)
- **LiveLock** - система занимается бесполезной работой, ее состояние постоянно меняется, но она зациклена
- **Starvation** - результат процесса, которой не может получить регулярный доступ к общим ресурсам, необходимым для выполнения работы, и, следовательно, не может добиться какого либо прогресса
- Пакет **java.util.concurrent.atomic**.* Под капотом механизм CAS (`compare-and-swap`). LockFree алгоритм, то есть в работе не используют локи. В этом пакете есть обертки над примитивами, и класс `AtomicReference` для собственных потоко-безопасных типов данных
- **ThreadLocal** - Один из способов хранить данные в потоке и сделать блокировку необязательной — это использовать хранилище `ThreadLocal`. Концептуально `ThreadLocal` действует так, как будто в каждом потоке есть своя версия переменной. `ThreadLocal` обычно используется для фиксации значений каждого потока, таких как «текущая транзакция», или других ресурсов. Кроме того, они используются для содержания поточных счетчиков, статистики или генераторов идентификаторов.
- **CountDownLatch** — это инструмент синхронизации потоков в Java, предназначенный для координации действий между ними. Его основное предназначение — обеспечение механизма, позволяющего одному или нескольким потокам ожидать окончания определённого количества операций, выполненных другими потоками.
- **violotile** - является ключевым словом Java, которое позволяет объявлять поля таким образом, чтобы обеспечить видимость изменений, сделанных одним потоком другим потокам. Хотя атомические классы (AtomicInteger, AtomicBoolean) обеспечивают аналогичную функциональность, использование примитивных типов с volatile проще и быстрее, особенно если речь идёт лишь о чтении значений. Но если ваша задача — простая гарантия видимости состояния между потоками, то volatile обеспечивает нужный эффект с минимальными издержками.

Как узнать что мы поймали `DeadLock`? Выгрузить `ThreadDump`. Там будет написан статус потоков, из которого мы сможем сделать вывод о блокировке.

**_Когда лучше использовать каждый механизм?_**
- **Volatile**: Используйте, когда достаточно простого чтения-переменного значения, которое должно немедленно становиться видимым другим потокам.
- **Aтомики**: Применяйте, когда нужны более сложные операции с изменением данных (инкремент, обмен значениями).
- **Синхронизация и блокировки**: Используются, когда необходим исключительный доступ к критическим секциям, защищающим ресурсы от конкурентного доступа нескольких потоков.

#### Блокировки
- **Lock** Пакет `java.util.concurrent.locks` имеет стандартный интерфейс `Lock`. Реализация `ReentrantLock` дублирует функциональность ключевого слова `synchronized`, но также предоставляет дополнительные функции, такие как получение информации о состоянии блокировки, неблокирующий `tryLock()` и прерываемая блокировке.
- **ReadWriteLock** Пакет `java.util.concurrent.locks` также содержит интерфейс `ReadWriteLock` (и реализацию `ReentrantReadWriteLock`), который определяется парой блокировок для чтения и записи, обычно позволяя считывать одновременно нескольким читателям, но допуская только одного писателя.
- **CountDownLatch** инициализируется счетчиком. Потоки могут вызывать `await()`, чтобы ждать, пока счетчик не достигнет 0. Другие потоки (или тот же поток) могут вызвать `countDown()`, чтобы уменьшить счетчик. Нельзя использовать повторно, как только счетчик достигнет 0. Используется для запуска неизвестного набора потоков, как только произошло некоторое количество действий.
- **CompletableFuture** является абстракцией для произведения асинхронных вычислений. В отличие от простого `Future`, где единственная возможность получить результат — блокировать, рекомендуется регистрировать обратные вызовы для создания конвейера задач, которые должны выполняться, когда доступен результат или исключение. Либо во время создания (через `CompletableFuture#supplyAsync/runAsync`), либо во время добавления обратных вызовов (методы семейства `*async`) может быть указан исполнитель, где должно выполняться вычисление (если он не указан стандартным глобальным `ForkJoinPool#commonPool`). Учтите, что если `CompletableFuture` уже завершен, обратные вызовы, зарегистрированные с помощью не `*async` методов, будут выполняться в вызывающем потоке. Если есть несколько `future`, вы можете использовать `CompletableFuture#allOf`, чтобы получить `future`, который будет завершен, когда все `future` будут завершены, или `CompletableFuture#anyOf`, который будет завершен, как только будет завершен какой-либо `future`.
- 

#### CAS (compare-and-swap)
1) Получение текущего значения переменной
2) Вычисление нового значения
3) Сравнение текущего значения и значения из памяти (на случай если оно поменялось)
4) если они равны замена значения из памяти на новое значение, если не равны повторить с первого шага

#### Основные потокобезопасные коллекции

##### Списки
- ArrayList -> **CopyOnWriteArrayList**. Предоставляет семантику копирования при записи, где каждая модификация структуры данных приводит к новой внутренней копии данных (поэтому запись очень дорогая, тогда как чтение дешевое). Итераторы в структуре данных всегда видят снепшот данных с момента создания итератора.
- HashMap -> **ConcurrentHashMap**. ПОбычно выступает в качестве сегментированной хэш-таблицы. Операции чтения, как правило, не блокируют и отражают результаты последней завершенной записи. Запись первого узла в пустой ящик выполняется просто CAS-ом (сравнить и установить), тогда как другим операциям записи требуются блокировки (первый узел сегмента используется как блокировка).

##### Множества
- TreeMap -> **ConcurrentSkipListMap** Обеспечивает параллельный доступ наряду функциональностью сортированного `Map`, подобной `TreeMap`. Границы производительности такие же как у `TreeMap`, хотя несколько потоков обычно могут читать и записывать из ассоциативного массива без конфликтов, если они не изменяют одну и ту же часть отображения.
- Set -> **CopyOnWriteArraySet** Подобно CopyOnWriteArrayList, он использует семантику copy-on-write для реализации интерфейса Set.
- Set -> **ConcurrentSkipListSet** Подобно ConcurrentSkipListMap, но реализует интерфейс Set.

##### Очереди
Очереди выступают в качестве труб между «производителями» и «потребителями». Элементы помещаются в один конец трубы и выходят из другого конца трубы в том же порядке «первый зашел, первый вышел» (FIFO). Интерфейс `BlockingQueue` расширяет `Queue`, чтобы предоставить дополнительные варианты того, как обрабатывать сценарий, где очередь может быть заполнена (когда производитель добавляет элемент) или пустой (когда потребитель читает или удаляет элемент). В этих случаях `BlockingQueue` предоставляет методы, которые либо блокируют навсегда, либо блокируют в течение определенного периода времени, ожидая изменения условия из-за действий другого потока.
- **ConcurrentLinkedQueue** Неограниченная неблокирующая очередь, поддерживаемая связанным списком.
- **LinkedBlockingQueue** Опционально ограниченная блокирующая очередь, поддерживаемая связанным списком.
- **PriorityBlockingQueue** Неограниченная блокирующая очередь, поддерживаемая минимальной кучей. Элементы удаляются из очереди в порядке, основанном на компараторе Comparator, связанном с очередью (вместо порядка FIFO).
- **DelayQueue** Неограниченная блокирующая очередь элементов, каждый из которых имеет значение задержки. Элементы могут быть удалены только тогда, когда их задержка прошла и удаляются в порядке старейшего истекшего элемента.
- **SynchronousQueue** Очередь о-длины, где производитель и потребитель блокируются до тех пор, пока не прибудет другой. Когда оба потока приходят, значение передается напрямую от производителя к потребителю. Полезно при передаче данных между потоками.


### Executors
Преимущества:
- Возможность переиспользовать уже созданные потоки
- Меньшие затраты по памяти
- Позволяют планировать и исполнять задачи

#### Что такое пул потов?
Пул потоков (thread pool) — это заранее созданный набор готовых к выполнению потоков, которые используются повторно для обработки множества задач. Вместо постоянного создания новых потоков под каждую отдельную операцию, задачи помещаются в очередь, откуда потоки берут их и выполняют последовательно.

Ключевые реализации
- **SingleThreadPoolExecutor** имеет только 1 поток в пуле
- **FixedThreadPoolExecutor** создает фиксированное количество потоков в пуле. Чаще всего это количество процессоров - 1. Задачи накапливаются в `LinkedBlockingQueue`, и далее воркеры выгребают задачи из него
- **ScheduledThreadPoolExecutor** позволяет выполнять задачи на регулярной основе или с отложенным стартом
- **CachedThreadPoolExecutor** количество потоков в пуле не ограничивается и может быть увеличено при высокой нагрузке. Если нагрузка низкая, количество потоков уменьшается
- **newWorkStealingPool** возвращает крадущий задачи ExecutorService.

При определении размера пулов потока часто бывает полезно определить размер числа логических ядер в машине, на которой запущено приложение. Получить это значение в Java можно вызвав `Runtime.getRuntime().AvailableProcessors().`

Реализации пула потоков
- **ThreadPoolExecutor** реализация по умолчанию с изменяющим размер пулом потока, одной рабочей очереди и настраиваемой политикой для отклоненных задач (через `RejectedExecutionHandler`) и создания потоков (через `ThreadFactory`).
- **ScheduledThreadPoolExecutor** расширение `ThreadPoolExecutor`, которое обеспечивает возможность планирования периодических задач.
- **ForkJoinPool** крадущий задачи пул: все потоки в пуле пытаются найти и запустить либо поставленные задачи, либо задачи, созданные другими активными задачами. Количество потоков равно количеству ядер процессора, для каждого потока создается своя очередь задач. Не выгодно использовать для задач с ожиданием

Задачи отправляются с помощью `ExecutorService#submit`, `ExecutorService#invokeAll` или `ExecutorService#invokeAny`, которые имеют несколько перегрузок для разных типов задач.

 ### Fork-Join фреймворк
- Реализация параллельной обработки для `ExecutorService`, использует принцип "разделяй и властвуй"
- Задачи должны быть независимы друг от друга (сумма элементов в массиве, поиск максимального элемента в массиве)
- Для того чтобы получить возвращаемое значение используем `RecursiveTask<T>` и переопределяем метод `compute()`
- Без возвращаемого значения используем `RecursiveAction`
- `Work-Stealing` - потоки "воруют" работу друг друга
- Создается фиксированный пул потоков (обычно - количество CPU)

### Способы создания потоков
В Java доступны три варианта создания потока из класса - расширить класс `Thread` либо реализовать интерфейсы `Callable` или `Runnable`. 

