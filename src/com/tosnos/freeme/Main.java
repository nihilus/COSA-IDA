package com.tosnos.freeme;


import com.tosnos.cosa.android.AndroidPackage;
import java.io.IOException;

/**
 * Created by kevin on 11/17/14.
 */
public class Main {
    private final static String APP_DIR = "./apps/";
    private final static String VERSION = "0.1";
    private final static String TITLE = "Comprehensive Static Analyzer For Android Application (COSA with IDA Pro)";
    private final static String AUTHOR = "Sangwon Lee(sangwon.lee@usc.edu)";
    private final static int SKIP_LINES = 8;

    private static void about() {
        System.out.println(TITLE + " " + VERSION);
        System.out.println("Copyright (c) 2014, University of Southern California All rights reserved");
        System.out.println("Author: " + AUTHOR);
    }

    public static void main(String[] args) {
        about();
        FreemeAnalyzer freemeAnalyzer = new FreemeAnalyzer("FreemeTest.apk");
        System.out.println("done");
    }

    public void addLibrary() throws IOException {
        AndroidPackage.addExtraLibrary("org.opencv.engine", AndroidPackage.CPU_TYPE_ARM_V7A);
    }
}
