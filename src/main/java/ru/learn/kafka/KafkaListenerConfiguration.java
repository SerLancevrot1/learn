package ru.learn.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.EOSMode;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;

/**
 * Создаёт две отдельные настройки listener-контейнеров:
 * одну с ручным подтверждением offset и вторую с Kafka-транзакцией.
 */
@Configuration
public class KafkaListenerConfiguration {

    /**
     * Фабрика, которую использует at-least-once listener.
     * Offset будет подтверждён только после явного acknowledgment.acknowledge().
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> atLeastOnceKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory) {
        // Контейнер — инфраструктура Spring, которая создаёт и запускает Kafka consumer.
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        // Использует consumer-настройки Spring Boot из application.properties.
        factory.setConsumerFactory(consumerFactory);
        // Запрещает автоматический commit: момент подтверждения задаёт код listener-а.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    /**
     * Фабрика для consume-transform-produce: контейнер начинает транзакцию
     * до вызова listener-а и завершает её после успешной обработки записи.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> exactlyOnceKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            KafkaAwareTransactionManager<Object, Object> kafkaTransactionManager) {
        // Создаём отдельный контейнер, чтобы транзакционные настройки
        // не меняли поведение at-least-once listener-а.
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        // Передаём фабрику Kafka consumers, сконфигурированную Spring Boot.
        factory.setConsumerFactory(consumerFactory);
        // Связывает обработку входной записи с транзакционным Kafka producer.
        factory.getContainerProperties().setKafkaAwareTransactionManager(kafkaTransactionManager);
        // EOS V2 — современный режим exactly-once processing в Spring Kafka.
        factory.getContainerProperties().setEosMode(EOSMode.V2);
        return factory;
    }
}
