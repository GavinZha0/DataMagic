package com.ninestar.datapie.datamagic.consts;

public enum MatchType {
    /**
     * 等于
     */
    EQUAL("等于", "equal"),

    /**
     * 模糊查询
     */
    LIKE("模糊查询", "like"),

    /**
     * 为空
     */
    ISNULL("为空", "isNull"),

    /**
     * 两个时间中间
     */
    BetweenTime("两个时间中间", "betweenTime"),

    /**
     * 在值之内
     */
    In("在值之内", "in"),

    /**
     * 在值之间
     */
    Between("在值之间", "between"),

    /**
     * 大于
     */
    GT("大于", "gt"),

    /**
     * 大于等于
     */
    GE("大于等于", "ge"),

    /**
     * 小于
     */
    LT("小于", "lt"),

    /**
     * 小于等于
     */
    LE("小于等于", "le");

    private final String method;
    private final String desc;

    MatchType(String desc, String method) {
        this.method = method;
        this.desc = desc;
    }

    public String getMethod() {
        return method;
    }

    public String getDesc() {
        return desc;
    }
}
