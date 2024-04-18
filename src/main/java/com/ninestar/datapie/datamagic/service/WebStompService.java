package com.ninestar.datapie.datamagic.service;

import com.ninestar.datapie.datamagic.socket.LoggerQueue;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import javax.websocket.server.ServerEndpoint;
import java.security.Principal;

@Component
@Controller
@ServerEndpoint("/ws")
public class WebStompService {

    @Resource
    private SimpMessagingTemplate template;

    @Resource
    private SimpUserRegistry userRegistry;

    private LoggerQueue loggerQueue = LoggerQueue.getInstance();

    // broadcast
    @MessageMapping("/cast")
    public void broadcast(Principal principal, StompHeaderAccessor accessor, @Payload String message) {
        String uid = principal.getName();
        //logger.info("WS-" + accessor.getUser().getName() + "-cast: "+ message.length());
        System.out.println("WS-" + accessor.getUser().getName() + "-cast: "+ message.length());
    }

    // group message
    @MessageMapping("/msg/{target}")
    @SendTo("/topic/msg")
    public void multicast(@DestinationVariable(value = "target") String target, StompHeaderAccessor accessor, @Payload String message) {
        //logger.info("WS-" + accessor.getUser().getName() + "-msg: "+ message.length());
        System.out.println("WS-" + accessor.getUser().getName() + "-msg: "+ message.length());
    }

    // peer to peer chat
    @MessageMapping("/chat/{target}")
    public void p2pMessage(@DestinationVariable(value = "target") String target, StompHeaderAccessor accessor, @Payload String message) {
        // target is like "1_alg3". it means user id 1 and algorithm id 3.
        String dest[] = target.split("_");

        if(accessor!=null){
            //logger.info("WS-" + accessor.getUser().getName() + "->" + target + ": " + message.length());
            System.out.println("WS-" + accessor.getUser().getName() + "->" + target + ": " + message.length());
        }
        else{
            //logger.info("WS-" + dest[0] + "->" + target + ": " + message.length());
            System.out.println("WS-" + dest[0] + "->" + target + ": " + message.length());
        }

        // forward message to target user if it is online
        SimpUser simpUser = userRegistry.getUser(dest[0]);
        if(simpUser!=null){
            template.convertAndSendToUser(dest[0], "/feedback", message);
        }
        else{
            System.out.println("WS-target user " + target + " is not online!");
            //logger.warn("WS-target user " + target + " is not online!");
        }
    }
}
