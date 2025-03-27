package ru.learn.stream.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapTask {

    public static void main(String[] args) {
        HashMap<Integer, List<Long>> input = new HashMap<>();
        input.put(1, List.of(2L, 3L, 4L));
        input.put(2, List.of(5L, 6L, 7L));

        /*
        на выходе должна быть мапа
        1 - 2
        1 - 3
        1 - 4
        2 - 5
        2 - 6
        2 - 7
         */

        /*
        HashMap<Integer, Long> output = input
                .entrySet()
                .stream()
                .flatMap(entry -> {
                    Integer key = entry.getKey();
                    List<Long> val = entry.getValue();
                    return val.stream().map(num -> Map.entry(num, key));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        */
    }
}
