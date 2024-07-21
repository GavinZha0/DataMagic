package com.ninestar.datapie.datamagic.config;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.servlet.ServletContext;
import javax.websocket.server.ServerContainer;
import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ServletContext servletContext;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //url of front end(ws://localhost:9527/ws)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // two brokers (one for app, one for users)
        registry.enableSimpleBroker("/topic", "/user");

        // prefix for all websocket message
        // all '/app/xxx' will be transferred to controller function which is marked by @MessageMapping
        //registry.setApplicationDestinationPrefixes("/app");

        //prefix for peer to peer message
        registry.setUserDestinationPrefix("/user");
    }


    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(1024 * 1024); // default : 64 * 1024
        registration.setSendBufferSizeLimit(5120 * 1024); // default : 512 * 1024
        registration.setSendTimeLimit(15 * 1000);

    }

    // comment it out because Lifecycle/test will fail -- Gavin!!!
    // if it doesn't work please release @Bean
    //@Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        // Check if attribute is set in the ServletContext
        ServerContainer serverContainer = (ServerContainer) this.servletContext.getAttribute("javax.websocket.server.ServerContainer");
        if (serverContainer == null) {
            logger.error("Could not initialize Websocket Container in Testcase.");
            return null;
        }

        ServletServerContainerFactoryBean factoryBean = new ServletServerContainerFactoryBean();
        factoryBean.setMaxTextMessageBufferSize(1024 * 1024);
        factoryBean.setMaxBinaryMessageBufferSize(5120 * 1024);
        return factoryBean;
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // used to verify authorization
        registration.interceptors(new ChannelInterceptor(){
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                // first connect from client
                // user x connect to server
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // get user Id from stomp header
                    List<String> nativeHeader = accessor.getNativeHeader("uid");

                    if (nativeHeader != null && !nativeHeader.isEmpty()) {
                        String uid = nativeHeader.get(0);
                        if (!StrUtil.isEmpty(uid)) {
                            Principal principal = new Principal() {
                                @Override
                                public String getName() {
                                    return nativeHeader.get(0);
                                }
                            };
                            //save user id as user-name in header
                            accessor.setUser(principal);
                            logger.info("WS-user " + uid + " connected!");
                            return message;
                        }
                    } else {
                        return null;
                    }
                }
                else if(accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())){
                    if(accessor.getUser()!=null){
                        logger.info("WS-user " + accessor.getUser().getName() + " disconnected!");
                        return null;
                    }
                }
                return message;
            }
        });
    }
}
