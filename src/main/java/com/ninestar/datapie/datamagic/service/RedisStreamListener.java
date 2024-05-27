package com.ninestar.datapie.datamagic.service;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Map;


@Component
public class RedisStreamListener implements StreamListener<String, MapRecord<String, String, String>> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    RedisConfig redisConfig;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final SimpMessagingTemplate simpMessagingTemplate;

    public RedisStreamListener(final SimpMessagingTemplate template) {
        this.simpMessagingTemplate = template;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        // receive msg from redis STREAM
        redisTemplate.opsForStream().acknowledge(message.getStream(),redisConfig.getConsumerGroup(), message.getId());

        String msgId = message.getId().getValue();
        logger.info("MsgQ recv id: " + msgId);
        Map<String, String> streamMsg = message.getValue();
        Long rspTime = Long.parseLong(msgId.split("-")[0]);
        Long reqTime = Long.parseLong(streamMsg.get("reqMsgId").split("-")[0]);
        Long duration = rspTime - reqTime; // ms
        String currentTime = new SimpleDateFormat("HH:mm:ss").format(duration);
        logger.info("Duration: " + currentTime);
        // save result to db - to do


        JSONObject jsonMsg = new JSONObject(streamMsg);
        // send task result to user by websocket
        if(simpMessagingTemplate != null && streamMsg.containsKey("userId")){
            logger.info("Forward msg to user by ws");
            simpMessagingTemplate.convertAndSendToUser(streamMsg.get("userId"), redisConfig.getWsChannel(), jsonMsg.toString());
        }
    }
}
