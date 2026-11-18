# Java: язык, JVM и многопоточность

[Все конспекты](../README.md)

Начни с контрактов языка и коллекций. Для вопросов о памяти и диагностике открой JVM, для задач с общим состоянием — многопоточность.

| Тема | Где читать |
| --- | --- |
| Передача параметров, перегрузка, equals и hashCode, generics, Optional, record | [Язык и контракты](language.md) |
| Value objects, sealed-результаты и совместимость поведения | [Доменные типы](domain-types.md) |
| Время и зоны, Unicode, файлы, деньги и округление | [Стандартная библиотека](standard-library.md) |
| List, Set, Map, Queue, устройство HashMap, представления и итераторы | [Коллекции](collections.md) |
| Comparator, выбор ключа, живые представления и атомарность Map | [Контракты и композиция коллекций](collection-contracts.md) |
| Ленивый обход, groupingBy, toMap, reduce и parallel stream | [Stream API](streams.md) |
| Загрузка классов, JIT, heap, GC, JFR, профилирование | [JVM](jvm.md) |
| Classpath, модули, jlink и расследование проблем процесса | [Сценарии работы JVM](jvm-runtime-cases.md) |
| Java Memory Model, happens-before, volatile, CAS, блокировки, гонки | [Многопоточность](concurrency.md) |
| Атомарные инварианты, публикация снимков, лимиты и отмена | [Общее состояние в нескольких потоках](concurrent-state.md) |
| Пулы потоков, CompletableFuture, Fork/Join, виртуальные потоки, ThreadLocal | [Выполнение задач](async.md) |
| HTTP-клиент, тайм-ауты фаз, чтение тела, HTTP/2 и прокси | [Сетевые вызовы Java](http-clients.md) |
| Фазы Maven, scopes, BOM, Java release и диагностика classpath | [Сборка Maven](maven.md) |

**Практика:** [коллекции](../../../src/main/java/ru/learn/collections/README.md) · [дженерики](../../../src/main/java/ru/learn/generic/README.md) · [Stream API](../../../src/main/java/ru/learn/stream/api/README.md) · [многопоточность](../../../src/main/java/ru/learn/cuncurrent/README.md)

**Собеседование:** [Java Core и JVM](../../interview/base/java.md) · [Многопоточность](../../interview/advanced/concurrency.md) · [Коллекции и Stream API](../../interview/advanced/collections.md)
