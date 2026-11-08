package ru.learn.patterns.observer;

/**
 * Observer — рассылает обновления погоды подписанным экранам.
 * WeatherData вызывает общий метод update(), а каждый экран сам решает, как показать данные.
 */
public class Main {
    public static void main(String[] args) {
        var weatherData = new WeatherData();

        var firstWeatherScreen = new FirstWeatherScreen(weatherData);
        var secondWeatherScreen = new SecondWeatherScreen(weatherData);

        weatherData.registerObserver(firstWeatherScreen);
        weatherData.registerObserver(secondWeatherScreen);

        // В этом примере сохранение данных и уведомление подписчиков вызываются отдельно.
        weatherData.setWeatherDataDto(new WeatherDataDto(20, 600));
        weatherData.notifyObserver();
        weatherData.setWeatherDataDto(new WeatherDataDto(23, 620));
        weatherData.notifyObserver();

    }
}
