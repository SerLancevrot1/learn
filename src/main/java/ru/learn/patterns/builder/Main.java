package ru.learn.patterns.builder;

/**
 * Builder — собирает запрос из обязательного пути и необязательных настроек.
 * Промежуточные настройки хранятся в Builder. Вызов build() создаёт готовый неизменяемый Request.
 */
public class Main {
    public static void main(String[] args) {
        Request request = new Request.Builder("/orders")
                .timeoutSeconds(5)
                .authenticated()
                .build();
        System.out.println(request);
    }

    record Request(String path, int timeoutSeconds, boolean authenticated) {
        Request {
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
        }

        // Путь обязателен, а значения остальных параметров можно уточнить перед build().
        static class Builder {
            private final String path;
            private int timeoutSeconds = 30;
            private boolean authenticated;

            Builder(String path) {
                this.path = path;
            }

            Builder timeoutSeconds(int seconds) {
                timeoutSeconds = seconds;
                return this;
            }

            Builder authenticated() {
                authenticated = true;
                return this;
            }

            Request build() {
                return new Request(path, timeoutSeconds, authenticated);
            }
        }
    }
}
