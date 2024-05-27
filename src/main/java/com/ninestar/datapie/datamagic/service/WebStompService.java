package com.ninestar.datapie.datamagic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // broadcast
    @MessageMapping("/cast")
    public void broadcast(Principal principal, StompHeaderAccessor accessor, @Payload String message) {
        // receive broadcast from front end
        String uid = principal.getName();
        logger.info("WS-" + accessor.getUser().getName() + "-cast: "+ message.length());
        //System.out.println("WS-" + accessor.getUser().getName() + "-cast: "+ message.length());
    }

    // group message
    @MessageMapping("/msg/{target}")
    public void multicast(@DestinationVariable(value = "target") String target, StompHeaderAccessor accessor, @Payload String message) {
        // receive multicast from front end
        //logger.info("WS-" + accessor.getUser().getName() + "-msg: "+ message.length());
        System.out.println("WS-" + accessor.getUser().getName() + "-msg: "+ message.length());
    }

    // peer to peer chat
    @MessageMapping("/chat/{target}")
    public void p2pMessage(@DestinationVariable(value = "target") String target, StompHeaderAccessor accessor, @Payload String message) {
        // receive p2p message from source user and send it to target user
        // source user is accessor.getUser().getName()
        // target is like "1_alg3". it means user id 1 and algorithm id 3.
        String dest[] = target.split("_");
        String targetUser = dest[0];

        if(accessor!=null){
            //logger.info("WS-" + accessor.getUser().getName() + "->" + target + ": " + message.length());
            System.out.println("WS-" + accessor.getUser().getName() + "->" + target + ": " + message.length());
        }
        else{
            //logger.info("WS-" + targetUser + "->" + target + ": " + message.length());
            System.out.println("WS-" + targetUser + "->" + target + ": " + message.length());
        }

        // forward message to target user if it is online
        SimpUser simpUser = userRegistry.getUser(targetUser);
        if(simpUser!=null){
            template.convertAndSendToUser(targetUser, "/user", message);
        }
        else{
            System.out.println("WS-target user " + target + " is not online!");
            //logger.warn("WS-target user " + target + " is not online!");
        }
    }
}
