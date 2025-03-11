package ru.learn.patterns.adapter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DuckAdapter implements Duck {

    private final Turkey turkey;

    @Override
    public void quack() {
        turkey.gobble();
    }

    @Override
    public void fly() {
        turkey.heightJump();
    }
}
