package ru.learn.patterns.factory.abstractt;

import ru.learn.patterns.factory.abstractt.glass.AppleGlass;
import ru.learn.patterns.factory.abstractt.glass.Glass;
import ru.learn.patterns.factory.abstractt.gpu.AppleGpu;
import ru.learn.patterns.factory.abstractt.gpu.Gpu;

public class ApplePartsFactory implements PhonePartsFactory {
    @Override
    public Glass createGlass() {
        return new AppleGlass();
    }

    @Override
    public Gpu createGpu() {
        return new AppleGpu();
    }
}
