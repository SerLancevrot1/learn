package ru.learn.patterns.factory.abstractt;

public class PhoneFabric {

    public Phone buildPhone(String type) {
        return switch (type) {
            case "samsung" -> assemble(new SamsungPartsFactory());
            case "apple" -> assemble(new ApplePartsFactory());
            default -> null;
        };
    }

    // Оба компонента берутся из одной фабрики, чтобы сохранить согласованность семейства.
    public Phone assemble(PhonePartsFactory parts) {
        return Phone.builder()
                .glass(parts.createGlass())
                .gpu(parts.createGpu())
                .build();
    }
}
