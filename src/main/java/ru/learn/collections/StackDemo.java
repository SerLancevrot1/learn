package ru.learn.collections;

import java.util.ArrayDeque;
import java.util.Deque;

/** Показывает стек отмены действий в редакторе. Последнее изменение отменяется первым. */
public class StackDemo {

    public static void main(String[] args) {
        Deque<String> undo = new ArrayDeque<>();
        undo.push("Вернуть прежний заголовок");
        undo.push("Удалить вставленное изображение");
        undo.push("Вернуть удалённый абзац");

        System.out.println("Следующая отмена: " + undo.peek());
        // push и pop работают с одним концом. pop на пустом стеке бросает исключение.
        while (!undo.isEmpty()) {
            System.out.println("Отменяем: " + undo.pop());
        }
    }
}
