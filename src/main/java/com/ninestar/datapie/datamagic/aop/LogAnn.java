package com.ninestar.datapie.datamagic.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * log annotation
 * 
 * @author Gavin Zhao
 *
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAnn
{
    public String title() default "";
    public LogType logType() default LogType.ACTION;
    public ActionType actionType() default ActionType.OTHER;
    public boolean saveReq() default true;
    public boolean saveRsp() default false;
}
