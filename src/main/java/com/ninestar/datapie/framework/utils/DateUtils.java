package com.ninestar.datapie.framework.utils;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {

    public static final String DATE_BASE_REGEX = "\\d{4}-\\d{1,2}-\\d{1,2}";
    public static final String SEPARATOR_REGEX = "(T?|\\s*)";

    public static final String DATE_REGEX = "^\\s*" + DATE_BASE_REGEX + "\\s*$";
    public static final String DATE_H_REGEX = "^\\s*" + DATE_BASE_REGEX + SEPARATOR_REGEX + "\\d\\d\\s*$";
    public static final String DATE_HM_REGEX = "^\\s*" + DATE_BASE_REGEX + SEPARATOR_REGEX + "\\d{1,2}:\\dd{1,2}\\s*$";
    public static final String DATE_HMS_REGEX = "^\\s*" + DATE_BASE_REGEX + SEPARATOR_REGEX + "\\d{1,2}:\\d{1,2}:\\d{1,2}\\s*$";
    public static final String DATE_HMS_M_REGEX = "^\\s*" + DATE_BASE_REGEX + SEPARATOR_REGEX + "\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}\\s*$";

    private enum DATE_FORMAT_ENUM {

        DATE(Pattern.compile(DATE_REGEX), "yyyy-M-d"),
        DATE_H(Pattern.compile(DATE_H_REGEX), "yyyy-M-d H"),
        DATE_HM(Pattern.compile(DATE_HM_REGEX), "yyyy-M-d H:m"),
        DATE_HMS(Pattern.compile(DATE_HMS_REGEX), "yyyy-M-d H:m:s"),
        DATE_HMS_M(Pattern.compile(DATE_HMS_M_REGEX), "yyyy-M-d H:m:s.SSS");

        private Pattern pattern;
        private String format;

        DATE_FORMAT_ENUM(Pattern pattern, String format) {
            this.pattern = pattern;
            this.format = format;
        }

        static Date formatDate(String timeString) throws Exception {
            timeString = prepare(timeString);
            for (DATE_FORMAT_ENUM formatEnum : values()) {
                Matcher matcher = formatEnum.pattern.matcher(timeString);
                if (matcher.find()) {
                    SimpleDateFormat sdf = new SimpleDateFormat(formatEnum.format);
                    return sdf.parse(timeString);
                }
            }
            throw new Exception("Unparseable date: " + timeString);
        }

        private static String prepare(String timeString) {
            String s = null;
            if (!StringUtils.isEmpty(timeString)) {
                if (timeString.contains("-")) {
                    s = timeString;
                } else if (timeString.contains("/")) {
                    s = timeString.replaceAll("[/]", "-");
                } else {
                    s = timeString.substring(0, 4) + "-" + timeString.substring(4, 6) + "-" + timeString.substring(6);
                }
                int dotStart = s.indexOf(".") + 1;
                boolean hasDot = dotStart > 0;
                int msLength = dotStart + 3;
                int overMsLength = s.length() - msLength;
                int lessMsLength = msLength - s.length();

                if (hasDot && overMsLength >= 0) {
                    return s.substring(0, s.length() - overMsLength);
                } else if (hasDot && lessMsLength >= 0) {
                    for (int i = 0; i < lessMsLength; i++) {
                        s += "0";
                    }
                    return s;
                }

                if (hasDot) {
                    s += ".000";
                }
            }

            return s;
        }
    }

    public static Date currentDate() {
        return new Date();
    }

    public static long getNowMilliSecondTime() {
        return currentDate().getTime();
    }


    public static String getNowDateYYYYMM() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");

        return formatter.format(currentDate());
    }

    public static String getNowDateYYYYMMDD() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        return formatter.format(currentDate());
    }

    public static String getTheDayBeforeNowDateYYYYMMDD() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        return formatter.format(calendar.getTime());
    }

    public static String getTheDayBeforeAWeekYYYYMMDD() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate());
        calendar.add(Calendar.DAY_OF_MONTH, -7);

        return formatter.format(calendar.getTime());
    }

    public static String getNowDateFormatCustom(String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);

        return formatter.format(currentDate());
    }

    public static long getNowDaySecondTime0() {
        Calendar c = Calendar.getInstance();
        Date d = currentDate();
        c.setTime(d);
        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

        return c.getTime().getTime() / 1000;
    }

    public static int getDayOfMonth() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.DAY_OF_MONTH);
    }

    public static String toyyyyMMddHHmmss(long currentTime) {
        Date date = new Date(currentTime);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(date);
        return timeStr;
    }

    public static String toyyyyMMddHHmmss(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = sdf.format(date);
        return timeStr;
    }

    public static Date getUtilDate(String date, String formatter) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(formatter);
        return sdf.parse(date);
    }

    public static long getOldTime(int unitCount, int unit) {
        Calendar c = Calendar.getInstance();
        c.add(unit, -unitCount);
        return c.getTimeInMillis();
    }

    public static Date toDate(String timeString) throws Exception {
        if (StringUtils.isEmpty(timeString)) {
            return null;
        }
        return DATE_FORMAT_ENUM.formatDate(timeString);
    }

    public static Date toDate(DateTime dateTime) {
        if (null == dateTime) {
            return null;
        }
        return dateTime.toDate();
    }

    public static Date toDate(Timestamp timestamp) {
        if (null == timestamp) {
            return null;
        }
        return new Date(timestamp.getTime());
    }

    public static DateTime toDateTime(Date date) {
        if (null == date) {
            return null;
        }
        return new DateTime(date);
    }

    public static DateTime toDateTime(Long timeLongInMicros) {
        if (null == timeLongInMicros || timeLongInMicros.longValue() == 0L) {
            return null;
        }
        return toDateTime(new Date(timeLongInMicros));
    }

    public static DateTime toDateTime(String timeString) throws Exception {
        if (StringUtils.isEmpty(timeString)) {
            return null;
        }
        return toDateTime(toDate(timeString));
    }

    public static Timestamp toTimestamp(Date date) {
        if (null == date) {
            return null;
        }
        return new Timestamp(date.getTime());
    }

    public static Timestamp toTimestamp(Long timeLongInMicros) {
        if (null == timeLongInMicros || timeLongInMicros.longValue() == 0L) {
            return null;
        }
        return toTimestamp(new Date(timeLongInMicros));
    }

    public static Timestamp toTimestamp(String timeString) throws Exception {
        if (StringUtils.isEmpty(timeString)) {
            return null;
        }
        return toTimestamp(toDate(timeString));
    }

    public static DateTime toDateTime(Timestamp timestamp) throws Exception {
        if (null == timestamp) {
            return null;
        }
        return toDateTime(toDate(timestamp));
    }

    public static Timestamp toTimestamp(DateTime dateTime) throws Exception {
        if (null == dateTime) {
            return null;
        }
        return toTimestamp(toDate(dateTime));
    }


    public static Long toLong(Date date) {
        if (null == date) {
            return null;
        }
        return date.getTime() * 1000;
    }

    public static Long toLong(String timeString) throws Exception {
        if (null == timeString) {
            return null;
        }
        return toLong(toDate(timeString));
    }

    public static Long toLong(DateTime dateTime) throws Exception {
        if (null == dateTime) {
            return null;
        }
        return toLong(toDate(dateTime));
    }

    public static Long toLong(Timestamp timestamp) throws Exception {
        if (null == timestamp) {
            return null;
        }
        return toLong(toDate(timestamp));
    }

    public static java.sql.Date toSqlDate(Date date) {
        if (null == date) {
            return null;
        }
        return new java.sql.Date(date.getTime());
    }

    public static java.sql.Date toSqlDate(DateTime dateTime) {
        if (null == dateTime) {
            return null;
        }
        return new java.sql.Date(toDate(dateTime).getTime());
    }

    public static java.sql.Date toSqlDate(Long timeLongInMicros) {
        if (null == timeLongInMicros || timeLongInMicros.longValue() == 0L) {
            return null;
        }
        return toSqlDate(new Date(timeLongInMicros));
    }

    public static Date dateFormat(String date, String dateFormat) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        try {
            return format.parse(date);
        } catch (Exception ex) {

        }
        return null;
    }

    public static String dateFormat(Date date, String dateFormat) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        try {
            return format.format(date);
        } catch (Exception ex) {

        }
        return "";
    }

    public static Date add(Date date, int field, int amount) {

        if (date == null) {
            date = new Date();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(field, amount);

        return cal.getTime();
    }

    public static String tsFormatDetect(String ts, String format){
        if(StrUtil.isNotEmpty(format)){
            return format;
        }

        String tsFormat = "";
        String dateFormat = "";
        String timeFormat = "";
        String separator = "";
        int dateIdx = -1;
        int timeIdx = -1;
        String[] timeSegments;
        int amPmIdx = -1;
        boolean isUtc = false;
        boolean hasMsec = false;
        String[] tsSegments = ts.split(" ");// default "2015-05-22 09:29:48"

        if(tsSegments.length==0 && ts.equalsIgnoreCase("T") && ts.endsWith("Z")){
            // UTC format, example: "2019-09-12T04:30:25.091Z"
            isUtc = true;
            if(ts.equals(".")){
                hasMsec = true;
                tsSegments = ts.substring(0, ts.indexOf(".")).split("T");
            }
            tsSegments = ts.substring(0, ts.indexOf("Z")).split("T");
        }
        else if(tsSegments.length==0){
            //compact format like "20210703182530"
            if(ts.length()==14){
                tsFormat = "yyyyMMddHHmmss";
            }
            else if(ts.length()==12){
                tsFormat = "yyyyMMddHHmm";
            }
            else if(ts.length()==10){
                tsFormat = "yyyyMMddHH";
            }
            else if(ts.length()==8){
                tsFormat = "yyyyMMdd";
            }
        }

        for(int i=0; i<tsSegments.length; i++){
            if(dateIdx<0){
                separator = tsSegments[i].contains("_")?"_":separator;
                separator = tsSegments[i].contains("|")?"|":separator;
                separator = tsSegments[i].contains("/")?"/":separator;
                separator = tsSegments[i].contains("-")?"-":separator;
                if(!StrUtil.isEmpty(separator)){
                    dateIdx = i;
                    String[] dateSegments = tsSegments[i].split(separator);
                    for(int j=0; j<dateSegments.length; j++){
                        if(dateSegments[j].length()==4){
                            if(j==0){
                                dateFormat = "yyyy"+separator+"MM"+separator+"dd";
                                break;
                            }
                            else if(j==2){
                                dateFormat = "MM"+separator+"dd"+separator+"yyyy";
                                if(Integer.parseInt(dateSegments[0])>12){
                                    dateFormat = "dd"+separator+"MM"+separator+"yyyy";
                                }
                                break;
                            }
                        }
                    }
                }
            }

            if(timeIdx<0){
                timeSegments = tsSegments[i].split(":");
                if(timeSegments.length>1){
                    timeIdx = i;
                    if(timeSegments.length==2){
                        timeFormat = "HH:mm";
                    }
                    else if(timeSegments.length==3){
                        timeFormat = "HH:mm:ss";
                    }
                }
            }

            if(amPmIdx<0){
                if(tsSegments[i].equalsIgnoreCase("am") || tsSegments[i].equalsIgnoreCase("pm")){
                    amPmIdx = i;
                    timeFormat = timeFormat.replace("HH", "hh");
                }
            }
        }

        if(dateIdx==0){
            if(isUtc){
                tsFormat = dateFormat + "'T'" + timeFormat;
                if(hasMsec){
                    tsFormat += ".SSS'Z'";
                }
            }
            else if(timeIdx==1){
                tsFormat = dateFormat + " " + timeFormat;
                if(amPmIdx>0){
                    tsFormat += " a";
                }
            }
            else if(timeIdx<0){
                tsFormat = dateFormat;
            }
        }

        return tsFormat;
    }

}
