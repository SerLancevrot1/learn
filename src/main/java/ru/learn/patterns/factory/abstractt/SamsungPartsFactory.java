package ru.learn.patterns.factory.abstractt;

import ru.learn.patterns.factory.abstractt.glass.Glass;
import ru.learn.patterns.factory.abstractt.glass.SamsungGlass;
import ru.learn.patterns.factory.abstractt.gpu.Gpu;
import ru.learn.patterns.factory.abstractt.gpu.SamsungGpu;

public class SamsungPartsFactory implements PhonePartsFactory {
    @Override
    public Glass createGlass() {
        return new SamsungGlass();
    }

    @Override
    public Gpu createGpu() {
        return new SamsungGpu();
    }
}
