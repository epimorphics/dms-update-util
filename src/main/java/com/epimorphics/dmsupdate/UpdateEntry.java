/******************************************************************
 * File:        UpdateEntry.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Represents a single entry in the S3 data state collection.
 */
public class UpdateEntry {
    private static final String APPLICATION_SPARQL_UPDATE = "application/sparql-update";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger( UpdateEntry.class );
    
    private static final int N_RETRIES = 3;
    private static final long WAIT_PERIOD = 3 * 1000;
    
    protected String objectName;
    protected Operation op;
    protected String arg;
    protected String label;
    protected String effectiveDate;
    protected String format;
    protected boolean gzipped = false;
    
    public UpdateEntry(String objectName) {
        this.objectName = objectName;
        
        Matcher matcher = OBJECT_FORMAT.matcher(objectName);
        if (matcher.matches()) {
            effectiveDate = matcher.group(1) + "-" + matcher.group(2);
            label = matcher.group(3);
            op = Operation.valueOf( matcher.group(4) );
            arg = Util.decodePercent( matcher.group(5) );
            if (arg != null && arg.contains(".") && op != Operation.drop) {
                if (arg.endsWith(".gz")) {
                    gzipped = true;
                    arg = arg.substring(0, arg.length() - 3);
                }
                format = Util.extension(arg);
                arg = Util.removeExtension(arg);
            } else {
                format = "";
            }
        } else {
            logger.warn("Unexpected object found, skipping: " + objectName);
        }
    }
    
    protected static final Pattern OBJECT_FORMAT = Pattern.compile(".*/([0-9-]+)/([0-9-]+)/([^_]*)_([^_\\.]*)_?(.*)$");
    
    public String getObjectName() {
        return objectName;
    }

    public Operation getOp() {
        return op;
    }

    public String getArg() {
        return arg;
    }
    
    public void setArg(String arg) {
        this.arg = arg;
    }

    public String getLabel() {
        return label;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public String getFormat() {
        return format;
    }
    
    public String getLocalName() {
        int split = objectName.lastIndexOf("/");
        return objectName.substring(split+1);
    }
    
    public String asCommand() {
        String command = String.format("op_%s s3://%s/%s %s", 
                op ==  Operation.postproc ? "update" : op.name(), 
                Config.getBucketName(), objectName,
                effectiveDate);
        if (op != Operation.postproc && arg != null && !arg.isEmpty()) {
            command += " " + arg;
        }
        return command;
    }
    
    public String getS3Name() {
        return "s3://" + Config.getBucketName() + "/" + objectName;
    }
    
    
    /**
     * Run the update action.
     * If a database install or build-from-dump is required then
     * uses external shell script "install_image" or "install_dump". 
     * Assumes that these leave the fuseki server running so that it
     * can receive further updates.
     * @return the effectiveDate of this operation
     * @throws IOException 
     */
    public String execute() throws IOException {
        logger.info("Execute: " + asCommand());
        switch(op) {
        case image:
        case dump:
            install();
            break;
            
        case replace:
            sendData(false, false);
            break;
            
        case update:
        case postproc:
            sendData(true, true);
            break;
            
        case add:
            sendData(false, true);
            break;
        
        case drop:
            ClientResponse response= Client.create()
                .resource(Config.getService() + "data")
                .queryParam("graph", arg)
                .type(APPLICATION_SPARQL_UPDATE)
                .delete(ClientResponse.class);
            if (response.getStatus() >= 500) {
                throw new DUException("Update failed with response " + response);
            }
            break;
            
        default:
            throw new DUException("Unimplemented operation: " + op);
        }
        return effectiveDate;
    }
    
    protected void sendData(boolean isUpdate, boolean isPost) throws IOException {
        InputStream content = S3Util.getObject(objectName);
        if (gzipped) {
            content = new GZIPInputStream(content);
        }
        try {
            String mediaType = isUpdate ? APPLICATION_SPARQL_UPDATE : mediaTypeFor(format);
            WebResource resource = Client.create()
                    .resource(Config.getService() + (isUpdate ? "update" : "data"));
            if (!isUpdate) {
                resource = resource.queryParam("graph", arg);
            }
            WebResource.Builder builder = resource.type( mediaType ).entity( content ); 
            ClientResponse response = isPost ? builder.post(ClientResponse.class) : builder.put(ClientResponse.class);
            if (response.getStatus() >= 400) {
                throw new DUException("Update failed with response " + response);
            }
        } finally {
            content.close();
        }
    }

    protected void install() throws IOException {
        String script = "install_" + op.name();
        ShellScript ss = new ShellScript(Config.BIN_DIR + script, Config.getDBLocation());
        int status = -1;
        if (arg != null && ! arg.isEmpty()) {
            status = ss.run( getS3Name(), Config.getDBName(), arg );
        } else {
            status = ss.run( getS3Name(), Config.getDBName());
        }
        if (status != 0) {
            throw new DUException(script + " failed");
        }
        checkServiceUp();
    }
    
    protected void checkServiceUp() {
        for (int i = 0; i < N_RETRIES; i++) {
            try {
                ClientResponse response = Client
                    .create()
                    .resource(Config.getService() + "query")
                    .queryParam("query", "ASK {}")
                    .get(ClientResponse.class);
                if (response.getStatus() >= 400) {
                    throw new DUException("Bad response from fuseki service " + response.getStatus());
                }
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(WAIT_PERIOD);
                } catch (InterruptedException e1) {
                    throw new DUException("Interrupted while waiting for service to become live");
                }
            }
        }
        throw new DUException("Failed to contact fuseki service");
    }
    
    protected String mediaTypeFor(String fmt) {
        if ("ttl".equals(fmt)) {
            return "text/turtle";
        } else if ("rdf".equals(fmt)) {
            return "application/rdf+xml";
        } else if ("nt".equals(fmt)) {
            return "application/n-triples";
        } else if ("ru".equals(fmt)) {
            return APPLICATION_SPARQL_UPDATE;
        } else {
            throw new DUException("Could not find media type for update format: " + fmt);
        }
    }
}
