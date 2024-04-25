package com.ninestar.datapie.datamagic.service;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.socket.LoggerQueue;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@Component
public class RedisChannelListener implements MessageListener {
    private LoggerQueue loggerQueue  = LoggerQueue.getInstance();

    private final SimpMessagingTemplate simpMessagingTemplate;

    // import simpMessagingTemplate in config file and pass it into Listener
    // it will be null if you import it here
    public RedisChannelListener(final SimpMessagingTemplate template) {
        this.simpMessagingTemplate = template;
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        // receive msg from redis channel
        // ex: {"taskId": "1707357705334-0@tasks.task1.add@1", "retval": -0.5078692079266254, "status": 0}
        System.out.println("MsgQ recv: " + message.toString());
        /*
        JSONObject jsonMsg = new JSONObject(message.toString());
        String taskId[] = jsonMsg.get("taskId").toString().split("@");
        String msgId = taskId[0];
        String funcName = taskId[1];
        String userId = taskId[2];

        // send task result to user by websocket
        if(simpMessagingTemplate != null){
            System.out.println("Forward msg to user by ws");
            simpMessagingTemplate.convertAndSendToUser("1", "/feedback", jsonMsg.toString());
        }
        */
    }
}
