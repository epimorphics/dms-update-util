/******************************************************************
 * File:        S3Util.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 *  Utilities to access configured S3 bucket area
 */
public class S3Util {
    
    public static List<String > listObjects(String prefix, boolean recursive) {
        AmazonS3Client client = new AmazonS3Client();
        String fullPrefix = Config.getPrefix() + "/" + prefix;
        if (!recursive && !prefix.endsWith("/")) fullPrefix += "/";
        ListObjectsRequest request = new ListObjectsRequest()
            .withBucketName(Config.getBucketName())
            .withPrefix(fullPrefix);
        if (!recursive) {
            request.setDelimiter("/");
        }
        List<String> results = new ArrayList<>();
        ObjectListing listing = null;
        do {
            listing = (listing == null) ? client.listObjects(request) : client.listNextBatchOfObjects(listing);
            if (recursive) {
                for (S3ObjectSummary o : listing.getObjectSummaries()) {
                    results.add( o.getKey() );
                }
            } else {
                results.addAll( listing.getCommonPrefixes() );
            }
        } while (listing.isTruncated());
        return results;
    }
    
    public static List<UpdateEntry> listEntries(String prefix) {
        List<UpdateEntry> entries = new ArrayList<>();
        for (String result : listObjects(prefix, true)) {
            entries.add( new UpdateEntry(result) );
        }
        return entries;
    }
    
    public static List<UpdateEntry> listUpdatesSince(String effectiveDate) {
        List<UpdateEntry> entries = new ArrayList<>();
        for (UpdateEntry e : listEntries("updates")) {
            if (e.getEffectiveDate().compareTo(effectiveDate) > 0) {
                entries.add(e);
            }
        }
        return entries;
    }
    
    public static S3Object getObject(String name) {
        AmazonS3Client client = new AmazonS3Client();
        return client.getObject(Config.getBucketName(), name);
    }

//    public static void main(String[] args) {
//        System.out.println("List updates non-recursive");
//        for (String result : listObjects("updates", false)) {
//            System.out.println(" - " + result);
//        }
//        System.out.println("List all recursive");
//        for (String result : listObjects("", true)) {
//            System.out.println(" - " + result);
//        }
//    }
}
