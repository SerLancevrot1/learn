package ru.learn.patterns.factory.abstractt;

import ru.learn.patterns.factory.abstractt.glass.AppleGlass;
import ru.learn.patterns.factory.abstractt.glass.SamsungGlass;
import ru.learn.patterns.factory.abstractt.gpu.AppleGpu;
import ru.learn.patterns.factory.abstractt.gpu.SamsungGpu;

public class PhoneFabric {

    public Phone buildPhone(String type) {
        if (type.equals("samsung")){
            return Phone.builder().glass(new SamsungGlass()).gpu(new SamsungGpu()).build();
        } else if (type.equals("apple")) {
            return Phone.builder().glass(new AppleGlass()).gpu(new AppleGpu()).build();
        } else {
            return null;
        }
    }
}
