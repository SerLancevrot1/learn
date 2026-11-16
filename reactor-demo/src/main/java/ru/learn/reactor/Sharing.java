package ru.learn.reactor;

import reactor.core.publisher.Sinks;

final class Sharing {
    private Sharing() {
    }

    static Sinks.Many<String> recentUpdates() {
        // Новый подписчик получит два последних обновления и затем новые события.
        // Такой replay хранит историю в памяти процесса.
        return Sinks.many().replay().limit(2);
    }

    static Sinks.Many<String> liveUpdates() {
        // Подписчик без спроса пропустит событие. Подписчики со спросом его получат.
        // При отсутствии получателей tryEmitNext вернёт результат ошибки.
        return Sinks.many().multicast().directBestEffort();
    }

    static void sendUpdate(Sinks.Many<String> updates, String value) {
        // В лабораторной один producer. Проверка помогает заметить ошибку сценария.
        // В сервисе реакция зависит от причины и требований к потере событий.
        Sinks.EmitResult result = updates.tryEmitNext(value);
        if (result.isFailure()) {
            throw new IllegalStateException("Событие не принято: " + result);
        }
    }
}
