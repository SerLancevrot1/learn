# Дженерики в Java

Короткие примеры на стандартной библиотеке Java. У каждого примера свой `main`. Комментарии в коде показывают, какой тип выводит компилятор, что можно читать и записывать и где возникнет ошибка.

## Запуск

Запусти нужный `main` из IDE или запусти исходник из корня проекта с JDK 17 или новее.

```powershell
java "-Dfile.encoding=UTF-8" src/main/java/ru/learn/generic/variance/PecsCopyDemo.java
```

## 1. Классы, методы и интерфейсы

| Пример | Что посмотреть |
| --- | --- |
| [GenericTypesDemo](basics/GenericTypesDemo.java) | `Box<T>` хранит значение заданного типа, а `Entry<K, V>` связывает ключ и значение с независимыми типами. |
| [GenericMethodDemo](basics/GenericMethodDemo.java) | `firstOrDefault` возвращает элемент или запасное значение. Тип `T` выводится для каждого вызова. |
| [GenericInterfaceDemo](basics/GenericInterfaceDemo.java) | Один каталог хранит статусы заказов по числовым ключам, другой хранит настройки по текстовым ключам. Оба используют одну реализацию. |

## 2. Ограничения типов

| Пример | Что посмотреть |
| --- | --- |
| [BoundedTypesDemo](bounds/BoundedTypesDemo.java) | `T extends Number` даёт доступ к `doubleValue()`. Пересечение с `Comparable` позволяет сравнить значения и вернуть `BigDecimal` без приведения. |
| [OrderingDemo](bounds/OrderingDemo.java) | Сравниваем даты по естественному порядку и строки по длине. В обоих случаях сравнение определено для супертипа. |

## 3. Инвариантность, wildcard и PECS

| Пример | Что посмотреть |
| --- | --- |
| [InvarianceDemo](variance/InvarianceDemo.java) | `List<Integer>` можно читать через `List<? extends Number>`. Чтобы добавить дробное число, создаём отдельный `List<Number>`. |
| [PecsCopyDemo](variance/PecsCopyDemo.java) | `? extends T` читается как `T`, а в `? super T` можно записывать `T`. Один метод переносит целые числа в отчёт, а отчёт в список объектов. |
| [WildcardCaptureDemo](variance/WildcardCaptureDemo.java) | `List<?>` принимает списки разных типов. Вспомогательный метод захватывает неизвестный тип и переставляет элементы. |
| [FunctionVarianceDemo](variance/FunctionVarianceDemo.java) | `Function<? super T, ? extends R>` позволяет применить функцию для `CharSequence` к строкам и собрать полученные `Integer` в `List<Number>`. |

Параметр `T` связывает типы нескольких аргументов или результата. `?` обозначает один неизвестный тип. Для чтения через `? extends T` известна верхняя граница, а из `? super T` гарантированно читается только `Object`. [JLS о wildcard](https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-4.5.1) · [JLS о захвате wildcard](https://docs.oracle.com/javase/specs/jls/se17/html/jls-5.html#jls-5.1.10)

## 4. Стирание типов и проверка при выполнении

| Пример | Что посмотреть |
| --- | --- |
| [TypeErasureDemo](runtime/TypeErasureDemo.java) | У `ArrayList<String>` и `ArrayList<Integer>` один класс во время выполнения. `instanceof List<?>` проверяет, что объект является списком. |
| [RawTypeDemo](runtime/RawTypeDemo.java) | Запись через raw `List` кладёт `Integer` в список строк. Чтение как `String` заканчивается перехваченным `ClassCastException`. |
| [GenericArrayDemo](runtime/GenericArrayDemo.java) | Фабрика `String[]::new` сохраняет тип компонента. Попытка записать `Double` в массив `Integer[]` вызывает `ArrayStoreException`. |
| [GenericVarargsDemo](runtime/GenericVarargsDemo.java) | Метод объединяет списки через generic varargs. `@SafeVarargs` уместна здесь, потому что массив аргументов только читается и остаётся внутри метода. |
| [ClassTokenDemo](runtime/ClassTokenDemo.java) | `Class<T>` связывает ключ настройки и тип значения. `Class.cast()` проверяет его во время выполнения. |

[Конспект по Generics и PECS](../theory/Java.md#9-generics-и-pecs) · [JLS о стирании типов](https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-4.6)
