package com.ninestar.datapie.datamagic.config;

import com.ninestar.datapie.datamagic.i18n.CustomLocaleResolver;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

@Configuration
public class I18nConfig implements WebMvcConfigurer{
    /**
     * register custom locale resolver
     * this function is not necessary if you just want to switch it automatically following browser language because LocaleChangeInterceptor will do it for you
     * but if you will get exception 'UnsupportedOperationException: Cannot change HTTP accept header - use a different locale resolution strategy' if you try to change it by url parameter 'lang'
     * so this is necessary for this project to switch language by url parameter
     */

    @Bean
    public LocaleResolver localeResolver() {
        // option #1 language is in session
        // set default language of session
        // it will take effect if http header doesn't have 'Accept-Language'
        //SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
        //sessionLocaleResolver.setDefaultLocale(Locale.CHINA);

        // option #2 language is in cookie
        //CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        //cookieLocaleResolver.setDefaultLocale(Locale.CHINA);

        // option #3 custom localeResolver to get language
        CustomLocaleResolver customLocaleResolver = new CustomLocaleResolver();
        customLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        return customLocaleResolver;
    }

    /**
     * register interceptor for language switch
     */

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor localeInterceptor = new LocaleChangeInterceptor();
        // monitor url parameter 'lang'
        // the language will be switched automatically if any url has this parameter
        localeInterceptor.setParamName("lang");
        registry.addInterceptor(localeInterceptor);
    }

    /**
     * override AcceptHeaderLocaleResolver to change language following http request setting
     * by default it user browser language
     * once a http request has 'lang=zh-CN', only this response will be set to zh-CN
     * because AcceptHeaderLocaleResolver will be invoked by every http request
     * this is similar with custom localeResolver
     * please disable the above two if enable this option
     */
/*
    // option #4 override AcceptHeaderLocaleResolver
    @Bean
    public AcceptHeaderLocaleResolver localeResolver(WebMvcProperties mvcProperties) {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                String locale = request.getParameter("lang");
                if(locale != null){
                    return StringUtils.parseLocaleString(locale);
                }
                else{
                    return super.resolveLocale(request);
                }
            }
        };

        localeResolver.setDefaultLocale(mvcProperties.getLocale());
        return localeResolver;
    }
 */
}
