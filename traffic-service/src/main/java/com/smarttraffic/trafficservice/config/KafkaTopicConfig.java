package com.smarttraffic.trafficservice.config;

import com.smarttraffic.trafficservice.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic trafficReadingsTopic() {
        // Single broker, single partition - fine for local dev.
        // In a real multi-broker deployment you'd raise partitions/replicas.
        return TopicBuilder.name(KafkaTopics.TRAFFIC_READINGS)
                .partitions(1)
                .replicas(1)
                .build();
    }

}