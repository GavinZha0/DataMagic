package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class I18nUtil {
    private static final Logger logger = LoggerFactory.getLogger(I18nUtil.class);

    private static MessageSource messageSource;

    public I18nUtil(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * get i18n message
     */
    public static String get(String msgKey) {
        try {
            // language is decided by browser language （'Accept-Language' in header of http request）
            // it can be switched by url parameter 'lang=zh-CN'
            Locale locale = LocaleContextHolder.getLocale();
            String i18nMsg = messageSource.getMessage(msgKey, null, locale);
            if(StrUtil.isEmpty(i18nMsg)){
                return msgKey;
            }
            else{
                return i18nMsg;
            }
        } catch (Exception e) {
            logger.error(e.toString());
            return msgKey;
        }
    }
}
