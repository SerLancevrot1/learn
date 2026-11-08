package ru.learn.patterns.factory.abstractt;

import ru.learn.patterns.factory.abstractt.glass.Glass;
import ru.learn.patterns.factory.abstractt.gpu.Gpu;

public interface PhonePartsFactory {
    Glass createGlass();
    Gpu createGpu();
}
