package com.example.auctionnotification.common.config;

import com.example.auctionnotification.notification.event.NotificationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootStrapServers;

    private ConsumerFactory<String, NotificationEvent> notificationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        JacksonJsonDeserializer<NotificationEvent> deserializer = new JacksonJsonDeserializer<>(NotificationEvent.class);
        deserializer.addTrustedPackages(
                "com.example.auction.domain.notification.event",
                "com.example.auctionnotification.notification.event");
        deserializer.ignoreTypeHeaders();

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> eventKafkaListenerContainerFactory(
            KafkaTemplate<String, NotificationEvent> notificationEventKafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(notificationEventKafkaTemplate);
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(notificationConsumerFactory());
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}