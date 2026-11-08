package ru.learn.patterns.templatemethod;

/**
 * Template Method — задаёт общий порядок экспорта отчёта.
 * Базовый класс фиксирует этапы, а подклассы определяют только формат содержимого.
 */
public class Main {
    public static void main(String[] args) {
        new TextReport().export("Orders: 3");
        new HtmlReport().export("Orders: 3");
    }

    abstract static class ReportExporter {
        // final фиксирует порядок этапов, а format() оставляет подклассам выбор представления.
        final void export(String content) {
            System.out.println("Export started");
            System.out.println(format(content));
            System.out.println("Export finished");
        }

        abstract String format(String content);
    }

    static class TextReport extends ReportExporter {
        @Override
        String format(String content) {
            return content;
        }
    }

    static class HtmlReport extends ReportExporter {
        @Override
        String format(String content) {
            String escaped = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            return "<p>" + escaped + "</p>";
        }
    }
}
