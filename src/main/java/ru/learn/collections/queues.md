# Очереди в Java

[Практикум](README.md) · [Конкурентные коллекции](concurrent.md)

Голова очереди — элемент, который вернёт следующая операция извлечения. Правило выбора головы зависит от реализации. В FIFO первый поставленный заказ обработают первым. В LIFO последнюю правку отменят первой. В приоритетной очереди сначала выберут самую срочную заявку.

## Операции

| Действие | Сразу, с особым результатом | Сразу, с исключением | Ждать в `BlockingQueue` | Ждать до таймаута в `BlockingQueue` |
| --- | --- | --- | --- | --- |
| Добавить | `offer(e)` → `false`, если нет места | `add(e)` → `IllegalStateException`, если нет места | `put(e)` | `offer(e, timeout, unit)` → `false` по истечении таймаута |
| Извлечь голову | `poll()` → `null`, если элемент недоступен | `remove()` → `NoSuchElementException` | `take()` | `poll(timeout, unit)` → `null` по истечении таймаута |
| Посмотреть голову | `peek()` → `null`, если голова отсутствует | `element()` → `NoSuchElementException` | — | — |

`remove()` извлекает голову, а `remove(e)` ищет и удаляет заданный элемент. У `Deque` методы `offerFirst/Last`, `pollFirst/Last` и `peekFirst/Last` выбирают конец. Методы `addFirst/Last`, `removeFirst/Last` и `getFirst/Last` бросают исключения, если выполнить операцию нельзя. Для стека `push` соответствует `addFirst`, а `pop` — `removeFirst`. Операции ожидания прерываемы и могут бросить `InterruptedException`. [Queue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Queue.html) · [Deque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Deque.html) · [BlockingQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/BlockingQueue.html) · [BlockingDeque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/BlockingDeque.html)

`ArrayDeque`, `PriorityQueue` и конкурентные очереди из таблицы ниже запрещают `null`. `LinkedList` допускает `null`, поэтому его `poll()` с таким элементом невозможно отличить от `poll()` пустой очереди по возвращённому значению. [LinkedList](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedList.html)

## Выбор реализации

| Реализация | Порядок и ёмкость | Где увидеть в коде |
| --- | --- | --- |
| `ArrayDeque` | Растущий кольцевой массив. Операции на концах амортизированно O(1). Подходит для FIFO, LIFO и работы с обоими концами в одном потоке. | Заявки в [QueueDemo](QueueDemo.java), отмена в [StackDemo](StackDemo.java), поиск пути в [CollectionsExercises](CollectionsExercises.java). |
| `LinkedList` | Двусвязный список, реализует `List` и `Deque`. Операции на концах O(1), на каждый элемент нужен узел. | Вставка на позиции итератора в [ListDemo](ListDemo.java). |
| `PriorityQueue` | Растущая двоичная куча. `peek` O(1), `offer` и `poll` O(log n) для операций с кучей, расширение массива может стоить O(n). | Аварийные заявки с разрешением ничьей в [QueueDemo](QueueDemo.java). |
| `ConcurrentLinkedQueue` | Потокобезопасная FIFO без лимита и без ожидания данных. | Поток событий заказов в [ConcurrentQueuesDemo](ConcurrentQueuesDemo.java). |
| `ConcurrentLinkedDeque` | Потокобезопасная дека без лимита и без ожидания данных. | Срочная задача в начале, обычная в конце в [ConcurrentQueuesDemo](ConcurrentQueuesDemo.java). |
| `ArrayBlockingQueue` | FIFO на массиве фиксированной ёмкости. Можно включить справедливость доступа для ожидающих потоков. | Отказ, таймаут и производитель с потребителем в [BlockingQueuesDemo](BlockingQueuesDemo.java). |
| `LinkedBlockingQueue` | FIFO на связных узлах. Лимит задаётся конструктором, по умолчанию `Integer.MAX_VALUE`. | Такой же сценарий перегрузки на буфере из узлов в [BlockingQueuesDemo](BlockingQueuesDemo.java). |
| `LinkedBlockingDeque` | Блокирующие операции с обоих концов. По умолчанию лимит `Integer.MAX_VALUE`, можно задать меньший. | Повторная отправка письма с начала в [BlockingQueuesDemo](BlockingQueuesDemo.java). |
| `PriorityBlockingQueue` | Потокобезопасная очередь на куче. Ожидание при пустой очереди, логического лимита на вставку нет. | Срочные фоновые задания в [PriorityQueuesDemo](PriorityQueuesDemo.java). |
| `DelayQueue` | Извлечение готовых элементов по сроку. Логического лимита нет. | Повтор запроса сейчас и через час в [PriorityQueuesDemo](PriorityQueuesDemo.java). |
| `SynchronousQueue` | Нулевая ёмкость. Каждая вставка требует встретиться с извлечением в другом потоке. | Передача выписки без буфера в [ConcurrentQueuesDemo](ConcurrentQueuesDemo.java). |
| `LinkedTransferQueue` | FIFO без заданного лимита. `put` помещает в очередь, `transfer` ждёт, пока потребитель получит элемент. | Буфер и подтверждение получения задачи в [ConcurrentQueuesDemo](ConcurrentQueuesDemo.java). |

[ArrayDeque](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ArrayDeque.html) · [PriorityQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/PriorityQueue.html) · [Очереди в java.util.concurrent](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/package-summary.html#package.description)

## Приоритет и задержка

- `PriorityQueue` и `PriorityBlockingQueue` выбирают наименьший элемент по заданному `Comparator`. Для равного приоритета порядок не гарантирован. Если нужен FIFO внутри приоритета, сравнивай затем по монотонному номеру поступления. Обход итератором читает содержимое без гарантии сортировки. Для обработки по приоритету извлекай элементы через `poll` или `take`. Поля, по которым сравниваются задачи, должны оставаться стабильными, пока задача в куче. [PriorityQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/PriorityQueue.html) · [PriorityBlockingQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/PriorityBlockingQueue.html)
- `DelayQueue` хранит `Delayed`. `getDelay` сообщает оставшееся время, `compareTo` задаёт порядок по сроку. `peek()` и `element()` могут показать будущий элемент, `size()` учитывает и будущие элементы, а `poll()` вернёт `null`, пока ни один срок не наступил. `take()` ждёт готовую задачу. Для относительных задержек используй разность значений `System.nanoTime()`. [DelayQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/DelayQueue.html) · [System.nanoTime](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#nanoTime())

## Обмен между потоками

- **Перегрузка.** Лимит ограничивает число заданий в буфере. Когда он заполнен, `offer` даёт немедленный отказ, `offer` с таймаутом ограничивает ожидание, `put` ждёт освобождения места или прерывания. Выбор зависит от того, может ли производитель ждать, сообщить об отказе или повторить постановку. `remainingCapacity()` показывает состояние на момент вызова. Для решения о принятии новой задачи используй результат `offer`. [BlockingQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/BlockingQueue.html)
- **Извлечение.** Между `isEmpty()` и `poll()` другой потребитель может забрать элемент. Используй сам `poll()` и обработай `null` или вызови `take()`, если нужно дождаться работы. У `ConcurrentLinkedQueue` вычисление `size()` требует обхода очереди и при изменениях может вернуть неточное значение. Порядок извлечения FIFO сам по себе не задаёт порядок завершения задач у нескольких обработчиков. [ConcurrentLinkedQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ConcurrentLinkedQueue.html) · [BlockingQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/BlockingQueue.html)
- **Передача.** `SynchronousQueue.offer(e)` вернёт `false`, если получателя нет. `LinkedTransferQueue.tryTransfer(e)` при таком результате тоже не оставит элемент в очереди. У `LinkedTransferQueue` можно отдельно выбрать `put(e)` и сохранить элемент в буфере. Возврат из `transfer` подтверждает получение элемента потребителем. За подтверждение обработанной задачи отвечает отдельный протокол. [SynchronousQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/SynchronousQueue.html) · [TransferQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/TransferQueue.html)
- **Остановка.** У `BlockingQueue` нет метода `close`. В FIFO-примере один производитель ставит `STOP` после всех заданий, один потребитель выходит при его получении. При нескольких производителях отправляй сигнал после завершения всех постановок, при нескольких потребителях в такой схеме требуется по сигналу на каждого. При отмене задачи можно прервать ожидающий поток. [BlockingQueue](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/BlockingQueue.html)
