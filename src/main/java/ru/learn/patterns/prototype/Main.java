package ru.learn.patterns.prototype;

import java.util.ArrayList;
import java.util.List;

/**
 * Prototype — создаёт новый отчёт из уже настроенного образца.
 * Копия наследует разделы шаблона и получает собственный список для дальнейших изменений.
 */
public class Main {
    public static void main(String[] args) {
        Report template = new Report(List.of("Summary", "Details"));
        Report draft = template.copy();
        draft.addSection("Appendix");

        System.out.println(template.sections());
        System.out.println(draft.sections());
    }

    interface Prototype<T> {
        T copy();
    }

    static class Report implements Prototype<Report> {
        private final List<String> sections;

        Report(List<String> sections) {
            this.sections = new ArrayList<>(sections);
        }

        @Override
        public Report copy() {
            // Конструктор создаёт отдельный список, чтобы изменения копии не затронули шаблон.
            return new Report(sections);
        }

        void addSection(String section) {
            sections.add(section);
        }

        List<String> sections() {
            return List.copyOf(sections);
        }
    }
}
