package ru.learn.patterns.composite;

import java.util.List;

/**
 * Composite — позволяет одинаково запрашивать размер файла и целой папки.
 * Файл возвращает свой размер, а папка суммирует размеры дочерних Entry, включая вложенные папки.
 */
public class Main {
    public static void main(String[] args) {
        Entry tree = new Folder(List.of(
                new FileEntry(120),
                new Folder(List.of(new FileEntry(80), new FileEntry(50)))));
        System.out.println(tree.size());
    }

    interface Entry {
        long size();
    }

    record FileEntry(long size) implements Entry {
    }

    record Folder(List<Entry> children) implements Entry {
        Folder {
            children = List.copyOf(children);
        }

        // Вложенная папка сама подсчитает размер своего поддерева через тот же Entry.size().
        @Override
        public long size() {
            return children.stream().mapToLong(Entry::size).sum();
        }
    }
}
