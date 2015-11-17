package com.tosnos.cosa.android;

import java.util.Hashtable;

/**
 * Created by kevin on 11/16/14.
 */
public class IntentFilter {
    private Hashtable<String, String> attributes;

    public IntentFilter() {
        attributes = new Hashtable<String, String>();
    }

    public void addAttribute(String attribute, String name) {
        attributes.put(attribute, name);
    }
}