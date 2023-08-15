package com.ninestar.datapie.datamagic.socket;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LoggerQueue {
    //队列大小
    public static final int QUEUE_MAX_SIZE = Integer.MAX_VALUE;
    private static final LoggerQueue alarmMessageQueue = new LoggerQueue();
    //阻塞队列
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_MAX_SIZE);

    public static LoggerQueue getInstance() {
        return alarmMessageQueue;
    }

    /**
     * 消息入队
     * @param log
     * @return
     */
    public boolean push(String log) {
        return this.queue.add(log);
    }

    /**
     * 消息出队
     * @return
     */
    public String pop() {
        String result = null;
        try {
            result = this.queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
