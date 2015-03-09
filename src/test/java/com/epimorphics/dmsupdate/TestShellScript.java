/******************************************************************
 * File:        TestShellScript.java
 * Created by:  Dave Reynolds
 * Created on:  9 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestShellScript {

    @Test
    public void testShellScript() throws IOException {
        ShellScript script = new ShellScript("src/test/bin/test.sh");
        assertEquals(1, script.run("24"));
        assertEquals(0, script.run("42"));
    }
}
