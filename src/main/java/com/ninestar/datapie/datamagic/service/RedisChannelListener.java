package com.ninestar.datapie.datamagic.service;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;


@Component
public class RedisChannelListener implements MessageListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static RedisChannelListener redisListener;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Resource
    private SimpUserRegistry userRegistry;

    @Resource
    private RedisConfig redisConfig;

    @PostConstruct
    public void init(){ redisListener = this; }

    public RedisChannelListener(final SimpMessagingTemplate template) {
        this.simpMessagingTemplate = template;
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        // receive msg from redis channel
        // ex: {"userId": 3, "payload": {"code": 1, "task": "ml_algo_1", "progress": 0.9}}
        String redisChannel = new String(message.getChannel());
        logger.info("Channel " + redisChannel + " recv: " + message.toString());

        // get target user
        JSONObject jsonMsg = new JSONObject(message.toString());
        String userId = jsonMsg.get("userId").toString();

        // check if the user is online
        SimpUser simpUser = redisListener.userRegistry.getUser(userId);
        if(simpUser!=null && simpMessagingTemplate != null){
            String wsChannel = redisListener.redisConfig.getWsChannel();
            // forward message to user via websocket
            logger.info("Forward msg to user " + userId + " via ws");
            simpMessagingTemplate.convertAndSendToUser(userId, wsChannel, jsonMsg.get("payload").toString());
        }
    }
}
