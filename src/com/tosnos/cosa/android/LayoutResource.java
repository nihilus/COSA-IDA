package com.tosnos.cosa.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kevin on 11/16/14.
 */
public class LayoutResource {
    final String nodeName;
    LayoutResource parent = null;
    List<LayoutResource> children = new ArrayList<LayoutResource>();
    Map<String, Object> values = new HashMap<String, Object>();

    //        public LayoutResource() {
//        }
    public LayoutResource(String nodeName, LayoutResource parent) {
        this.nodeName = nodeName;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public void setParent(LayoutResource parent) {
        this.parent = parent;
    }

    public Object put(String key, Object obj) {
        return values.put(key, obj);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public void addChild(LayoutResource child) {
        children.add(child);
    }

    public String getString(String key) {
        Object obj = values.get(key);
        if (obj != null && obj instanceof String) {
            return (String) obj;
        }
        return null;
    }

    public String getNodeName() {
        return nodeName;
    }
}
