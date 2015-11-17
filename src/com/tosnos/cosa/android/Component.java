package com.tosnos.cosa.android;

import soot.*;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.util.Chain;
import soot.util.NumberedString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by kevin on 11/16/14.
 */

public class Component {
    // ACTIVITY , FRAGMENT, APPLICATION, SERVICE, RECEIVER, Android Remote Interface

    public static final String NODE_MANIFEST = "manifest";
    public static final String NODE_APPLICATION = "application";
    public static final String NODE_ACTIVITY = "activity";
    public static final String NODE_ACTIVITY_ALIAS = "activity-alias";
    public static final String NODE_SERVICE = "service";
    public static final String NODE_RECEIVER = "receiver";
    public static final String NODE_PROVIDER = "provider";
    public static final String NODE_INTENT = "intent-filter";
    public static final String NODE_ACTION = "action";
    public static final String NODE_CATEGORY = "category";
    public static final String NODE_USES_SDK = "uses-sdk";
    public static final String NODE_INSTRUMENTATION = "instrumentation";
    public static final String NODE_USES_LIBRARY = "uses-library";
    public static final String NODE_SUPPORTS_SCREENS = "supports-screens";
    public static final String NODE_USES_CONFIGURATION = "uses-configuration";
    public static final String NODE_USES_FEATURE = "uses-feature";

    public static final int UNKNOWN = 0;
    public static final int APPLICATION = 1;
    public static final int ACTIVITY = 2;
    public static final int SERVICE = 3;
    public static final int PROVIDER = 4;
    public static final int RECEIVER = 5;
    public static final int ACTIVITY_ALIAS = 6;

    private SootMethod entry = null;

    private int type = UNKNOWN;
    private String name;
    private IntentFilter intents;
    private String targetActivityName;

    public Component(String type) {
        if (NODE_APPLICATION.equals(type)) {
            this.type = APPLICATION;
        } else if (NODE_ACTIVITY.equals(type)) {
            this.type = ACTIVITY;
        } else if (NODE_SERVICE.equals(type)) {
            this.type = SERVICE;
        } else if (NODE_RECEIVER.equals(type)) {
            this.type = RECEIVER;
        } else if (NODE_PROVIDER.equals(type)) {
            this.type = PROVIDER;
        } else if (NODE_ACTIVITY_ALIAS.equals(type)) {
            this.type = ACTIVITY_ALIAS;
        }
        this.intents = null;
    }

    public void setComponentClassName(String name) throws Exception {
        if (name == null) {
            throw new Exception("Component name should not be null");
        }
        this.name = name;
    }

    public void setTargetActivityName(String targetActivityName) {
        this.targetActivityName = targetActivityName;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the intents
     */
    public IntentFilter getIntents() {
        return intents;
    }

    /**
     * @param intents the intents to set
     */
    public void setIntents(IntentFilter intents) {
        this.intents = intents;
    }

//        private Set<NumberedString> getEntryPoints(SootClass sc) {
//            Set<NumberedString> entry = null;
//
//            while (sc.hasSuperclass()) {
//                if (!sc.isInAndroidPackage()) {
//                    entry = entryPoints.get(sc);
//                    break;
//                }
//                sc = sc.getSuperclass();
//            }
//
//            if (entry == null) {
//                entry = new HashSet<NumberedString>();
//                entryPoints.put(sc, entry);
//                while (sc.hasSuperclass()) {
//                    for (SootMethod m : sc.getMethods()) {
//                        if (m.getName().startsWith("on")) {
//                            entry.add(m.getNumberedSubSignature());
//                        }
//                    }
//                    sc = sc.getSuperclass();
//                }
//            }
//            return entry;
//        }

    public SootMethod getEntryPoint(AndroidPackage androidPackage) {
        if (entry != null) {
            return entry;
        }

        if (type > RECEIVER) {
            return null;
        }

        SootClass sc = Scene.v().loadClassAndSupport(name);
        if (sc.isPhantomClass()) { // skip for a phantom class
            return null;
        }


        int regIndex = 0;
        entry = new SootMethod("start" + sc.getName().replace(".", ""), new ArrayList(), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);

        JimpleBody body = Jimple.v().newBody(entry);
        entry.setActiveBody(body);

        Chain units = body.getUnits();
        Chain locals = body.getLocals();

        Local activityLocal = Jimple.v().newLocal("$r" + (regIndex++), sc.getType());
        locals.add(activityLocal);


        units.add(Jimple.v().newAssignStmt(activityLocal, Jimple.v().newNewExpr(sc.getType())));

        if (sc.declaresMethod("void <init>()")) {
            units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(activityLocal, sc.getMethod("void <init>()").makeRef())));
        }

        //            for (SootMethod method : AndroidAnalyzer.getMethodStartsWith(sc, "on")) {
        for (SootMethod method : sortMethodByLifeCycle(androidPackage.getOverriddenMethodsStartsWith(sc, "on"))) {

            //                SootMethod method = getMethod(sc, entry);
            if (method != null && method.getDeclaringClass().isApplicationClass()) {

                // Analyzer.v().addEventPoint(method);

                List<Value> params = new ArrayList<Value>();
                for (Type localType : method.getParameterTypes()) {
                    Value localValue;
                    if (localType instanceof PrimType) { //IntegerType) {
                        localValue = IntConstant.v(0);
                    } else {
                        SootClass localSc = Scene.v().loadClassAndSupport(localType.toString());
                        localValue = Jimple.v().newLocal("$r" + (regIndex++), localType);
                        locals.add(localValue);
                        units.add(Jimple.v().newAssignStmt(localValue, Jimple.v().newNewExpr(localSc.getType())));
                        if (localSc.declaresMethod("void <init>()")) {
                            units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr((Local) localValue, localSc.getMethod("void <init>()").makeRef())));
                        }
                    }
                    params.add(localValue);
                }
                units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(activityLocal, method.makeRef(), params)));
            }
            //              System.out.println(method);
        }

        units.add(Jimple.v().newReturnVoidStmt());
        return entry;

    }


    private List<SootMethod> sortMethodByLifeCycle(Collection<SootMethod> methods) {
        List<SootMethod> list = new ArrayList<SootMethod>();
        for (SootMethod method : methods) {
            int size = list.size();
            if (size == 0) {
                list.add(method);
            } else {
                int i = size - 1;
                if ("onStart".equals(name)) {
                    for (; i >= 0; i--) {
                        String name0 = list.get(i).getName();
                        if (name0.startsWith("onCreate") || name0.equals("onStart") || !name0.startsWith("on")) {
                            break;
                        }
                    }
                    list.add(i + 1, method);
                } else if ("onResume".equals(name)) {
                    for (; i >= 0; i--) {
                        String name0 = list.get(i).getName();
                        if (name0.startsWith("onCreate") || name0.equals("onStart") || name0.equals("onResume") || !name0.startsWith("on")) {
                            break;
                        }
                    }
                    list.add(i + 1, method);
                } else if ("onStop".equals(name)) {
                    for (; i >= 0; i--) {
                        String name0 = list.get(i).getName();
                        if (!name0.equals("onStop") && !name0.equals("onDestroy")) {
                            break;
                        }
                    }
                    list.add(i + 1, method);
                } else if ("onDestroy".equals(name)) {
                    list.add(method);
                } else {
                    // etc... onPause, onKeydown, onClick....
                    for (; i >= 0; i--) {
                        String name0 = list.get(i).getName();
                        if (!name0.startsWith("onPause") && !name0.equals("onStop") && !name0.equals("onDestroy")) {
                            break;
                        }
                    }
                    list.add(i + 1, method);
                }
            }
        }
        return list;
    }
}
