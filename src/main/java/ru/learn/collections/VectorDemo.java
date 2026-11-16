package ru.learn.collections;

import java.util.Vector;

/** Vector в старом API. Отдельные вызовы синхронизированы, общий шаг защищён одним монитором. */
public class VectorDemo {
    public static void main(String[] args) {
        Vector<String> enabledReports = new Vector<>();
        enabledReports.addElement("Дневной отчёт");

        // Без общего блока два потока могут оба увидеть отсутствие отчёта и оба добавить его.
        synchronized (enabledReports) {
            if (!enabledReports.contains("Месячный отчёт")) {
                enabledReports.add("Месячный отчёт");
            }
        }
        System.out.println("Включённые отчёты: " + enabledReports);
    }
}
