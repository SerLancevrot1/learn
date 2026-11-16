package ru.learn.collections;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Queue;

/** Обслуживание заявок по поступлению, по приоритету и с обоих концов очереди. */
public class QueueDemo {
    public static void main(String[] args) {
        fifo();
        priority();
        bothEnds();
    }

    private static void fifo() {
        Queue<String> requests = new ArrayDeque<>();
        requests.offer("Заявка 101: новый пароль");
        requests.offer("Заявка 102: выписка");
        System.out.println("Следующая без извлечения: " + requests.peek());

        String request;
        while ((request = requests.poll()) != null) {
            System.out.println("FIFO: " + request);
        }
        System.out.println("После обработки всех заявок: " + requests.poll());
    }

    private static void priority() {
        // Меньшее число означает более срочную заявку.
        Queue<Ticket> tickets = new PriorityQueue<>(Comparator.comparingInt(Ticket::priority)
                .thenComparingLong(Ticket::arrivalOrder));
        tickets.offer(new Ticket("Отчёт открывается медленно", 3, 1));
        tickets.offer(new Ticket("Оплата недоступна", 1, 2));
        tickets.offer(new Ticket("Вход недоступен", 1, 3));

        // У двух аварий одинаковый приоритет. Порядок поступления разрешает ничью.
        // Чтобы получить этот порядок, извлекаем голову. Итератор обходит элементы кучи.
        Ticket ticket;
        while ((ticket = tickets.poll()) != null) {
            System.out.println("По приоритету: " + ticket.subject());
        }
    }

    private static void bothEnds() {
        Deque<String> requests = new ArrayDeque<>();
        requests.offerLast("Обычная заявка");
        requests.offerFirst("Повтор после временного сбоя");
        System.out.println("Снять с начала: " + requests.pollFirst());
        System.out.println("Отменить последнюю заявку: " + requests.pollLast());
    }

    private record Ticket(String subject, int priority, long arrivalOrder) {
    }
}
