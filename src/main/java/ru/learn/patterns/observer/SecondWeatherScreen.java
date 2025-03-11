package ru.learn.patterns.observer;

import lombok.Data;

@Data
public class SecondWeatherScreen implements Observer, Display {

    private WeatherData weatherData;

    public SecondWeatherScreen(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    private WeatherDataDto weatherDataDto;

    @Override
    public void update() {
        this.weatherDataDto = weatherData.getWeatherDataDto();
        display();
    }

    @Override
    public void display() {
        System.out.println(this.getClass() + " " + weatherDataDto);
    }

}
