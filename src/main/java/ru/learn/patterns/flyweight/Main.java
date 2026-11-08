package ru.learn.patterns.flyweight;

import java.util.HashMap;
import java.util.Map;

/**
 * Flyweight — экономит память за счёт совместного использования одинаковых данных.
 * Деревья хранят свои координаты, а неизменяемый стиль переиспользуется для каждого вида.
 */
public class Main {
    public static void main(String[] args) {
        var styles = new TreeStyles();
        Tree first = new Tree(1, 3, styles.get("oak"));
        Tree second = new Tree(4, 7, styles.get("oak"));

        System.out.println(first);
        System.out.println(second);
        System.out.println("Shared style: " + (first.style() == second.style()));
    }

    record Tree(int x, int y, TreeStyle style) {
    }

    record TreeStyle(String species, String sprite) {
    }

    static class TreeStyles {
        // Все деревья одного вида получают одну ссылку на стиль из этого реестра.
        private final Map<String, TreeStyle> bySpecies = new HashMap<>();

        TreeStyle get(String species) {
            return bySpecies.computeIfAbsent(species,
                    name -> new TreeStyle(name, name + ".png"));
        }
    }
}
