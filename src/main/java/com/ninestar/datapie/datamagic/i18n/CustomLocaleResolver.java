package com.ninestar.datapie.datamagic.i18n;

import cn.hutool.core.util.StrUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * register custom locale resolver
 *
 */
public class CustomLocaleResolver implements LocaleResolver {
    @Nullable
    private Locale defaultLocale;

    public void setDefaultLocale(@Nullable Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @Nullable
    public Locale getDefaultLocale() {
        return this.defaultLocale;
    }

    /**
     * every http request will trigger this function
     * so the language will be changed following every request
     * it's not good because we want to set language once and keep it
     */
    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        //get default language of application.properties
        Locale customLocale = Locale.getDefault();

        if(request.getHeader("Lang-Id") != null) {
            // get language from header
            // doesn't work here ???
            String language = request.getHeader("Lang-Id");
            if (!StrUtil.isEmpty(language)) {
                customLocale = StringUtils.parseLocaleString(language);
            }
        }
        else if(request.getHeader("Accept-Language") != null) {
            // get language from header
            customLocale = request.getLocale();

            // get language from url
            String language = request.getParameter("lang");
            if (!StrUtil.isEmpty(language)) {
                customLocale = StringUtils.parseLocaleString(language);
            }
        }
        return customLocale;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    }
}
