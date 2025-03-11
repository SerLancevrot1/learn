package ru.learn.patterns.observer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeatherDataDto {

    private int temperature;
    private int pressure;
}
