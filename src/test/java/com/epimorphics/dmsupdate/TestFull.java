/******************************************************************
 * File:        TestFull.java
 * Created by:  Dave Reynolds
 * Created on:  15 Dec 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Tests some full plan/execute sequences by creating and updating an S3 test area.
 * These tests are normally disabled.
 *
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class TestFull {
    protected static final String DB_DIR = "/tmp/DS-DB";
    protected static final String S3BASE = "s3://epi-ops/test/dms-update-test";
    protected static final String CONFIG = "src/test/conf/config.json";
    protected static final String QUERY_ENDPOINT = "http://localhost:3030/ds/query";
    protected static final String QUERY = "SELECT ?label WHERE {GRAPH ?G {?res <http://www.w3.org/2000/01/rdf-schema#label> ?label}} ORDER BY ?label";
    
    Status status;
    
    // To run:
    //   - remove @Ignore
    //   - ensure /opt/dms-update/bin is linked to ./bin (or /opt/dms-update linked to .)
    //   - ensure AWS credential set up to access aws-expt
    //   - run a test fuseki server: fuseki-server --update --mem --port=3030 /ds
    @Ignore
    @Test public void testFull() throws IOException {
        // Set up empty initial state and initial upload batch
        removeOldDB();
        clearS3();
        initS3();
        Config.init(CONFIG);
        status = Status.loadStatus();
        
        // First publication catchup
        Plan plan = plan();
        checkPlan(plan, 
                Operation.image, "", 
                Operation.replace, "http://localhost/graph/r2",
                Operation.drop, "http://localhost/graph/r3",
                Operation.add, "http://localhost/graph/r4");
        assertTrue( plan.execute() ); 
        status.save();
        checkGraphs("label", "r1", "r2b", "r4");
        
        // Check a re-run doesn't include any more actions
        plan = plan();
        checkPlan(plan);
        
        // Add some more operations
        uploadS3(10, Operation.replace, "r3", ".ttl", "r3");
        uploadS3(11, Operation.replace, "r3b", ".ttl", "r3");
        uploadS3(12, Operation.drop, "empty", "", "r2");
        
        plan = plan();
        checkPlan(plan, Operation.replace, "http://localhost/graph/r3", Operation.drop, "http://localhost/graph/r2");
        assertTrue( plan.execute() ); 
        status.save();
        checkGraphs("label", "r1", "r3b", "r4");
        
        // Check a re-run doesn't include any more actions
        plan = plan();
        checkPlan(plan);

        // Try out an update
        uploadS3(20, Operation.update, "r4b", ".ru", "r4");
        
        plan = plan();
        assertTrue( plan.execute() ); 
        status.save();
        checkGraphs("label", "r1", "r3b", "r4b");

        // Check previous but where a drop with a "." in graph name caused problems
        uploadS3(13, Operation.drop, "empty", "", "r5.lookslikeanextension");
        plan = plan();
        checkPlan(plan, Operation.drop, "http://localhost/graph/r5.lookslikeanextension");
        assertTrue( plan.execute() ); 
        status.save();
        checkGraphs("label", "r1", "r3b", "r4b");

        clearS3();
        new ShellScript("bin/stop_fuseki").run();
    }
    
    protected Plan plan() {
        return DMSUpdate.createPlan(status);
    }
    
    protected void checkPlan(Plan plan, Object...expected) {
        List<UpdateEntry> updates = plan.listUpdates();
        assertEquals(expected.length/2, updates.size());
        for (int i = 0; i < updates.size(); i++){
            UpdateEntry update = updates.get(i);
            assertEquals(update.getOp(), expected[i*2]);
            assertEquals(update.getArg(), expected[i*2+1]);
        }
    }
    
    protected void checkGraphs(String...expected) {
        WebResource resource = Client.create().resource(QUERY_ENDPOINT);
        String result;
        try {
            result = resource.queryParam("query", QUERY).accept("text/csv").get(String.class);    
            String[] labels = result.split("\r\n");
            assertArrayEquals(expected, labels);
        } catch (Exception e) {
            throw new DUException("Query failed", e);
        }
    }
    
    protected void removeOldDB() {
        File dbDir = new File(DB_DIR);
        if (dbDir.exists()) {
            for (File f : dbDir.listFiles()) {
                f.delete();
            }
            dbDir.delete();
        }
    }
    
    protected void clearS3() throws IOException {
        if (new ShellScript("src/test/bin/s3clean.sh").run(S3BASE) != 0) {
            throw new DUException("Failed to clean S3 area");
        }
    }
    
    protected void initS3() throws IOException {
        if (new ShellScript("src/test/bin/s3init.sh").run(S3BASE) != 0) {
            throw new DUException("Failed to initialize S3 area");
        }
    }
    
    protected void uploadS3(int timecount, Operation op, String base, String ext, String graph) throws IOException {
        ShellScript s = new ShellScript("src/test/bin/s3upload.sh");
        if ( s.run(S3BASE, Integer.toString(timecount), op.name(), base, ext, graph) != 0) {
            throw new DUException("Failed to upload " + base);
        }
    }
}
