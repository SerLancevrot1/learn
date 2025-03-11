package ru.learn.generic.dto;

public class Camera extends Product {
    private int size;

    public Camera(String name, int price, int size) {
        super(name, price);
        this.size = size;
    }
}
