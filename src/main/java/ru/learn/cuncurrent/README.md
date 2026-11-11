# Многопоточность в Java

Короткие примеры на стандартной библиотеке Java. У каждого класса есть свой `main` и комментарии к местам, где потоки взаимодействуют. Можно начать с жизненного цикла потока, затем перейти к общим данным, координации и пулам.

## Запуск

Запусти нужный `main` из IDE. Из корня проекта отдельный пример можно выполнить с JDK 17 или новее, без сборки Maven и запуска Spring.

```powershell
java "-Dfile.encoding=UTF-8" src/main/java/ru/learn/cuncurrent/shared/CounterDemo.java
```

Для скрипта с виртуальными потоками нужен JDK 21 или новее.

```powershell
jshell --execution local "-J-Dfile.encoding=UTF-8" src/main/java/ru/learn/cuncurrent/threads/VirtualThreadsDemo.jsh
```

## 1. Потоки

| Пример | Что посмотреть |
| --- | --- |
| [ThreadLifecycleDemo](threads/ThreadLifecycleDemo.java) | `run()` выполняется в `main`, `start()` запускает рабочий поток, `join()` ждёт его завершения. |
| [InterruptDemo](threads/InterruptDemo.java) | `interrupt()` прерывает ожидание, и рабочий поток выходит с очисткой в `finally`. |
| [ThreadSettingsDemo](threads/ThreadSettingsDemo.java) | Порядок вывода потоков с разными приоритетами может меняться. JVM завершится, даже если daemon ещё ждёт. |
| [VirtualThreadsDemo](threads/VirtualThreadsDemo.jsh) | Каждый запрос получает свой виртуальный поток. Семафор ограничивает число одновременных обращений к сервису. |

## 2. Общие данные

| Пример | Что посмотреть |
| --- | --- |
| [CounterDemo](shared/CounterDemo.java) | Две продажи дают `1` при потере обновления и `2` с `synchronized`, `AtomicInteger` и `LongAdder`. |
| [VolatileFlagDemo](shared/VolatileFlagDemo.java) | Запись в `volatile`-флаг делает готовый отчёт видимым читателю. |
| [ConcurrentCollectionsDemo](shared/ConcurrentCollectionsDemo.java) | `ConcurrentHashMap.merge()` атомарно считает посещения. Итератор `CopyOnWriteArrayList` обходит снимок списка подписчиков. |

## 3. Блокировки и проблемы с прогрессом

| Пример | Что посмотреть |
| --- | --- |
| [ReentrantLockDemo](lock/ReentrantLockDemo.java) | Первая попытка `tryLock()` истекает по таймауту. После освобождения ресурса повтор проходит. |
| [ReadWriteLockDemo](lock/ReadWriteLockDemo.java) | Читатель получает согласованную сумму цен, даже если другой поток обновляет оба компонента. |
| [MonitorWaitDemo](lock/MonitorWaitDemo.java) | Получатель ждёт посылку через `wait()`. Доставщик меняет состояние и вызывает `notifyAll()`. |
| [ConditionDemo](lock/ConditionDemo.java) | Робот делает три пары шагов. Потоки ждут своей очереди через `await()` и проверяют условие после пробуждения. |
| [MonitorDeadlockDemo](lock/MonitorDeadlockDemo.java) | Два потока застревают на мониторах друг друга. JVM обнаруживает цикл ожидания, после чего процесс завершается. |
| [LivelockDemo](lock/LivelockDemo.java) | Два работника уступают друг другу. После шести попыток выполнено ноль работ. |
| [StarvationDemo](lock/StarvationDemo.java) | Модель очереди показывает, как срочные задания вытесняют обычное. В FIFO оно выполняется первым. |

## 4. Координация задач

| Пример | Что посмотреть |
| --- | --- |
| [OrderedStepsDemo](coordination/OrderedStepsDemo.java) | `CountDownLatch` задаёт порядок упаковки, оплаты и отправки. Задачи подаются в обратном порядке. |
| [SemaphoreDemo](coordination/SemaphoreDemo.java) | Четыре запроса делят два разрешения. Пик одновременных запросов равен двум. |
| [BlockingQueueDemo](coordination/BlockingQueueDemo.java) | Производитель кладёт отчёты в ограниченную очередь, потребитель забирает их до сообщения `STOP`. |
| [CyclicBarrierDemo](coordination/CyclicBarrierDemo.java) | Два склада встречаются у одного барьера после каждого из двух раундов сверки. |
| [ExchangerDemo](coordination/ExchangerDemo.java) | Два потока обмениваются отчётами и получают данные партнёра. |

## 5. Пулы, планирование и Future

| Пример | Что посмотреть |
| --- | --- |
| [PoolFactoriesDemo](executor/PoolFactoriesDemo.java) | Одна пачка `Runnable` в single, fixed и cached пулах. По именам потоков видно, кто выполнил задачи. |
| [FutureDemo](executor/FutureDemo.java) | `Callable` возвращает значение. `Future.get()` передаёт ошибку и поддерживает таймаут, после которого можно запросить отмену. |
| [BoundedPoolDemo](executor/BoundedPoolDemo.java) | Два работника и одно место в очереди. Четвёртая задача отклоняется, а принятые завершаются при `shutdown()`. |
| [ScheduledTasksDemo](executor/ScheduledTasksDemo.java) | Отложенный отчёт, периодические метрики и отмена следующих запусков. |
| [ThreadLocalDemo](executor/ThreadLocalDemo.java) | `remove()` в `finally` очищает контекст до обработки следующего запроса тем же потоком. |

## 6. Асинхронные цепочки и вычисления

| Пример | Что посмотреть |
| --- | --- |
| [CompletableFutureDemo](async/CompletableFutureDemo.java) | `thenCompose` запускает зависимый запрос, `thenApply` применяет скидку, `thenCombine` добавляет независимый тариф доставки. |
| [CompletableFutureRecoveryDemo](async/CompletableFutureRecoveryDemo.java) | Ошибка и таймаут дают запасные значения. `allOf` ждёт обе ветки. |
| [ForkJoinSumDemo](forkjoin/ForkJoinSumDemo.java) | Деление массива на диапазоны через `fork()` и `join()`. Результат сравнивается с последовательной суммой и `parallel()`. |

[Модель памяти и блокировки](../theory/Concurrent.md) · [Пулы, асинхронность и виртуальные потоки](../theory/AsyncJava.md)
