package com.kafka.notification_service.kafka;

import com.kafka.notification_service.event.UserCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${app.kafka.user-created.retry.backoff.delay}")
    private Long BACK_OFF_DELAY;

    @Value("${app.kafka.user-created.retry.max.attempt}")
    private Integer MAX_ATTEMPTS;

    /*
    This creates the kafka topic
     */
    public NewTopic newTopic() {
        return new NewTopic("user-message", 3, (short) 1);
    }

    /*
     For String type of the topics errorhandler, consumer and producer
     */

    /*
    This is the default producer factory used to define the producing config to kafka topic from error handler
     */
    @Bean
    public ProducerFactory<Object, Object> producerFactory(KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(props);
    }

    /*
    This is the generic kafka template
     */
    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate(
            ProducerFactory<Object, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /*
    This is the generic default error handler.
    This control the retry of consuming and producing to the kafka topics
     */
    @Bean("errorHandlerForGenericType")
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, ex) ->
                        {
                            // Trigger the notification service
                            log.info("Sending message to the user-message.DLT after " + MAX_ATTEMPTS + " retries: {}", record.value());
                            return new TopicPartition(record.topic() + ".DLT", record.partition());
                        }
                );
        FixedBackOff backOff = new FixedBackOff(BACK_OFF_DELAY, MAX_ATTEMPTS);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory(KafkaProperties kafkaProperties) {

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "dltKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(
            KafkaProperties kafkaProperties,
            @Qualifier("errorHandlerForGenericType")
            DefaultErrorHandler errorHandler
    ) {

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /*
    This is the producer factory for the user-created-event-topic
     where key is of the Long type and the value is of JSON type
    */

    @Bean("userCreatedEventHandlerProducer")
    public ProducerFactory<Long, UserCreatedEvent> userCreatedProducerFactory(
            KafkaProperties kafkaProperties,
            Environment env) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, env.getProperty("app.kafka.user-created.key-serializer"));
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, env.getProperty("app.kafka.user-created.value-serializer"));
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean("kafka-template-for-user-created-event-topic")
    public KafkaTemplate<Long, UserCreatedEvent> userCreatedKafkaTemplate(
            @Qualifier("userCreatedEventHandlerProducer")
            ProducerFactory<Long, UserCreatedEvent> producerFactory) {

        return new KafkaTemplate<>(producerFactory);
    }

    @Bean("userCreatedErrorHandler")
    public DefaultErrorHandler userCreatedErrorHandler(
            @Qualifier("kafka-template-for-user-created-event-topic")
            KafkaTemplate<Long, UserCreatedEvent> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
                );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 3));
    }

    @Bean("userCreatedEventHandlerConsumer")
    public ConsumerFactory<Long, UserCreatedEvent> userCreatedConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            Environment env
    ) {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("app.kafka.user-created.group-id"));

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                env.getProperty("app.kafka.user-created.key-deserializer"));

        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                env.getProperty("app.kafka.user-created.value-deserializer"));

        // 👇 REQUIRED for JSON
        props.put("spring.json.trusted.packages",
                env.getProperty("app.kafka.user-created.trusted-packages"));

        props.put("spring.json.value.default.type",
                env.getProperty("app.kafka.user-created.value-default-type"));

        props.put("spring.json.use.type.headers", false); // important

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("userCreatedHandlerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Long, UserCreatedEvent>
    userCreatedKafkaListenerContainerFactory(
            @Qualifier("userCreatedEventHandlerConsumer")
            ConsumerFactory<Long, UserCreatedEvent> consumerFactory,
            @Qualifier("userCreatedErrorHandler") DefaultErrorHandler errorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<Long, UserCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // 👇 attach custom error handler ONLY here
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

}
