package ru.learn.generic.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Samsung extends Phone {

    private int version;
    public Samsung(String name, int price, String model, int version) {
        super(name, price, model);
        this.version = version;
    }
}
