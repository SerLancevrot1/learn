package ru.learn.patterns.observer;

import java.util.ArrayList;

import lombok.Data;

@Data
public class WeatherData implements Subject {

    private ArrayList<Observer> observers = new ArrayList<>();
    private WeatherDataDto weatherDataDto;

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObserver() {
        observers.forEach(Observer::update);
    }
}
