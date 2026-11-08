package ru.learn.patterns.chainofresponsibility;

/**
 * Chain of Responsibility — передаёт тикет по цепочке, пока его кто-нибудь не обработает.
 * Каждое звено обрабатывает знакомую категорию и передаёт остальные тикеты дальше.
 */
public class Main {
    public static void main(String[] args) {
        // Последнее звено сообщает клиенту, что ни один обработчик не принял тикет.
        Handler unhandled = ticket -> false;
        Handler chain = new PasswordResetHandler(new BillingHandler(unhandled));

        for (String ticket : new String[]{"password", "billing", "other"}) {
            if (!chain.handle(ticket)) {
                System.out.println("No handler for " + ticket);
            }
        }
    }

    interface Handler {
        boolean handle(String ticket);
    }

    record PasswordResetHandler(Handler next) implements Handler {
        @Override
        public boolean handle(String ticket) {
            if ("password".equals(ticket)) {
                System.out.println("Reset password");
                return true;
            }
            return next.handle(ticket);
        }
    }

    record BillingHandler(Handler next) implements Handler {
        @Override
        public boolean handle(String ticket) {
            if ("billing".equals(ticket)) {
                System.out.println("Forward to billing");
                return true;
            }
            return next.handle(ticket);
        }
    }
}
