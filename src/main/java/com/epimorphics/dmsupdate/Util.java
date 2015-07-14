/******************************************************************
 * File:        Util.java
 * Created by:  Dave Reynolds
 * Created on:  15 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.nio.charset.StandardCharsets;

public class Util {

    /**
     * Decode an encoded safe name
     */
    public static String decodePercent(String name) {
        byte[] decode = new byte[ name.length() ];
        int p = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '%') {
                decode[p++] = (byte) Integer.parseInt(name.substring(i+1, i+3), 16);
                i += 2;
            } else {
                decode[p++] = (byte) c;
            }
        }
        byte[] after = new byte[p];
        System.arraycopy(decode, 0, after, 0, p);
        return new String(after, StandardCharsets.UTF_8);
    }
    

    /**
     * Remove a trailing .xxx extension from a file name
     */
    public static String removeExtension(String f) {
        return splitBeforeLast(f, ".");
    }

    /**
     * Find the .xxx extension of a file name
     */
    public static String extension(String f) {
        return splitAfterLast(f, ".");
    }

    /**
     * Return segment of a string after the last occurrence of "at"
     */
    public static String splitAfterLast(String filename, String at) {
        int split = filename.lastIndexOf(at);
        if (split == -1) {
            return filename;
        } else {
            return filename.substring(split + 1);
        }
    }


    /**
     * Return segment of a string before the last occurrence of "at"
     */
    public static String splitBeforeLast(String filen, String at) {
        int split = filen.lastIndexOf(at);
        if (split == -1) {
            return filen;
        } else {
            return filen.substring(0, split);
        }

    }
    
}
