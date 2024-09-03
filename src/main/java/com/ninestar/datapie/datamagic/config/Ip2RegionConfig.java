package com.ninestar.datapie.datamagic.config;

import org.apache.commons.io.IOUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import java.io.IOException;
import java.io.InputStream;

public class Ip2RegionConfig {
    public static Searcher searcher;

    static {
        InputStream resourceAsStream = Ip2RegionConfig.class.getClassLoader().getResourceAsStream("ip2region/ip2region.xdb");
        if (resourceAsStream != null) {
            byte [] cBuff= null;
            try {
                cBuff = IOUtils.toByteArray(resourceAsStream);
                searcher = Searcher.newWithBuffer(cBuff);
            }catch (IOException e) {
                e.printStackTrace();
            }finally {
                try {
                    resourceAsStream.close();
                }catch (Exception e) {}
            }
        }
    }
    public static Searcher getSearch(){
        return searcher;
    }
}
