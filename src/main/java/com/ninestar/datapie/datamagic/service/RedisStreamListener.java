package com.ninestar.datapie.datamagic.service;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.config.RedisConfig;
import com.ninestar.datapie.datamagic.socket.LoggerQueue;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;


@Component
public class RedisStreamListener implements StreamListener<String, MapRecord<String, String, String>> {
    private LoggerQueue loggerQueue  = LoggerQueue.getInstance();

    @Resource
    RedisConfig redisConfig;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ?> streamMessageListenerContainerOptions;

    private final SimpMessagingTemplate simpMessagingTemplate;

    // import simpMessagingTemplate in config file and pass it into Listener
    // it will be null if you import it here
    public RedisStreamListener(final SimpMessagingTemplate template) {
        this.simpMessagingTemplate = template;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        // receive msg from redis stream
        redisTemplate.opsForStream().acknowledge(message.getStream(),redisConfig.getConsumerGroup(), message.getId());

        String msgId = message.getId().getValue();
        System.out.println("MsgQ recv id: " + msgId);
        Map<String, String> streamMsg = message.getValue();
        Long rspTime = Long.parseLong(msgId.split("-")[0]);
        Long reqTime = Long.parseLong(streamMsg.get("reqMsgId").split("-")[0]);
        Long duration = rspTime - reqTime; // ms
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(duration);
        System.out.println("Duration: " + currentTime);
        // save result to db - to do


        JSONObject jsonMsg = new JSONObject(streamMsg);
        // send task result to user by websocket
        if(simpMessagingTemplate != null && streamMsg.containsKey("userId")){
            System.out.println("Forward msg to user by ws");
            simpMessagingTemplate.convertAndSendToUser(streamMsg.get("userId"), redisConfig.getStompFeedback(), jsonMsg.toString());
        }
    }
}
