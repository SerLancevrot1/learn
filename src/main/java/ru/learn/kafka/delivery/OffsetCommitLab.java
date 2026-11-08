package ru.learn.kafka.delivery;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/** Сравнивает две последовательности обработки при выключенном enable.auto.commit. */
public final class OffsetCommitLab {
    private OffsetCommitLab() {
    }

    public enum CommitOrder {
        BEFORE_PROCESSING, AFTER_PROCESSING
    }

    /**
     * Опрос и синхронная обработка на потоке владельца Kafka consumer.
     * Если обработка или commit падают, вызывающий закрывает consumer и запускает новый
     * с тем же group.id. Продолжение poll() на том же клиенте могло бы перескочить запись.
     */
    public static int pollAndProcess(org.apache.kafka.clients.consumer.Consumer<String, String> consumer,
                                     Duration timeout, CommitOrder order,
                                     Consumer<ConsumerRecord<String, String>> process) {
        var records = consumer.poll(timeout);
        for (var record : records) {
            // poll уже продвинул локальную позицию на всю пачку. Коммитим точный offset
            // конкретной записи, иначе commitSync() без аргументов подтвердил бы всю пачку.
            var offsets = Map.of(new TopicPartition(record.topic(), record.partition()),
                    new OffsetAndMetadata(record.offset() + 1));

            if (order == CommitOrder.BEFORE_PROCESSING) {
                // Сбой после commit означает потерю обработки: группа возобновится со следующей записи.
                consumer.commitSync(offsets);
            }
            process.accept(record);
            if (order == CommitOrder.AFTER_PROCESSING) {
                // Сбой после побочного эффекта и до commit приводит к повторной обработке.
                consumer.commitSync(offsets);
            }
        }
        return records.count();
    }
}
