package ru.learn.cuncurrent;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class Spoon {

    public Spoon(Eater eater) {
        this.eater = eater;
    }

    private Eater eater;

    public Eater getOwner() {
        return eater;
    }

    public void serOwner(Eater eater){
        this.eater = eater;
    }

    @SneakyThrows
    public void use() {
        Thread.sleep(1);
    }
}
