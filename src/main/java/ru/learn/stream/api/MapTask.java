package ru.learn.stream.api;

import java.util.AbstractMap;
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

        var q = input
                .entrySet()
                .stream()
                .flatMap(f -> f.getValue().stream().map(m ->  new AbstractMap.SimpleEntry<>(m, f.getKey())))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        System.out.println(q);



    }
}
