package com.tosnos.freeme;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;

import java.util.*;

/**
 * Created by kevin on 11/17/14.
 */
public class Node {
    // for FREEME
    public static final SootField EntirelyUsedField = new SootField("_______", null);
    boolean root = false;
    boolean restricted = false;
    private SootMethod method;
    private Map<Unit, Node> branches = new HashMap<Unit, Node>(); // HashMultimap.create();// new Multimap<SootMethod, Node>();
    private Set<SootField> requiredStaticFields = new HashSet<SootField>();
    private Multimap<Integer, SootField> requiredFieldMap = HashMultimap.create();
    private Table<Map.Entry<Unit, Node>, Integer, Integer> fieldIndexMap = HashBasedTable.create();
    private boolean filtered = false;

    public Node(SootMethod method) {
        this.method = method;
    }

    private static boolean hasMethodWriteObject(SootClass cl) {
        return cl.declaresMethod("void writeObject(java.io.ObjectOutputStream)");
//
//        try {
//            cl.getMethod("void writeObject(java.io.ObjectOutputStream)");
//            return true;
//        } catch (Exception e) {
//        }
//        return false;
    }

    public SootMethod getMethod() {
        return method;
    }

    public void setRoot() {
        root = true;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRestricted() {
        restricted = true;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void addRequiredStaticField(SootField f) {
        requiredStaticFields.add(f);
    }

    public Set<SootField> getRequiredStaticFields() {
        return requiredStaticFields;
    }

    public void addRequiredField(Integer idx, SootField field) {
        requiredFieldMap.put(idx, field);
    }

    public Collection<SootField> getRequriedFields(Integer idx) {
        return requiredFieldMap.get(idx);
    }

    public void addBranch(Unit u, Node branch) {
        branches.put(u, branch);
    }

    public Map<Unit, Node> getBranches() {
        return branches;
    }

    public void setFieldIndexForBranch(Unit u, Node branch, Integer paramIndex, int branchParamIndex) {
        fieldIndexMap.put(new AbstractMap.SimpleEntry(u, branch), paramIndex, branchParamIndex);
    }

    public Set<SootField> getAllRequiredFields(int index) {
        return getAllRequiredField(index, new HashSet<Node>());
    }

    private Set<SootField> getAllRequiredField(int index, Set<Node> visited) {
        Set<SootField> fields = new HashSet<SootField>();
        getAllRequiredField(index, fields, visited);
        return fields;
    }

    private void getAllRequiredField(int index, Set<SootField> fields, Set<Node> visited) {
        if (visited.contains(this)) {
            return;
        }
        visited.add(this);
        fields.addAll(getRequriedFields(index));
        for (Map.Entry<Unit, Node> branch : branches.entrySet()) {
            Integer idx = fieldIndexMap.get(branch, index);
            if (idx != null) {
                branch.getValue().getAllRequiredField(idx, fields, visited);
            }
        }
        //   visited.remove(this);
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered() {
        filtered = true;
    }
}
