/******************************************************************
 * File:        Config.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Configuration information loaded from /opt/dms-update/config.json
 */
public class Config {
    public static final String CONFIG_AREA = "/opt/dms-update";
    public static final String CONFIG_FILE = CONFIG_AREA + "/config.json";
    public static final String BIN_DIR = CONFIG_AREA + "/bin/";
    public static final String LOG_AREA = "/var/opt/dms-update";
    public static final String EFFECTIVE_DATE_FILE = LOG_AREA + "/lastEffectiveDate";
    public static final String STATUS_FILE = LOG_AREA + "/status.json";
    public static final String LOCK_FILE = LOG_AREA + "/lock";
    
    public static final String S3ROOT = "s3root";
    public static final String DBLOCATION = "dblocation";
    public static final String DBNAME = "dbname";
    public static final String SERVICE = "service";
    
    public static final String DEFAULT_DATE = "1970-01-01-00-00-00-0000";
    
    protected static JsonNode config;
    protected static String bucketName;
    protected static String prefix;
    
    protected static Pattern S3PAT = Pattern.compile("s3://([^/]+)/(.*)"); 
    static {
        ObjectMapper mapper = new ObjectMapper();
        try {
            config = mapper.readTree(new File(CONFIG_FILE));
        } catch (Exception e) {
            throw new DUException("Failed to load configuration file");
        }
        String root = get(S3ROOT);
        Matcher m = S3PAT.matcher(root);
        if (m.matches()) {
            bucketName = m.group(1);
            prefix = m.group(2);
        } else {
            throw new DUException("Specifed s3 root is malformed: " + root);
        }
    }
    
    public static String get(String key) {
        return config.get(key).asText();
    }
    
    public static String getBucketName() {
        return bucketName;
    }
    
    public static String getPrefix() {
        return prefix;
    }
    
    public static String getDBLocation() {
        return get(DBLOCATION);
    }
    
    public static String getDBName() {
        return get(DBNAME);
    }
    
    public static String getDBAddress() {
        return get(DBLOCATION) + "/" + get(DBNAME);
    }
    
    public static String getService() {
        return get(SERVICE);
    }
    
}
