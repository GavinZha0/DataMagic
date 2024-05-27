package com.ninestar.datapie.datamagic.socket;

import com.ninestar.datapie.datamagic.service.WebStompService;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.annotation.Resource;
import java.io.Serializable;

// this is a log4j2 appender
// used to forward log to UI via stomp service
// used to replace UDP

@Plugin(name = "WebSocketAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class WebSocketAppender extends AbstractAppender {
    // 一个阻塞队列
    // start a task to get log from loggerQueue and send to UI via stomp
    // but this is null in another task ?????
    private LoggerQueue loggerQueue  = LoggerQueue.getInstance();

    @Resource
    private static WebStompService stompService; // it is null here. how to fix it ???



    // create appender with the parameters which are from log4j2.-spring.xml
    @PluginFactory
    public static WebSocketAppender createAppender(@PluginAttribute("name") String name,
                                                   @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
                                                   @PluginElement("Layout") Layout layout,
                                                   @PluginElement("Filters") Filter filter)
    {
        if (name == null) { return null; }
        if (layout == null) { layout = PatternLayout.createDefaultLayout(); }

        // new an instant of WebSocketAppender
        return new WebSocketAppender(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    // constructor
    public WebSocketAppender(String name,
                             Filter filter,
                             Layout<? extends Serializable> layout,
                             boolean ignoreExceptions,
                             Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }


    @Override
    public void append(LogEvent event) {
        // ideally send message to UI here by stomp service
        // but stompService is null here ?????
        // so put message to a Q then get it send to UI periodically by task
        //loggerQueue.push(new String(getLayout().toByteArray(event)));
    }
}

