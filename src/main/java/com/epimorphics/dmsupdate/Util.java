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
}
