/******************************************************************
 * File:        Status.java
 * Created by:  Dave Reynolds
 * Created on:  8 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the saved status of the processing.
 * This is a json structure including the effective date we are up to
 * and a list of the dates processed today (used to avoid race conditions when
 * files are published close to each other which an update is happening).
 */
public class Status {
    protected static final String LAST_EFFECTIVE_DATE_FIELD = "lastEffectiveDate";
    protected static final String PROCESSED_TODAY_FIELD = "processedToday";
    
    protected JSONObject status;
    
    public static Status getStatus() throws JSONException {
        String statusSer = readFile(Config.STATUS_FILE);
        if (statusSer != null) {
            return new Status( new JSONObject(statusSer) );
        }
        String lastEffectiveDate = readFile(Config.EFFECTIVE_DATE_FILE);
        if (lastEffectiveDate == null) {
            lastEffectiveDate = Config.DEFAULT_DATE;
        }
        JSONObject status = new JSONObject();
        status.put(LAST_EFFECTIVE_DATE_FIELD, lastEffectiveDate);
        status.put(PROCESSED_TODAY_FIELD, Collections.emptyList());
        return new Status( status );
    }
    
    public static String readFile(String filename) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(filename));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
    
    public Status(JSONObject status) {
        this.status = status;
    }
    
    // TODO writer
    // TODO record processed today
    // TODO filter plan by processed today
}
