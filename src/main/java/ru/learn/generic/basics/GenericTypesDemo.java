package ru.learn.generic.basics;

/**
 * Показывает тип с одним параметром T и тип с двумя независимыми параметрами K и V.
 * При чтении компилятор уже знает тип каждого значения.
 */
public class GenericTypesDemo {

    /** Хранит одно значение того типа, который выбрал вызывающий код. */
    private static final class Box<T> {
        private final T value;

        private Box(T value) {
            this.value = value;
        }

        private T value() {
            return value;
        }
    }

    /** Хранит ключ и значение. Их типы могут различаться. */
    private record Entry<K, V>(K key, V value) {}

    public static void main(String[] args) {
        // Diamond <> позволяет вывести аргумент типа класса при вызове конструктора.
        Box<String> trackingCode = new Box<>("PKG-42");
        Box<Integer> itemCount = new Box<>(3);
        // Box<int> не скомпилируется. Для int используем ссылочный тип Integer.

        String code = trackingCode.value();
        int count = itemCount.value();
        Entry<String, Integer> shipment = new Entry<>(code, count);

        System.out.printf("Посылка %s, товаров: %d%n", shipment.key(), shipment.value());
    }
}
