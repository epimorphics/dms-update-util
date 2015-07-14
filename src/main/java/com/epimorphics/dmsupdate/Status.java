/******************************************************************
 * File:        Status.java
 * Created by:  Dave Reynolds
 * Created on:  8 Mar 2015
 * Created on:  9 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

/**
 * Encapsulates the processing state, saved to disk between runs.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class Status {
    protected static final String EFFECTIVE_DATE_FIELD = "effectiveDate";
    protected static final String RECENT_FIELD = "recent";
    
    protected String effectiveDate;
    protected Set<String> recent = new HashSet<String>();
    
    protected String today = new SimpleDateFormat("YYYY-MM-dd").format(new Date());
    
    public Status(JSONObject status) {
        try {
            effectiveDate = status.getString(EFFECTIVE_DATE_FIELD);
            JSONArray recentA = status.getJSONArray(RECENT_FIELD);
            for (int i = 0; i < recentA.length(); i++) {
                recent.add( recentA.getString(i) );
            }
        } catch (JSONException e) {
            throw new DUException("Status data structure broken", e);
        }
    }
    
    public Status(String effectiveDate) {
        this.effectiveDate = effectiveDate.trim();
    }
    
    public String getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(String date) {
        this.effectiveDate = date;
    }
    
    public String getToday() {
        return today;
    }
    
    public boolean shouldProcess(String date) {
        if (later(date, today)) {
            return !recent.contains(date);
        }
        return later(date, effectiveDate);
    }
    
    /**
     * Save the current status, retaining only update records from today
     */
    public void save() {
        deleteOldRecords();
        save( Config.STATUS_FILE );
    }
    
    /**
     * Remove update records that predate today
     */
    public void deleteOldRecords() {
        for (Iterator<String> i = recent.iterator(); i.hasNext();) {
            String date = i.next();
            if ( !later(date, today) ) {
                i.remove();
            }
        }
    }
    
    /**
     * Save the current status, 
     */
    public void save(String filename) {
        try {
            FileWriter writer = new FileWriter( filename );
            JSONObject status = new JSONObject();
            status.put(EFFECTIVE_DATE_FIELD, effectiveDate);
            status.put(RECENT_FIELD, recent);
            status.write(writer);
            writer.close();
        } catch (Exception e) {
            throw new DUException("Failed to save status file " + filename, e);
        }
    }
    
    public void recordUpdate(String date) {
        if ( later(date, effectiveDate) ) {
            effectiveDate = date;
        }
        if ( later(date, today) ) {
            recent.add(date);
        }
    }
    
    public static boolean later(String date1, String date2) {
        return date1.compareTo(date2) >= 0;  // Actually later than or same
    }
    
    public static Status loadStatus() {
        try {
            String statusString = readFile( Config.STATUS_FILE );
            if (statusString == null) {
                statusString = readFile( Config.EFFECTIVE_DATE_FILE );
                if (statusString == null) {
                    statusString = Config.DEFAULT_DATE;
                }
                return new Status(statusString);
            } else {
                return new Status( new JSONObject(statusString) );
            }
        } catch (JSONException e) {
            throw new DUException("Status data structure broken", e);
        }
    }
    
    private static String readFile(String filename) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(filename));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }        
    }

}
