/******************************************************************
 * File:        S3Util.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *  Utilities to access configured S3 bucket area
 */
public class S3Util {

    public static List<String> listObjects(String prefix, boolean recursive) {
        S3Client s3Client = S3Client.builder()
                .build();
        String fullPrefix = Config.getPrefix() + "/" + prefix;
        if (!recursive && !prefix.endsWith("/")) fullPrefix += "/";
        ListObjectsV2Request.Builder request = ListObjectsV2Request.builder()
                .bucket(Config.getBucketName())
                .prefix(fullPrefix);

        if (recursive) {
            ListObjectsV2Iterable listRes = s3Client.listObjectsV2Paginator(request.build());
            return listRes.stream()
                    .flatMap(r -> r.contents().stream())
                    .map(content -> content.key())
                    .collect(Collectors.toList());
        } else {
            request.delimiter("/");
            ListObjectsV2Iterable listRes = s3Client.listObjectsV2Paginator(request.build());
            return listRes.stream()
                    .flatMap(r -> r.commonPrefixes().stream())
                    .map(p -> p.prefix())
                    .collect(Collectors.toList());
        }
    }

    public static List<UpdateEntry> listEntries(String prefix) {
        List<UpdateEntry> entries = new ArrayList<>();
        for (String result : listObjects(prefix, true)) {
            if (!result.endsWith("stats.opt")) {
                // Ignore the (optional) stats.opt which is used for image rebuilding
                entries.add(new UpdateEntry(result));
            }
        }
        return entries;
    }

    public static List<UpdateEntry> listNewUpdates(Status status) {
        Matcher m = DAY_ED_PATTERN.matcher(status.getEffectiveDate());
        if (!m.matches()) {
            throw new DUException("Effective date malformed, couldn't determine day: " + status.getEffectiveDate());
        }
        String day = m.group(1);
        List<String> candidates = new ArrayList<String>();
        for (String object : listObjects("updates", false)) {
            m = DAY_DIR_PATTERN.matcher(object);
            if (m.matches()) {
                String cday = m.group(1);
                if (cday.compareTo(day) >= 0) {
                    candidates.add("updates/" + cday);
                }
            }
        }
        String today = status.getToday();
        if (!candidates.contains(today)) {
            candidates.add(today);
        }
        List<UpdateEntry> entries = new ArrayList<>();
        for (String candidate : candidates) {
            for (UpdateEntry e : listEntries(candidate)) {
                if (status.shouldProcess(e.getEffectiveDate())) {
                    entries.add(e);
                }
            }
        }
        return entries;
    }

    static final Pattern DAY_DIR_PATTERN = Pattern.compile(".*/([0-9]{4}-[0-9]{2}-[0-9]{2})/");
    static final Pattern DAY_ED_PATTERN = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})-.*");

    public static InputStream getObject(String name) {
        S3Client client = S3Client.builder().build();
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(Config.getBucketName())
                .key(name)
                .build();
        return client.getObject(getRequest, ResponseTransformer.toInputStream());
    }
}
