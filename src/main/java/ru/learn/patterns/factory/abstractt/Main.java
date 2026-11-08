package ru.learn.patterns.factory.abstractt;

/**
 * Abstract Factory — создаёт согласованные семейства деталей телефона.
 * Клиент выбирает фабрику Apple или Samsung, а сборщик работает с общим интерфейсом PhonePartsFactory.
 */
public class Main {

    public static void main(String[] args) {
        var phoneFabric = new PhoneFabric();

        System.out.println(phoneFabric.assemble(new SamsungPartsFactory()));
        System.out.println(phoneFabric.assemble(new ApplePartsFactory()));
    }
}
