package com.ninestar.datapie.datamagic.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// this is used to get a bean
// doesn't work in WebSocketAppender

@Component
public class SpringBeanUtils implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringBeanUtils.applicationContext == null) {
            SpringBeanUtils.applicationContext = applicationContext;
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    public static Object getBean(String name){
        if(applicationContext!=null){
            return applicationContext.getBean(name);
        }
        else{
            return null;
        }
    }
    public static <T> T getBean(Class<T> clazz){
        if(applicationContext!=null){
            return applicationContext.getBean(clazz);
        }
        else{
            return null;
        }
    }
    public static <T> T getBean(String name, Class<T> clazz){
        if(applicationContext!=null){
            return applicationContext.getBean(name, clazz);
        }
        else{
            return null;
        }
    }
    public static <T> T getBean(Class<? extends T> clazz, Class<T> targetClazz){
        return (T)getBean(clazz);
    }
}
