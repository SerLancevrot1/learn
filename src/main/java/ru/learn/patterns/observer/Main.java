package ru.learn.patterns.observer;

public class Main {
    public static void main(String[] args) {
        var weatherData = new WeatherData();

        var firstWeatherScreen = new FirstWeatherScreen(weatherData);
        var secondWeatherScreen = new SecondWeatherScreen(weatherData);

        weatherData.registerObserver(firstWeatherScreen);
        weatherData.registerObserver(secondWeatherScreen);

        weatherData.setWeatherDataDto(new WeatherDataDto(20, 600));
        weatherData.notifyObserver();
        weatherData.setWeatherDataDto(new WeatherDataDto(23, 620));
        weatherData.notifyObserver();

    }
}
