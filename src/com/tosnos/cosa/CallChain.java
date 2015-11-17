package com.tosnos.cosa;

import java.util.LinkedHashSet;

/**
 * Created by kevin on 7/20/15.
 */
public class CallChain {
    private LinkedHashSet<CallChain> callees = new LinkedHashSet<CallChain>();
    private TraceMethod traceMethod;

    public CallChain(TraceMethod traceMethod) {
        this.traceMethod = traceMethod;
    }

    public void addCallee(CallChain callee) {
        this.callees.add(callee);
    }

    public int sizeOfCallees() {
        return callees.size();
    }

    public LinkedHashSet<CallChain> getCallees() {
        return callees;
    }
}
