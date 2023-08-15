package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import static com.ninestar.datapie.datamagic.config.Ip2RegionConfig.searcher;

/**
 * 获取IP方法
 * 
 * @author ruoyi
 */
public class IpUtils
{
    private static final Logger log = LoggerFactory.getLogger(com.ninestar.datapie.datamagic.utils.IpUtils.class);

    // IP地址查询
    private static final String IP2LOC_URL1 = "http://ip-api.com/json/ADDRESS?fields=24598";
    private static final String IP2LOC_URL2 = "http://www.geoplugin.net/json.gp?ip=";

    /**
     * 获取客户端IP
     * 
     * @param request 请求对象
     * @return IP地址
     */
    public static String getIpAddr(HttpServletRequest request)
    {
        if (request == null)
        {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
        {
            ip = request.getRemoteAddr();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : getMultistageReverseProxyIp(ip);
    }

    /**
     * 检查是否为内部IP地址
     * 
     * @param ip IP地址
     * @return 结果
     */
    public static boolean internalIp(String ip)
    {
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr) || "127.0.0.1".equals(ip);
    }

    /**
     * 检查是否为内部IP地址
     * 
     * @param addr byte地址
     * @return 结果
     */
    private static boolean internalIp(byte[] addr)
    {
        if (addr == null || addr.length < 2)
        {
            return true;
        }
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        // 172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0)
        {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4)
                {
                    return true;
                }
            case SECTION_5:
                switch (b1)
                {
                    case SECTION_6:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * 将IPv4地址转换成字节
     * 
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text)
    {
        if (text.length() == 0)
        {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try
        {
            long l;
            int i;
            switch (elements.length)
            {
                case 1:
                    l = Long.parseLong(elements[0]);
                    if ((l < 0L) || (l > 4294967295L))
                    {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                    bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 2:
                    l = Integer.parseInt(elements[0]);
                    if ((l < 0L) || (l > 255L))
                    {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l & 0xFF);
                    l = Integer.parseInt(elements[1]);
                    if ((l < 0L) || (l > 16777215L))
                    {
                        return null;
                    }
                    bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 3:
                    for (i = 0; i < 2; ++i)
                    {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L))
                        {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    l = Integer.parseInt(elements[2]);
                    if ((l < 0L) || (l > 65535L))
                    {
                        return null;
                    }
                    bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 4:
                    for (i = 0; i < 4; ++i)
                    {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L))
                        {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        return bytes;
    }

    /**
     * ip转long
     * @param ip
     * @return
     */
    public static long ipToLong(String ip){
        if(StrUtil.isEmpty(ip)){
            return 0L;
        }
        String[] split = ip.split("\\.");
        long i1 = 0L;
        i1 += Integer.parseInt(split[0])<<24;
        i1 += Integer.parseInt(split[1])<<16;
        i1 += Integer.parseInt(split[2])<<8;
        return i1 + Integer.parseInt(split[3]);
    }


    /**
     * 获取IP地址
     * 
     * @return 本地IP地址
     */
    public static String getHostIp()
    {
        try
        {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
        }
        return "127.0.0.1";
    }

    /**
     * 获取主机名
     * 
     * @return 本地主机名
     */
    public static String getHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
        }
        return "未知";
    }

    /**
     * 从多级反向代理中获得第一个非unknown IP地址
     *
     * @param ip 获得的IP地址
     * @return 第一个非unknown IP地址
     */
    public static String getMultistageReverseProxyIp(String ip)
    {
        // 多级反向代理检测
        if (ip != null && ip.indexOf(",") > 0)
        {
            final String[] ips = ip.trim().split(",");
            for (String subIp : ips)
            {
                if (false == isUnknown(subIp))
                {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
    }

    /**
     * 检测给定字符串是否为未知，多用于检测HTTP请求相关
     *
     * @param checkString 被检测的字符串
     * @return 是否未知
     */
    public static boolean isUnknown(String checkString)
    {
        return StrUtil.isBlank(checkString) || "unknown".equalsIgnoreCase(checkString);
    }


    public static String getLocByIP(String ip)
    {
        // 内网不查询
        if (IpUtils.internalIp(ip))
        {
            return "Intranet";
        }

        if (true) // config is true
        {
            try
            {
                // get location by ip address from local db file
                if(true){
                    String testIp = "5.125.1.97";
                    long ipLong = ipToLong(ip);
                    // all Country|Region|Province|City are Chinese name. not good!!!
                    // only Chinese City can be located.
                    String search = searcher.search(ipLong); //Country|Region|Province|City|ISP
                    if(StrUtil.isEmpty(search) || search.startsWith("0")){
                        return null;
                    } else {
                        String[] loc = search.split("\\|");
                        if(loc[3].equals("0")){
                            return loc[0]; // only Country
                        } else {
                            return loc[3]+"|"+loc[2]+"|"+loc[0]; // City|Province|Country
                        }
                    }
                }

                if(false){
                    // get location by ip address online
                    HttpResponse response = HttpRequest.get(IP2LOC_URL1.replace("ADDRESS", ip)).execute();
                    if(response!=null){
                        // forward response of python server to front end
                        JSONObject result = new JSONObject(response.body());
                        if(result.getStr("status").equals("success")){
                            return result.getStr("city") + "|" + result.getStr("region") + "|" + result.getStr("countryCode");
                        }
                        else{
                            return null;
                        }
                    }
                    else{
                        return null;
                    }
                }

                if(false){
                    // get location by ip address online
                    HttpResponse response = HttpRequest.get(IP2LOC_URL2 + ip).execute();
                    if(response!=null){
                        // forward response of python server to front end
                        JSONObject result = new JSONObject(response.body());
                        if(result.getInt("geoplugin_status") == 200){
                            return result.getStr("geoplugin_city") + "|" + result.getStr("geoplugin_regionCode") + "|" + result.getStr("geoplugin_countryCode");
                        }
                        else{
                            return null;
                        }
                    }
                    else{
                        return null;
                    }
                }
            }
            catch (Exception e)
            {
                log.error("Exception when get location from IP {}", ip);
            }
        }
        return null;
    }
}