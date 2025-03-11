package ru.learn.patterns.factory.abstractt;


import ru.learn.patterns.factory.abstractt.glass.Glass;
import ru.learn.patterns.factory.abstractt.gpu.Gpu;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Phone {

    Gpu gpu;
    Glass glass;
}
