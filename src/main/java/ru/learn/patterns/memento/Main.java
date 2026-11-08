package ru.learn.patterns.memento;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Memento — позволяет откатить редактор к сохранённому состоянию.
 * История хранит снимки, а редактор сам сохраняет и восстанавливает их содержимое.
 */
public class Main {
    public static void main(String[] args) {
        var editor = new Editor();
        Deque<Editor.Snapshot> history = new ArrayDeque<>();

        editor.append("Hello");
        history.push(editor.save());
        editor.append(", world");
        System.out.println(editor.text());

        editor.restore(history.pop());
        System.out.println(editor.text());
    }

    static class Editor {
        private String text = "";

        void append(String suffix) {
            text += suffix;
        }

        String text() {
            return text;
        }

        Snapshot save() {
            return new Snapshot(text);
        }

        void restore(Snapshot snapshot) {
            text = snapshot.text;
        }

        // История хранит снимок, а к его содержимому обращается только Editor.
        static final class Snapshot {
            private final String text;

            private Snapshot(String text) {
                this.text = text;
            }
        }
    }
}
