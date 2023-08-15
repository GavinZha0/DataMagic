package com.ninestar.datapie.datamagic.socket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.io.IoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.logging.log4j.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "netty.udp")
public class NettyUdpServer implements ApplicationRunner{
    private final static Logger logger = LoggerFactory.getLogger(NettyUdpServer.class);

    public static Integer port;
    public void setPort(Integer port) {
        this.port = port;
    }
    public static boolean isReady = false;

    //表示服务器连接监听线程组，专门接受 accept 新的客户端client 连接
    private static EventLoopGroup bossGroup  = new NioEventLoopGroup();

    //创建用于处理网络操作的线程组
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();

    //1，创建netty bootstrap 启动类
    private static Bootstrap serverBootstrap = new Bootstrap();

    /**
     * 启动服务
     */
    public void start() {
        try {
            //2、设置boostrap 的eventLoopGroup线程组
            serverBootstrap.group(bossGroup)
                    .channel(NioDatagramChannel.class)  // 设置NIO UDP连接通道
                    .option(ChannelOption.SO_BROADCAST, true) // 设置通道参数 SO_BROADCAST广播形式
                    .handler(new NettyUdpHandler()); // 绑定server，通过调用sync（）方法异步阻塞，直到绑定成功

            //绑定端口，同步等待
            ChannelFuture cf = serverBootstrap.bind(this.port).sync();
            logger.info("------ NettyUdpServer is listening on " + cf.channel().localAddress());
            //监听服务器端口关闭
            cf.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // redirect system.out.print to log4j2
        // the progress of AI training can't be printed synchronized but print all at same time
        // so how to track the progress of training ????
        //System.setOut(
        //        IoBuilder.forLogger(LogManager.getLogger("system.out"))
        //                .setLevel(Level.INFO)
        //                .buildPrintStream()
        //);


        // don't start it if we don't use it
        new NettyUdpServer().start();
    }
}
