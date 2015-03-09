/******************************************************************
 * File:        TestStatus.java
 * Created by:  Dave Reynolds
 * Created on:  9 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestStatus {

    @Test
    public void testStatus() {
        String d1 = "2015-01-01-00-00-00-0000";  // before today
        String d2 = "2015-01-02-00-00-00-0000";  // before today
        String today = new SimpleDateFormat("YYYY-MM-dd").format(new Date());
        String d3 = today + "-09-30-00-0000";
        String d4 = today + "-10-30-00-0000";
        
        Status status = new Status(d1);
        status.recordUpdate(d2);
        status.recordUpdate(d3);
        status.recordUpdate(d4);
        status.deleteOldRecords();
        
        assertEquals(d4, status.effectiveDate);
        assertEquals(2, status.recent.size());
        assertTrue(status.recent.contains(d3));
        assertTrue(status.recent.contains(d4));
    }
}
