# Project Reactor для Java-разработчика

Это самостоятельный Maven-проект с семью консольными примерами на Java 17 и Project Reactor 3.8.8. Запуску достаточно JDK и Maven Wrapper из корня репозитория. Первый запуск скачивает зависимости. Сервер, Docker и база данных для примеров не нужны.

## Запуск

PowerShell, из корня репозитория. Если Maven Wrapper не видит Java, укажи путь к каталогу установленного JDK в `$env:JAVA_HOME`.

```powershell
.\mvnw.cmd -f reactor-demo/pom.xml test
.\mvnw.cmd -q -f reactor-demo/pom.xml compile exec:java
.\mvnw.cmd -q -f reactor-demo/pom.xml compile exec:java '-Dexec.args=demand'
```

В IntelliJ IDEA можно открыть `reactor-demo/pom.xml` как отдельный Maven-проект и запустить `ReactorDemo.main`. Чтобы запустить один пример, укажи его имя в аргументах запуска. На macOS или Linux вместо `.\mvnw.cmd` используй `sh ./mvnw`.

Если PowerShell отображает кириллицу неверно, на JDK 21+ можно добавить `-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8` к `$env:MAVEN_OPTS` и повторить запуск.

## Маршрут

1. Прочитай [основы Reactor](THEORY.md) до конца раздела про операторы. Перед запуском каждого примера предскажи результат и сверь его с выводом или тестом.
2. Разбери спрос, потоки, разделение источника и ошибки по таблице. Найди в тестах проверки `request`, `cancel` и виртуального времени.
3. Пройди расчёт заказа от подписки до ответа. После этого прочитай [раздел уровня Senior](PRODUCTION.md) и реши [вопросы и задачи для самопроверки](INTERVIEW.md).

| Аргумент | Код | Что увидеть |
| --- | --- | --- |
| `sources` | [Sources.java](src/main/java/ru/learn/reactor/Sources.java) | `just` повторяет уже вычисленное значение, `fromSupplier` пересчитывает его для каждой подписки, `generate` хранит своё состояние на подписку. |
| `composition` | [Composition.java](src/main/java/ru/learn/reactor/Composition.java) | `filter`, `map`, `zip`, `then` и три варианта преобразования в асинхронные операции. Точный порядок `flatMap` на настоящих часах может меняться. |
| `demand` | [FlowControl.java](src/main/java/ru/learn/reactor/FlowControl.java) | Подписчик сначала просит два значения, затем ещё два. `take` завершает подписчика и отменяет источник. `limitRate` меняет размер запросов к источнику, поэтому в журнале возможен запрос на пополнение партии рядом с отменой. |
| `threads` | [Execution.java](src/main/java/ru/learn/reactor/Execution.java) | Чтение и последующая обработка идут на разных потоках. Имитация блокирующего клиента ждёт на `boundedElastic`. `traceId` остаётся доступен после смены потока. |
| `sharing` | [Sharing.java](src/main/java/ru/learn/reactor/Sharing.java) | Две подписки на `Mono.cache()` делят одну загрузку. Поздний подписчик получает два последних события из replay-sink. |
| `errors` | [Failures.java](src/main/java/ru/learn/reactor/Failures.java) | Ошибка одной строки оставляет другие строки в результате. Временная ошибка вызывает новую попытку. Ресурс закрывается через `usingWhen`. |
| `orders` | [OrderQuotes.java](src/main/java/ru/learn/reactor/OrderQuotes.java) | Поиск цены, максимум две активные загрузки на расчёт, восстановление порядка, необязательная скидка и `Context`. Для входа `[3, 1, 2]` итог равен `5500` копеек. |

Точка входа [ReactorDemo.java](src/main/java/ru/learn/reactor/ReactorDemo.java) использует `block(Duration)` на границе консоли, чтобы дождаться вывода. Учебные операции возвращают `Mono` и `Flux`. Поиск цены и сессия имитируют внешние зависимости.

## Тесты как учебные примеры

| Файл | Что проверить |
| --- | --- |
| [SignalsTest.java](src/test/java/ru/learn/reactor/SignalsTest.java) | Ленивость, пустой `zip`, порядок, спрос с нуля, поздняя подписка, переполнение, потоки и контекст. |
| [FailuresTest.java](src/test/java/ru/learn/reactor/FailuresTest.java) | Таймер на попытку и на всю операцию, повторная подписка и закрытие ресурса по трём причинам. |
| [OrderQuotesTest.java](src/test/java/ru/learn/reactor/OrderQuotesTest.java) | Выдача в исходном порядке, лимит активных внутренних подписок, повтор только упавшего чтения и отмена другой ветки при ошибке. |

Один тест можно запустить из IDEA или командой `.\mvnw.cmd -f reactor-demo/pom.xml '-Dtest=SignalsTest#flatMapLetsFastCallsFinishFirst' test`.
