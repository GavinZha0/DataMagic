package com.ninestar.datapie.framework.consts;

import com.ninestar.datapie.datamagic.utils.I18nUtil;

public enum UniformResponseCode {

    /** 成功 **/
    SUCCESS(0,"SUCCESS"),
    /** 失败 **/
    FAILURE(-1,"FAILURE"),

    EXCEPTION(201, "未知异常"),
    RUNTIME_EXCEPTION(202, "运行时异常"),
    NULL_POINTER_EXCEPTION(203, "空指针异常"),
    CLASS_CAST_EXCEPTION(204, "类型转换异常"),
    IO_EXCEPTION(205, "IO异常"),
    SYSTEM_EXCEPTION(210, "系统异常"),
    NOT_FOUND(404, "Not Found"),


    EXCEPTION_UNKNOWN(999, "Unknown issue"),
    /**
     * 1000～1999 区间表示参数错误
     */
    PARAMS_IS_INVALID(1001,"参数无效"),
    PARAMS_IS_BANK(1002,"参数为空"),
    PARAMS_TYPE_BIND_ERROR(1003,"参数类型错误"),
    PARAMS_NOT_COMPLETE(1004,"参数缺失"),

    /**
     * 2000～2999 区间表示用户错误
     */
    USER_NOT_EXIST(2001,"user.not.exist"),
    USER_NOT_LOGGED_IN(2002,"user.not.login"),
    USER_AUTH_FAILURE(2003,"user.auth.failure"),
    USER_IS_FROZEN(2004,"user.is.frozen"),
    USER_IS_EXPIRED(2005,"user.expired"),
    USER_NO_PERMIT(2006,"user.no.permit"),
    USER_SIGN_FAILURE(2007,"user.sign.mismatch"),
    USER_UNKNOWN_IDENTITY(2008, "user.unknown.identity"),
    USER_HAS_EXISTED(2009,"user.has.existed"),
    USER_PASSWORD_RESET_FAILED(2010, "user.reset.pwd.failure"),
    USER_REPLICATED_LOGIN(2011,"user.is.online"),
    USER_MISS_INFO(2012, "user.mis.info"),

    TOO_MANY_PWD_ENTER(2101, "too.many.pwd.try"),
    VERIFICATION_CODE_INCORRECT(2102,"verification.code.wrong"),
    PASSWORD_UNMATCHED(2103,"pwd.mismatch"),

    TOKEN_GENERATION_FAIL(2201,"token.generation.failure"),
    TOKEN_INVALID(2202,"token.invalid"),
    TOKEN_VERIFICATION_FAIL(2203,"token.verification.failure"),
    TOKEN_EXPIRED(2204,"token.expired"),


    REQUEST_INCOMPLETE(2401, "api.request.incomplete"),
    TARGET_RESOURCE_EXIST(2402, "api.target.exist"),
    TARGET_RESOURCE_NOT_EXIST(2403, "api.target.not.exist"),
    TARGET_RESOURCE_NOT_AVAILABLE(2404, "api.target.not.available"),

    DATASOURCE_NOT_EXIST(2405, "api.datasource.not.exist"),
    DATASOURCE_IN_USE(2406, "api.datasource.in.use"),
    DATASOURCE_CONFIG_ERR(2407, "api.datasource.config.error"),

    DATASET_NOT_EXIST(2408, "api.dataset.not.exist"),
    DATASET_IN_USE(2409, "api.dataset.in.use"),
    DATASET_CONFIG_ERR(2410, "api.dataset.config.error"),

    DATAVIEW_NOT_EXIST(2411, "api.dataview.not.exist"),
    DATAVIEW_IN_USE(2412, "api.dataview.in.use"),
    DATAVIEW_CONFIG_ERR(2413, "api.dataview.config.error"),

    DATAREPORT_NOT_EXIST(2414, "api.datareport.not.exist"),
    DATAREPORT_IN_USE(2415, "api.datareport.in.use"),
    DATAREPORT_CONFIG_ERR(2416, "api.datareport.config.error"),

    /**
     * 3000～3999 区间表示接口异常
     */
    API_EXCEPTION(3000, "api.exception"),
    API_EXCEPTION_NOT_FOUND(3002, "api.exception.not.exist"),
    API_EXCEPTION_REQ_FREQ(3003, "api.exception.req.freq"),
    API_EXCEPTION_RESUBMIT(3004, "api.exception.resubmit"),
    API_EXCEPTION_PARAM(3005, "api.exeception.param"),
    API_EXCEPTION_PARAM_MISS(3006, "api.exception.param.miss"),
    API_EXCEPTION_NOT_SUPPORT(3007, "api.exception.not.support"),
    API_EXCEPTION_PARAM_TYPE(3008, "api.exception.param.type"),
    API_EXCEPTION_SQL_EXE(3009, "api.exception.sql.exe"),

    SQL_MULTIPLE_SELECT_UNSUPPORT(4000, "sql.multiple.select.unsupport"),
    SQL_ONLY_SELECT_SUPPORT(4001, "sql.only.select.support"),
    SQL_SECURITY_RISK(4002, "sql.security.risk"),
    SQL_VALIDATION_EXCEPTION(4004, "sql.validate.exception"),
    SQL_UNKNOWN_VARIABLE(4005, "sql.unknown.variable"),
    SQL_UNSUPPORTED_AGGREGATION(4006, "sql.unsupported.aggregation"),
    SQL_AGGREGATION_EXCEPTION(4007, "sql.aggregation.exception"),


    ARRAY_EXCEPTION(11001, "数组异常"),
    ARRAY_OUT_OF_BOUNDS_EXCEPTION(11002, "数组越界异常"),

    JSON_SERIALIZE_EXCEPTION(30000, "序列化数据异常"),
    JSON_DESERIALIZE_EXCEPTION(30001, "反序列化数据异常"),

    READ_RESOURCE_EXCEPTION(31002, "读取资源异常"),
    READ_RESOURCE_NOT_FOUND_EXCEPTION(31003, "资源不存在异常"),

    DATA_EXCEPTION(32004, "数据异常"),
    DATA_NOT_FOUND_EXCEPTION(32005, "未找到符合条件的数据异常"),
    DATA_CALCULATION_EXCEPTION(32006, "数据计算异常"),
    DATA_COMPRESS_EXCEPTION(32007, "数据压缩异常"),
    DATA_DE_COMPRESS_EXCEPTION(32008, "数据解压缩异常"),
    DATA_HAS_EXISTED(32010, "已存在"),
    DATA_NOT_EXIST(32010, "不存在"),
    DATA_PARSE_EXCEPTION(32009, "数据转换异常"),

    ENCODING_EXCEPTION(33006, "编码异常"),
    ENCODING_UNSUPPORTED_EXCEPTION(33006, "编码不支持异常"),

    DATE_PARSE_EXCEPTION(34001, "日期转换异常"),

    MALE_SEND_EXCEPTION(35001, "邮件发送异常"),

    SYNC_LOCK_FAILURE(4001, "获取锁失败"),
    SYNC_LOCK_SUCCESS(4002, "获取锁成功"),
    SYNC_LOCK_MANY_REQ(4003, "请求太多"),
    SYNC_LOCK_NOT_ENOUGH_STOCK(4004, "库存不够");


    private Integer code;

    private String msg;

    UniformResponseCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    // get i18n message in properties file
    public String getMsg() {
        return I18nUtil.get(msg);
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
