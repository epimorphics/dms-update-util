/******************************************************************
 * File:        ShellScript.java
 * Created by:  Dave Reynolds
 * Created on:  15 Feb 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsupdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to run a shell script. 
 * The stderr and stdout of the script are merged and copied to stdout of the parent process.
 */
public class ShellScript {
    public static final String DEFAULT_SHELL = "/bin/bash";
    
    protected String directory;
    protected String script;
    
    public ShellScript(String script) {
        this.script = script;
    }
    
    public ShellScript(String script, String execDirectory) {
        this.script = script;
        this.directory = execDirectory;
    }
    
    public int run(String...args) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add( DEFAULT_SHELL );
        commands.add( script );
        for (String arg : args) commands.add( arg );
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        if (directory != null) {
            pb.directory( new File(directory) );
        }
        Process process = pb.start();
        Thread echo = new Thread( new EchoOutput(process.getInputStream()) );
        echo.start();
        int status = 1;
        try {
            status = process.waitFor();
            echo.join();
        } catch (InterruptedException e) {
            System.out.println("Script action " + script + " interrupted");
            process.destroy();
            try {
                // Catch any dying messages
                echo.join();
            } catch (InterruptedException e2) {
                // No further clean up attempts
            }
            status = 2;
        }
        return status;
    }
    
    class EchoOutput implements Runnable {
        protected BufferedReader in;

        public EchoOutput(InputStream in) {
            this.in = new BufferedReader( new InputStreamReader(in) );
        }
        
        @Override
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println( line );
                }
                in.close();
            } catch (IOException e) {
                // Quietly exit if the stream dies
            }
        }
    }
    
}
