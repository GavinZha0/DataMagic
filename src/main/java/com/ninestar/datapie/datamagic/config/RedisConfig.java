package com.ninestar.datapie.datamagic.config;

import com.ninestar.datapie.datamagic.service.RedisChannelListener;
import com.ninestar.datapie.datamagic.service.RedisStreamListener;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.Resource;
import java.time.Duration;

@Data
@Configuration
public class RedisConfig {

    @Value("${spring.redis.msg.enabled}")
    public Boolean redisMsgEnabled;

    @Value("${spring.redis.stream.request}")
    public String reqStream;

    @Value("${spring.redis.stream.response}")
    private String rspStream;

    @Value("${spring.redis.consumer.group}")
    private String consumerGroup;

    @Value("${spring.redis.consumer.name}")
    private String consumerName;

    @Value("${spring.redis.channel.feedback}")
    private String channelFeedback;

    @Value("${stomp.channel.feedback}")
    private String stompFeedback;

    @Resource
    private SimpMessagingTemplate simpMessagingTemplate;

    @Resource
    RedisStreamListener redisStreamListener;

    @Resource
    RedisChannelListener redisChannelListener;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory, RedisChannelListener listener) {
        if(redisMsgEnabled) {
            // listen on msg channel
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.addMessageListener(new RedisChannelListener(simpMessagingTemplate), ChannelTopic.of(channelFeedback));
            return container;
        } else {
            return null;
        }
    }

    @Bean
    public StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ?> streamMessageListenerContainerOptions(){
        return StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(10))
                .build();
    }
    @Bean
    public StreamMessageListenerContainer streamMessageListenerContainer(RedisConnectionFactory factory,
               StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ?> streamMessageListenerContainerOptions){
        StreamMessageListenerContainer listenerContainer = StreamMessageListenerContainer.create(factory,
                streamMessageListenerContainerOptions);
        listenerContainer.start();
        return listenerContainer;
    }

    @Bean
    public Subscription subscription(StreamMessageListenerContainer streamMessageListenerContainer){
        if(redisMsgEnabled) {
            // subscribe a stream and auto ack when receive msg
            Subscription subscription = streamMessageListenerContainer.receiveAutoAck(
                    Consumer.from(consumerGroup, consumerName),
                    StreamOffset.create(rspStream, ReadOffset.lastConsumed()),
                    redisStreamListener
            );
            return subscription;
        } else {
            return null;
        }
    }
}

