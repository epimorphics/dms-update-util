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
 * Configuration information loaded from /opt/dms-update/config.json or some other location
 */
// This was just a set of statics, now it is mutated into a dynamic configuration point
// its rather ugly using statics rather than a singleton but whatever.
public class Config {
    public static final String CONFIG_AREA = "/opt/dms-update";
    public static final String DEFAULT_CONFIG_FILE = CONFIG_AREA + "/config.json";
    public static final String BIN_DIR = CONFIG_AREA + "/bin/";
    public static final String DEFAULT_LOG_AREA = "/var/opt/dms-update";
    private static final String EFFECTIVE_DATE_FILE = "lastEffectiveDate";
    private static final String STATUS_FILE = "status.json";
    private static final String LOCK_FILE = "/lock";
    
    public static final String S3ROOT = "s3root";
    public static final String DBLOCATION = "dblocation";
    public static final String DBNAME = "dbname";
    public static final String SERVICE = "service";
    public static final String LOG_AREA_PARAM = "logArea";
    
    public static final String DEFAULT_DATE = "1970-01-01-00-00-00-0000";
    
    protected static JsonNode config;
    protected static String bucketName;
    protected static String prefix;
    protected static String logArea = DEFAULT_LOG_AREA;
    
    protected static Pattern S3PAT = Pattern.compile("s3://([^/]+)/(.*)"); 
    
    public static void init(String configFile) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            config = mapper.readTree(new File(configFile));
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
        if (config.has(LOG_AREA_PARAM) && config.get(LOG_AREA_PARAM) != null) {
            logArea = config.get(LOG_AREA_PARAM).asText();
        }
    }
    
    public static void init() {
        init(DEFAULT_CONFIG_FILE);
    }
    
    public static String get(String key) {
        if (config == null) init();
        return config.get(key).asText();
    }
    
    public static String getEffectiveDateFile() {
        return logArea + "/" + EFFECTIVE_DATE_FILE;
    }
    
    public static String getLockFile() {
        return logArea + "/" + LOCK_FILE;
    }
    
    public static String getStatusFile() {
        return logArea + "/" + STATUS_FILE;
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
