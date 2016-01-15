/******************************************************************
 * File:        TestUpdateEntry.java
 * Created by:  Dave Reynolds
 * Created on:  15 Jan 2016
 * 
 * (c) Copyright 2016, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestUpdateEntry {

    @Test
    public void testParse() {
        UpdateEntry entry = new UpdateEntry("images/flood-monitoring/updates/2016-01-15/04-00-01-262711077/cleanup_drop_http:%2F%2Fenvironment%2Edata%2Egov%2Euk%2Fflood-monitoring%2Fgraph%2Freadings-2015-11-20");
        assertEquals("2016-01-15-04-00-01-262711077", entry.effectiveDate );
        assertEquals(Operation.drop, entry.op);
        assertEquals("cleanup", entry.label);
        assertEquals("http://environment.data.gov.uk/flood-monitoring/graph/readings-2015-11-20", entry.arg);
        assertEquals("", entry.format);
        
        entry = new UpdateEntry("images/flood-monitoring/updates/2016-01-15/02-03-07-185994019/readings_update.ru.gz");
        assertEquals("2016-01-15-02-03-07-185994019", entry.effectiveDate);
        assertEquals(Operation.update, entry.op);
        assertEquals("readings", entry.label);
        assertEquals("", entry.arg);
        assertEquals("ru", entry.format);
        
        entry = new UpdateEntry("images/flood-monitoring/updates/2016-01-15/03-18-10-364044013/3df_replace_http:%2F%2Fenvironment%2Edata%2Egov%2Euk%2Fflood-monitoring%2Fgraph%2F3df.ttl");
        assertEquals("2016-01-15-03-18-10-364044013", entry.effectiveDate);
        assertEquals(Operation.replace, entry.op);
        assertEquals("3df", entry.label);
        assertEquals("http://environment.data.gov.uk/flood-monitoring/graph/3df", entry.arg);
        assertEquals("ttl", entry.format);
    }
}
