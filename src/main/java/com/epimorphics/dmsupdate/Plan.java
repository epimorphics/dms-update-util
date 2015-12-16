/******************************************************************
 * File:        Plan.java
 * Created by:  Dave Reynolds
 * Created on:  15 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Represents an update plan which is a series of update actions
 * determined from the current state and the S3 contents
 */
public class Plan {
    private static final Logger logger = LogManager.getLogger( Plan.class );
    
    protected List<UpdateEntry> plan;
    protected Set<UpdateEntry> drop = new HashSet<>();
    protected Status status;
    
    public Plan(Status status) {
        plan = new ArrayList<>();
        this.status = status;
    }
    
    public void add(UpdateEntry e) {
        plan.add(e);
    }
    
    public void addAll(Collection<UpdateEntry> collection) {
        plan.addAll(collection);
    }
    
    /**
     * Remove redundant replace or postprocess commands from the plan
     */
    public void prune() {
        Map<String, UpdateEntry> seen = new HashMap<String, UpdateEntry>();
        
        for (UpdateEntry e : plan) {
            Operation op = e.getOp();
            if (op == Operation.replace || op == Operation.postproc || op == Operation.drop) {
                String key = e.getArg();
                if ( seen.containsKey(key) ) {
                    drop.add( seen.get(key) );
                }
                seen.put(key, e);
            }
        }
    }
    
    /**
     * Write a description of the plan
     */
    public void print(PrintStream out) {
        for (UpdateEntry e : listUpdates()) {
            out.println( e.asCommand() );
        }
    }
    
    /**
     * Access the ordered list of non-dropped updates for test purposes
     */
    public List<UpdateEntry> listUpdates() {
        List<UpdateEntry> p = new ArrayList<>( plan.size() );
        for (UpdateEntry e : plan) {
            if (!drop.contains(e)) {
                p.add(e);
            }
        }
        return p;
    }
    
    /**
     * Run the plan.
     * If a database install or build-from-dump is required then
     * uses external shell script "install_image" or "install_dump". 
     * Assumes that these leave the fuseki server running so that it
     * can receive further updates.
     * @return true if succeeded
     * @throws IOException 
     */
    public boolean execute() {
        for (UpdateEntry update : plan) {
            try {
                if (!drop.contains(update)) {
                    update.execute();
                }
                status.recordUpdate( update.getEffectiveDate() );
            } catch (Exception e) {
                logger.error("Execution failed with exception", e);
                return false;
            }
        }
        return true;
    }
}