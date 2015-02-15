/******************************************************************
 * File:        Operation.java
 * Created by:  Dave Reynolds
 * Created on:  14 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

/**
 * Operations supported by the update facility
 */
public enum Operation {
    image,
    dump,
    add,
    replace,
    drop,
    update,
    postproc; 
}
