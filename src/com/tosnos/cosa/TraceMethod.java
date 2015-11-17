package com.tosnos.cosa;

import soot.SootMethod;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by kevin on 4/22/15.
 */
public class TraceMethod {
    private SootMethod method;
    private HashMap<TraceMethod, Integer> children = new HashMap<TraceMethod, Integer>(); // Child method with the number of invocation
    private int methodId;
    private int testcaseId;
    private int numOfInvocation;
    private long elapsedTime;

    public TraceMethod(SootMethod method, int methodId, int testcaseId, int numOfInvocation, long elapsedTime ) {
        this.method = method;
        this.methodId = methodId;
        this.testcaseId = testcaseId;
        this.numOfInvocation = numOfInvocation;
        this.elapsedTime = elapsedTime;
    }

    public void addChild(TraceMethod child) {
        Integer numOfInvocation = children.get(child);
        if(numOfInvocation==null) {
            numOfInvocation = 0;
        }
        numOfInvocation++;
        children.put(child, numOfInvocation);
    }

    public void addChild(TraceMethod child, int numOfInvocation) {
        children.put(child, numOfInvocation);
    }

    public Set<TraceMethod> getChild() {
        return children.keySet();
    }
}
