package ru.learn.generic.dto;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Phone extends Product {

    private String model;

    public Phone(String name, int price, String model) {
        super(name, price);
        this.model = model;
    }
}
