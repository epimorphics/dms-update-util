/******************************************************************
 * File:        Main.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * <p>Utility for updating a fuseki-powered data server from a data state
 * held in S3. Configured by <code>/opt/dms-update/config.json</code>.</p>
 * 
 * <pre>usage:   dms-update  --plan|--perform</pre>
 * 
 * <p>With the <code>--plan</code> flag this generates a list of 
 * operations that are needed to bring data server up to date.</p> 
 *
 * <p>With the <code>--perform</code> flag this will generate a plan
 * and perform it. If there does not appear to be a current data store
 * then one will be created from the most recent image or dump using
 * the script install_image or install_dump in /opt/dms-update/bin. These
 * scripts should perform the image install or load from dump and
 * then start the fuseki server so that the remain operations 
 * can access it.</p>
 * 
 */
public class DMSUpdate {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger( DMSUpdate.class );

    public static void main(String[] args) throws IOException {
        
        String command = null;
        if (args.length == 2 && args[0].startsWith("--config=")) {
            String configFile = args[0].substring( "--config=".length() );
            Config.init(configFile);
            command = args[1];
        } else if (args.length == 1 && ! args[0].equals("--help")) {
            Config.init();
            command = args[0];
        } else {
            System.out.println("usage:   dms-update  [--config=config-file.json] --plan|--perform");
            System.exit(1);
        }
        
        // Ensure only one update process at a time
        File lockFile = new File(Config.getLockFile());
        FileOutputStream lockStream = new FileOutputStream( lockFile );
        FileLock lock = lockStream.getChannel().lock();
        
        try {
            Status status = Status.loadStatus();
            if (command.equals("--plan")) {
                createPlan(status).print( System.out );
            } else if (command.equals("--perform")) {
                if ( ! createPlan(status).execute() ) {
                    logger.error("Execution failed");
                    status.save();
                    System.exit(1);
                }
                status.save();
            } else {
                System.err.println("Action not recognized: " + command);
                System.exit(1);
            }
        } catch (Exception e) {
            logger.error("Exeception occurred", e);
            System.exit(1);
        } finally {
            lock.release();
            lockStream.close();
            lockFile.delete();
        }
    }
    
    public static Plan createPlan(Status status) {
        Plan plan = new Plan(status);
        if ( !dbExists()) {
            UpdateEntry lastMatch = null;
            UpdateEntry optFile = null;
            for (UpdateEntry e : S3Util.listEntries("images")) {
                if (e.getOp() == Operation.image || e.getOp() == Operation.dump) {
                    lastMatch = e;
                } else if (e.getObjectName().endsWith(".opt")) {
                    optFile = e;
                }
            }
            if (lastMatch == null) {
                throw new DUException("No image or dump to initialise from");
            }
            if (lastMatch.getOp() == Operation.dump && optFile != null) {
                lastMatch.setArg(optFile.getS3Name());
            }
            plan.add( lastMatch );
            status.setEffectiveDate( lastMatch.getEffectiveDate() );
        }
        plan.addAll( S3Util.listNewUpdates(status) );
        plan.prune();
        return plan;
    }
    
    protected static boolean dbExists() {
        File dbDir = new File(Config.getDBAddress());
        if (dbDir.exists() && dbDir.isDirectory() && dbDir.canRead()) {
            File[] contents = dbDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dat") || name.endsWith(".idn");
                }
            });
            return contents.length > 6;
        }
        return false;
    }

}
