/******************************************************************
 * File:        DUException.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

public class DUException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public DUException(String message) {
        super(message);
    }
    
    public DUException(String message, Throwable parent) {
        super(message, parent);
    }

}
