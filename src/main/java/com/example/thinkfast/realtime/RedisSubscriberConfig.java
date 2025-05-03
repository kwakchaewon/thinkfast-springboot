package com.example.thinkfast.realtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisSubscriberConfig {

    // Redis 에서 수신한 메시지를 java 객체의 메서드로 위임하는 어댑터
    // Redis 에서 메시지가 오면 RedisSubscriber.onMessage() 호출
    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }

    // Redis 메시지 수신 관리 컨테이너
    @Bean
    public RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter adapter) {

        // Redis 연결 설정 후 admin-notifications 채널 구독
        // admin-notifications 메시지 발행 시 RedisSubscriber.onMessage()
        // 해당 채널에 메시지가 도착하면 RedisSubscriber 의 메서드 호출
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(adapter, new ChannelTopic("admin-notifications"));
        return container;
    }
}
