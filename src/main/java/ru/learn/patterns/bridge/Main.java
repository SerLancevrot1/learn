package ru.learn.patterns.bridge;

/**
 * Bridge — позволяет независимо добавлять виды уведомлений и каналы доставки.
 * Notification задаёт содержание сообщения, а Sender отвечает за отправку.
 */
public class Main {
    public static void main(String[] args) {
        Sender email = message -> System.out.println("Email: " + message);
        Sender sms = message -> System.out.println("SMS: " + message);

        new Reminder(email).send("Meeting at 10");
        new Reminder(sms).send("Meeting at 10");
        new Alert(sms).send("Server unavailable");
    }

    interface Sender {
        void deliver(String message);
    }

    abstract static class Notification {
        // Канал выбирается при создании уведомления и может сочетаться с любым его видом.
        private final Sender sender;

        Notification(Sender sender) {
            this.sender = sender;
        }

        final void send(String message) {
            sender.deliver(format(message));
        }

        abstract String format(String message);
    }

    static class Reminder extends Notification {
        Reminder(Sender sender) {
            super(sender);
        }

        @Override
        String format(String message) {
            return "Reminder: " + message;
        }
    }

    static class Alert extends Notification {
        Alert(Sender sender) {
            super(sender);
        }

        @Override
        String format(String message) {
            return "URGENT: " + message;
        }
    }
}
