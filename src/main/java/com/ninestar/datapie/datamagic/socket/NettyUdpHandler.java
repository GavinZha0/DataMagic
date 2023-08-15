package com.ninestar.datapie.datamagic.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.bridge.MlTrainEpochIndType;
import com.ninestar.datapie.datamagic.service.WebStompService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

// log is redirected to UDP in config log4j2-spring
// this is used to forward log message to UI via stomp
// WebSocketAppender is another solution which can replace this UDP
// a better solution is to use zeroMQ in log4j2-spring

@Component
public class NettyUdpHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    // used to resolve null point issue
    // stompService will be null without this
    private static NettyUdpHandler nettyUdpHandler;

    @Resource
    private WebStompService stompService;

    private JSONObject msgChat = new JSONObject();
    private Map<String, String> targetMap = new HashMap<>();

    public NettyUdpHandler() {
    }

    // used to resolve null point issue
    // stompController will be null without this
    @PostConstruct
    public void init(){
        nettyUdpHandler = this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Netty UDP channelActive");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        try {
            String strMsg = msg.content().toString(CharsetUtil.UTF_8);
            if(strMsg.startsWith("Async-Task") && strMsg.contains("TARGET>>>")){
                // log start, bind target and task
                // log is from AsyncTaskService. a task servers one training which bind to one target (like 1_model3)
                String task = strMsg.substring(0, strMsg.indexOf("| "));
                String target = strMsg.substring(strMsg.indexOf("TARGET>>>")+9);
                target = target.replace("\n", "");
                nettyUdpHandler.targetMap.put(task, target);
                return;
            }
            else if(strMsg.startsWith("Async-Task") && strMsg.contains("TARGET<<<")){
                // log end, remove target and task
                String task = strMsg.substring(0, strMsg.indexOf("| "));
                nettyUdpHandler.targetMap.remove(task);
                return;
            }

            // pattern="%t| %d{yyyy-MM-dd HH:mm:ss} %-5level -> %msg%n"
            if(strMsg.contains("| ")){
                // find task/target
                String task = strMsg.substring(0, strMsg.indexOf("| "));
                String target = nettyUdpHandler.targetMap.get(task);
                if(!StrUtil.isEmpty(target)){
                    String dest[] = target.split("_");
                    // remove task from log
                    strMsg = strMsg.substring(strMsg.indexOf("| ")+2);
                    msgChat.clear();
                    if(strMsg.contains("Score at iteration")){
                        // DL4J score in log
                        // this is the progress of training of DL4J
                        //2022-05-15 11:32:08,967 INFO -> Score at iteration 880 is 0.10113004595041275
                        String segs[] = strMsg.split(" ");
                        MlTrainEpochIndType trainInd = new MlTrainEpochIndType();
                        trainInd.stage = "train";
                        trainInd.iterator = Integer.parseInt(segs[segs.length-3]);
                        trainInd.score = Float.parseFloat(segs[segs.length-1]);

                        msgChat.set("trainInd", trainInd);
                        // send to UI via stomp (websocket)
                        nettyUdpHandler.stompService.p2pMessage(target, null, msgChat.toString());
                    }
                    else{
                        msgChat.set("logs", strMsg);
                        // send to UI via stomp (websocket)
                        nettyUdpHandler.stompService.p2pMessage(target, null, msgChat.toString()+"\n");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
