package com.tosnos.freeme;

import com.google.common.collect.*;
import com.tosnos.cosa.DB;
import com.tosnos.cosa.android.AndroidPackage;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.function.Subroutine;
import javafx.scene.*;
import soot.*;
import soot.Scene;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;

import java.io.File;
import java.lang.annotation.Native;
import java.sql.*;
import java.util.*;

public class FreemeAnalyzer {
    private LinkedHashMultimap<Value, Value> storage = LinkedHashMultimap.create();
    private Map<SootMethod, Node> nodeMap = new HashMap<SootMethod, Node>();
    private String fileName;
    private Map<Type, Result> serializableTypeMap = new HashMap<Type, Result>();

    public static void updateCallGraph(String dir, int appNo) {
        ResultSet rs = null;
        try {
            rs = DB.getStmt().executeQuery("select app_no from callgraph where app_no=" + appNo);
            if(rs.next()) {
                System.out.println(appNo + " is skipped");
                return;
            }
            
            rs = DB.getStmt().executeQuery("select package from app where no=" + appNo);
            if(rs.next()) {



                String fileName = dir + rs.getString(1)+".apk";
                System.out.println(fileName);
                AndroidPackage androidPackage = new AndroidPackage(fileName);
                try {
                    CallGraph cg = androidPackage.getCallGraph(null);
                    if(cg!=null) {
                        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();

                        Set<SootMethod> methodSet = new HashSet<SootMethod>();

                        Iterator<Edge> edges = cg.iterator();
                        System.out.println(cg.size());
                        while(edges.hasNext()) {
                            Edge edge = edges.next();
                            methodSet.add(edge.src());
                            methodSet.add(edge.tgt());
                        }
                        for(SootMethod m:androidPackage.getDummyMethodSet()) {
                            methodSet.remove(m);
                        }
                        int totalMethodSet = methodSet.size();
                        int totalNativeMethod = 0;
                        Set<SootClass> classSet = new HashSet<SootClass>();
                        for(SootMethod m:methodSet) {
                            if(m.isNative()) {
                                totalNativeMethod++;
                            }
                            classSet.add(m.getDeclaringClass());
                        }
                        int totalClassSet = classSet.size();

                        int nativeMethod = 0;
                        int numOfMethod = 0;
                        Set<SootClass> cs = new HashSet<>();
                        for(SootMethod m:methodSet) {
                            if(Scene.v().getApplicationClasses().contains(m.getDeclaringClass())) {
                                numOfMethod++;
                                if(m.isNative()) {
                                    nativeMethod++;
                                }
                                cs.add(m.getDeclaringClass());
                            }
                        }

                        DB.getStmt().execute("insert into callgraph values(" + appNo + "," + totalMethodSet + ", " + totalNativeMethod + ", " + totalClassSet + ", " +numOfMethod+ ", " + nativeMethod+ ", " +cs.size()+ ")");

                        System.out.println(totalMethodSet + " " + totalNativeMethod);

/*
                        create table callgraph (
                            app_no int,
                        reachable_methods int,
                        native_methods int,
                        reachable_classes int
                        methods int,
                        native int,
                        classes int
                        )
*/

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public FreemeAnalyzer(String fileName) {
        this.fileName = fileName;
    }


    private static boolean checkSerialization(Type type) {
        boolean ret =
                isAssignableFrom(type, "android.os.Handler") ||
                        isAssignableFrom(type, "android.os.PowerManager$WakeLock") ||
                        isAssignableFrom(type, "android.widget.") ||
                        isAssignableFrom(type, "android.view.") ||
                        isAssignableFrom(type, "android.graphics.") ||
                        isAssignableFrom(type, "android.database.") ||
                        isAssignableFrom(type, "android.media.") ||
                        isAssignableFrom(type, "android.location.") ||
                        //         isAssignableFrom(type, "java.nio.") ||
                        //           isAssignableFrom(type, "java.io.File") ||
                        isAssignableFrom(type, "java.io.OutputStream") ||
                        isAssignableFrom(type, "java.io.InputStream");
        return ret;
    }

    private static boolean checkSerialization(List<Type> list) {
        for (Type type : list) {
            if (checkSerialization(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPrimitive(Type type) {
        Type baseType = type;
        if (type instanceof ArrayType) {
            baseType = ((ArrayType) type).baseType;
        }

        if (baseType instanceof PrimType) {
            return true;
        }
        return "java.lang.Integer".equals(type.toString()) ||
                "java.lang.Double".equals(type.toString()) ||
                "java.lang.Long".equals(type.toString()) ||
                "java.lang.Short".equals(type.toString()) ||
                "java.lang.Float".equals(type.toString()) ||
                "java.lang.Boolean".equals(type.toString()) ||
                "java.lang.Char".equals(type.toString()) ||
                "java.lang.Byte".equals(type.toString()) ||
                "java.lang.String".equals(type.toString());
    }

    public static boolean isParceable(SootClass cl) {
        return isAssignableFrom(cl, "android.os.Parcelable");
    }

    private static boolean isAssignableFrom(SootClass cl, String className) {
        String clName = cl.getName();
        boolean ret = className.equals(clName);

        if (!ret && className.endsWith(".")) {
            ret = clName.contains(className);
        }

        if (!ret && cl.hasSuperclass()) {
            ret = isAssignableFrom(cl.getSuperclass(), className);
        }
        return ret;
    }

    private static boolean isAssignableFrom(Type type, String className) {
        Type baseType = type;
        if (type instanceof ArrayType) {
            baseType = ((ArrayType) type).baseType;
        }
        return isAssignableFrom(((RefType) baseType).getSootClass(), className);
//        return isAssignableFrom(Scene.v().loadClassAndSupport(baseType.toString()), className);
    }

    public void doAnalysis() {
        doAnalysis((short)-1,(short)-1, null);
    }



//
//    public void nativeTravel(NativeLibraryHandler handler, CallGraph cg, SootMethod sm) {
//        nativeTravel(new HashSet<SootMethod>(), handler, cg, sm);
//    }
//
//    public void nativeTravel(Set<SootMethod> visited, NativeLibraryHandler handler, CallGraph cg, SootMethod sm) {
//        if(sm.isNative()) {
//            System.out.println(sm.getName());
//        }
//
//        Iterator<Edge> edges = cg.edgesOutOf(sm);
//        while(edges.hasNext()) {
//            Edge edge = edges.next();
//            SootMethod tgt = edge.tgt();
//            if(visited.contains(tgt)) {
//                return;
//            }
//            visited.add(tgt);
//            nativeTravel(visited, handler, cg, tgt);
//        }
//    }
//

    public void doAnalysis(short targetCPU, short secondTargetCPU, File dump) {
        AndroidPackage androidPackage = new AndroidPackage(fileName);
        try {
            CallGraph cg = androidPackage.getCallGraph(dump);

            if (targetCPU > 0) { // Do Native Analysis
                if (targetCPU == AndroidPackage.CPU_TYPE_X86) {
                    throw new RuntimeException("X86 architecture is not supported");
                }

                List<LibraryModule> modules = androidPackage.getModules(targetCPU);
                if (modules.size() == 0) {
                    modules = androidPackage.getModules(secondTargetCPU);
                }
                NativeLibraryHandler handler = NativeLibraryHandler.getInstance();
                handler.init(modules, targetCPU);
                handler.traverse(cg, Scene.v().getMainMethod());
            }



//
//                    //            do {
//                 //   buildCallGraph();
//
//                    System.out.println("Checking native methods....");
//
//                    //            } while(nativeAnalyzer.traverse(Scene.v().getCallGraph(), Scene.v().getMainMethod())>0);
//                    // buildCallGraph();
////                    List<Subroutine> nativeMethods = handler.traverse(nativeMethodChains);
////                    for (Subroutine nativeMethod : nativeMethods) {
////                        //  System.out.println(nativeMethod.getBody());
////                    }
//                }


            //          System.out.println("Filtering call graph.....");
//            Node mainNode = getNode(Scene.v().getMainMethod());
            //           filter(Scene.v().getCallGraph(), mainNode);
            //          reorganize(mainNode);


//        filter(cg, Scene.v().getMainMethod(), 0);
//
//        System.out.println("Reorganizing.......");
//
//        reorganize();
//
//        for (SootMethod method : rootNodes) {
//            if (method.isRestricted()) {
//                rootNodes.remove(method);
//            }
//        }


            // eliminate a simple method
//        Set<SootMethod> methods = new HashSet<SootMethod>(rootNodes);
//        for (SootMethod method : methods) {
//            Node node = nodeMap.get(method);

//            if(Misc.getHashCode(method)==-1544545078) {
//                Node node = rootNodes.get(method);
//                for(Node branch:node.getBranches()) {
//                    System.out.println(branch.getMethod().getActiveBody().toString());
//                }
//
//                break;
//            }
//            if (node.getSizeOfBranches() == 0) {
            //              rootNodes.remove(method);
            //        }
//        }


/*
        result[0] = totalMethod;
        result[1] = initReachableMethod;
        result[2] = accessedClass;
        result[3] = reachableMethod;
        result[4] = rootNodes.size();

        System.out.println("Total methods: " + totalMethod);
        System.out.println("Initially reachable methods: " + initReachableMethod);
        System.out.println("Classes with at least one reachable method: " + accessedClass);
        System.out.println("Number of reachable methods: " + reachableMethod);
        System.out.println("Total remote executable methods : " + rootNodes.size());


        // eliminate a simple method
//        Set<SootMethod> methods = new HashSet<SootMethod>(rootNodes);
//        for (SootMethod method : methods) {
//            Node node = nodeMap.get(method);
//
////            if(Misc.getHashCode(method)==-1544545078) {
////                Node node = rootNodes.get(method);
////                for(Node branch:node.getBranches()) {
////                    System.out.println(branch.getMethod().getActiveBody().toString());
////                }
////
////                break;
////            }
//            if (node.getSizeOfBranches() == 0) {
//                rootNodes.remove(method);
//            }
//        }
*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("done!!");
    }

    private Node getNode(SootMethod method) {
        Node node = nodeMap.get(method);
        if (node == null) {
            node = new Node(method);
            nodeMap.put(method, node);
        }
        return node;
    }

    public void filter(CallGraph cg, Node node) {   // filter restricted methods
        if (node.isFiltered() || node.getMethod().isJavaLibraryMethod()) {
            return;
        }

        node.setFiltered();
        SootMethod method = node.getMethod();

        for (Iterator<Edge> it = cg.edgesOutOf(method); it.hasNext(); ) {
            Edge e = it.next();
            SootMethod sm = e.tgt();
            node.addBranch(e.srcUnit(), getNode(sm));
        }

        if (method.isConcrete()) {
            Body b = method.retrieveActiveBody();

            HashMap<Value, Integer> paramLocals = new HashMap<Value, Integer>();
            if (!method.isStatic()) {         /// get "This" instance
                paramLocals.put(b.getThisLocal(), 0);
            }

            for (int i = 1; i <= method.getParameterCount(); i++) {  // get parameters
                paramLocals.put(b.getParameterLocal(i - 1), i); // parameter starts with 1 always
            }

            for (Unit u : b.getUnits()) { // inspect body
                for (ValueBox valueBox : u.getUseAndDefBoxes()) {
                    Value v = valueBox.getValue();
                    if (v instanceof StaticFieldRef) {   // add required static fields
                        SootField f = ((StaticFieldRef) v).getField();
                        if (!f.getDeclaringClass().isLibraryClass()) {
                            node.addRequiredStaticField(f);
                        }
                    } else if (v instanceof InstanceFieldRef) {
                        SootField f = ((InstanceFieldRef) v).getField();
                        Value base = ((InstanceFieldRef) v).getBase();
                        Integer paramIndex = paramLocals.get(base);
                        if (paramIndex != null) {
                            node.addRequiredField(paramIndex, f);
                        }
                    } else if (v instanceof InvokeExpr) {
                        Node branch = node.getBranches().get(u);
                        if (branch != null) {
                            if (checkRestrictedMethodCall(branch.getMethod())) {
                                branch.setRestricted();
                            }

                            if (v instanceof InstanceInvokeExpr) { // for dynamic method
                                Value base = ((InstanceInvokeExpr) v).getBase(); // for this parameters
                                Integer paramIndex = paramLocals.get(base); // get this object from parameters
                                if (paramIndex != null) {
                                    if (method.isConcrete() && !branch.getMethod().isJavaLibraryMethod()) {
                                        node.setFieldIndexForBranch(u, branch, paramIndex, 0);
                                    } else {
                                        node.addRequiredField(paramIndex, Node.EntirelyUsedField); // if it is library functions...
                                    }
                                }
                            }

                            // for other args
                            List<Value> args = ((InvokeExpr) v).getArgs();
                            for (int i = 0; i < args.size(); i++) {
                                Value arg = args.get(i);
                                Integer paramIndex = paramLocals.get(arg);
                                if (paramIndex != null) {
                                    if (method.isConcrete() && !branch.getMethod().isJavaLibraryMethod()) {
                                        node.setFieldIndexForBranch(u, branch, paramIndex, i + 1);
                                    } else {
                                        node.addRequiredField(paramIndex, Node.EntirelyUsedField);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (checkRestrictedStaticField(node)) {
            node.setRestricted();
        }

        for (Node branch : node.getBranches().values()) {
            filter(cg, branch);
        }
    }

    private Result isSerializable(List<Type> list) {
        for (Type type : list) {
            if (isSerializable(type) == Result.FALSE) {
                return Result.FALSE;
            }
        }
        return Result.TRUE;
    }

    private Result isSerializable(Type type) {
        return isSerializable(type, null);
    }

//    private CodeAnalyzer[] loadNativeMethods(String fileName) {
//        CodeAnalyzer[] libraries = CodeAnalyzer.loadLibrary(fileName);
//        if(libraries!=null) {
//            for(int i=0;i<libraries.length;i++) {
//                if(libraries[i]!=null) {
//                    for(NativeFunction m:libraries[i].getNativeMethods()) {
//                        generateBody(m);
//                    }
//                }
//            }
//        }
//        return libraries;
//    }


    // machine leaning to determine a branch when the parameter of a target methods are given.

    private Result isSerializable(Type type, Stack<SootClass> stack) {
        // check cache...
        if (type == null) {
            return Result.UNKNOWN;
        }

        Type baseType = type;
        if (type instanceof ArrayType) {
            baseType = ((ArrayType) type).baseType;
        }

        if (baseType instanceof VoidType) {
            return Result.TRUE;
        }

        if ("java.lang.Object".equals(baseType.toString())) { // Object.class
            return Result.UNKNOWN;
        }

        Result result = serializableTypeMap.get(baseType);
        if (result != null) {
            return result;
        }


        if (isAssignableFrom(type, "android.app.Activity")) { // handle for a special class type
            serializableTypeMap.put(baseType, Result.TRUE);
            return Result.TRUE;
        }


        if (isPrimitive(baseType)) {
            serializableTypeMap.put(baseType, Result.TRUE);
            return Result.TRUE;
        }


        if (checkSerialization(baseType)) {
            System.out.println("restricted " + baseType.toString());
            serializableTypeMap.put(baseType, Result.FALSE);
            return Result.FALSE;

        }

//        SootClass cl = Scene.v().loadClassAndSupport(baseType.toString());
        SootClass cl = ((RefType) baseType).getSootClass();


        if (cl.implementsInterface("java.io.Serializable")) {
            serializableTypeMap.put(baseType, Result.TRUE);
            return Result.TRUE;
        }


        if (stack == null) {
//            stack =  new WeakReference<Stack<SootClass>>(new Stack<SootClass>());
            stack = new Stack<SootClass>();

        } else if (stack.contains(cl)) { // check if the class is under processing.
//        } else if(stack.get().contains(cl)) { // check if the class is under processing.
            return Result.UNKNOWN;
        }

//        stack.get().push(cl);
        stack.push(cl);
        Result ret = Result.TRUE;
        for (SootField field : cl.getFields()) {
            if (!field.isStatic()) { // check only non-static fields
                Result subResult = isSerializable(field.getType(), stack);
                if (subResult == Result.FALSE) {
                    return Result.FALSE;
                }
            }
        }
//        stack.get().pop();
        stack.pop();
        if (result != null) {
            serializableTypeMap.put(baseType, ret);
        }
        return ret;
    }

    private boolean checkRestrictedMethodCall(SootMethod m) {
        String className = m.getDeclaringClass().getName();
        boolean ret =
//                        className.contains("java.lang.Runtime") || // prevent execute system dependents call
                className.contains("android.view") ||
                        className.contains("android.os.Handler") ||
//                        className.contains("android.os.SystemClock") ||
                        className.contains("android.widget") ||
                        className.contains("android.io.File") ||
                        className.contains("android.os.Environment") ||
                        className.contains("android.content.Intent");
//                        className.contains("android.os.Message");

        if (!ret) {
            if (m.getName().contains("getSystemService") && isAssignableFrom(m.getDeclaringClass(), "android.content.Context")) {
                return true;
            }

            if (isAssignableFrom(m.getDeclaringClass(), "android.app.Activity")) {
                if (m.getName().contains("getActionBar")) {
                    return true;
                }
//                if (m.getName().contains("findViewById")) {
//                    return true;
//                }
                if (m.getName().contains("invalidate")) {
                    return true;
                }
                if (m.getName().contains("isShowing")) {
                    return true;
                }
            }

            if (m.getName().contains("notifyDataSetChanged")) {
                return true;
            }
        }
        return ret;
    }

    private boolean checkRestrictedStaticField(Node node) {
        for (SootField field : node.getRequiredStaticFields()) {
            if (isSerializable(field.getType()) == Result.FALSE) {
                return true;
            }
        }
        return false;
    }

//    private boolean reorganizeNode(SootMethod method, Set<SootMethod> visited) {
//        boolean ret = true;
//
//        if (visited.contains(method)) {
//            return true;
//        }
//
//        visited.add(method);
//
//        if (method.isRestricted()) {
//            // if the current node has a restriction, put its child nodes to the temporary nodes
//            for (SootMethod branch : method.getBranches().values()) {
//                tmpRootNodes.add(branch);
//            }
//            ret = false;
//        } else { // if a child node has no restriction, check the next children.
//            for (SootMethod branch : method.getBranches().values()) {
//                if (!reorganizeNode(branch, visited)) { // if it has restrictions....
//                    tmpRootNodes.add(branch);
//                    ret = false;
//                }
//            }
//        }
//
//        if (ret && !checkRootNodeRestiction(method)) {
////            Node oldNode = rootNodes.get(node.getMethod()); //  if there is the same method in rootNodes, should combine two nodes.
////            if(oldNode!=null) {
////                if(oldNode!=node) {
////                    oldNode.combine(node);
////                }
////            } else {
//            rootNodes.add(method);
////            }
////            node.clearFieldIndexMap();
//
//
///*            SootMethod method = node.getMethod();
//                        if(method.getSignature().equals("<com.google.android.chess.b: void a(int)>")) {
//                System.out.println(method.getSignature());
//                for(Iterator<Node> it1 = node.getBranches();it1.hasNext();) {
//                    Node n = it1.next();
//                    System.out.println(n.getMethod());
//                    System.out.println(n.getMethod().getSignature() + " " + n.getFieldIndexMap(0));
//                }
//            }*/
//        } else {
//            method.setRestricted();
//        }
//
//        return ret;
//    }


//    public boolean checkRootNodeRestiction(SootMethod method) {   // check serialization...
//
//        if (method.isConstructor() || "<clinit>".equals(method.getName())) {
//            return true;
//        }
//
////        if(isSerializable(method.getParameterTypes())==Result.FALSE) {
////            return true;
////        }
//
//
//        if (checkRestrictedFields(method)) {
//            return true;
//        }
//
//
//        if (isSerializable(method.getReturnType()) == Result.FALSE) {
//            return true;
//        }
////
////
////        // if this is non-static method, it need to check the field of a class.
////        if(!method.isStatic()) {
////            if(checkSerialization(method.getDeclaringClass().getType())) {
////                return true;
////            }
////            // TODO: check required parameter and this ....
//////            for(Iterator<SootField> it=method.getDeclaringClass().getFields().snapshotIterator();it.hasNext();) {   // check requried field from parameter..
//////                SootField field = it.next();
//////                if(!field.isStatic() && checkRestriction(field.getType())) {
//////                    return true;
//////                }
//////            }
////        }
//        return false;
//
//    }

    public Set<Map.Entry<Unit, Node>> reorganize(Node node) {

//        Set<SootMethod> visited = new HashSet<SootMethod>();
//        tmpRootNodes.add(Scene.v().getMainMethod());
//        while (!tmpRootNodes.isUnknown()) {
//            SootMethod node = tmpRootNodes.first();
//            tmpRootNodes.remove(node);
//            reorganizeNode(node, visited);
//        }
        return null;
    }

    private boolean checkRestrictedFields(Node node) {
        SootMethod method = node.getMethod();
        if (!method.isStatic()) {
            // check required field of this instance
            Set<SootField> requiredField = node.getAllRequiredFields(0);
            if (requiredField.contains(Node.EntirelyUsedField)) {
                if (isSerializable(method.getDeclaringClass().getType()) == Result.FALSE) {
                    return true;
                }
            } else {
                for (SootField field : requiredField) {
                    if (isSerializable(field.getType()) == Result.FALSE) {
                        return true;
                    }
                }
            }
        }

        int paramCount = method.getParameterCount();
        for (int i = 0; i < paramCount; i++) {
            Set<SootField> requiredField = node.getAllRequiredFields(i + 1);
            if (requiredField.contains(Node.EntirelyUsedField)) {
                if (isSerializable(method.getParameterType(i)) == Result.FALSE) {
                    return true;
                }
            } else {
                for (SootField field : requiredField) {
                    if (isSerializable(field.getType()) == Result.FALSE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private enum Result {UNKNOWN, TRUE, FALSE}



    /*
       Collect fundamental information from a callgraph of APK file.
     */
/*
    public static void getCallGraphInfo(String no, String fileName) {
        Connection conn = null;
      //  Statement stmt = null;
      //  ResultSet rs = null;
        PreparedStatement pstmt0 = null;
        PreparedStatement pstmt1 = null;

        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
      //      stmt = conn.createStatement();
       //     rs = stmt.executeQuery("select no, package from app where no>=2 order by no asc ");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int totalMethod, totalNativeMethods, totalClasses, packageMethods, packageNativeMethods, packageClasses;

//        try {
//            while (rs.next()) {
//                String no = rs.getString(1);
//                String fileName = rs.getString(2);
                System.out.println(no+". " + fileName);
//                String fileName = "com.bigduckgames.flow";

                try {
                    AndroidAnalyzer analyzer = new AndroidAnalyzer(fileName);

                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("select no from callgraph0 where no="+no);

                    if(!rs.next()) {
                        totalMethod = totalNativeMethods = totalClasses = packageMethods = packageNativeMethods = packageClasses = 0;
                        analyzer.buildCallGraph(false);
                        totalMethod = Scene.v().getReachableMethods().size();
                        QueueReader<MethodOrMethodContext> methods = Scene.v().getReachableMethods().listener();
                        Set<SootClass> classSet = new HashSet<SootClass>();
                        while (methods.hasNext()) {
                            SootMethod sm = methods.next().method();
                            classSet.add(sm.getDeclaringClass());
                            if (AndroidPackage.isAPK(sm)) {
                                packageMethods++;
                            }
                            if (sm.isNative()) {
                                if (AndroidPackage.isAPK(sm)) {
                                    packageNativeMethods++;
                                }
                                totalNativeMethods++;
                            }
                        }

                        for (SootClass sc : classSet) {
                            if (AndroidPackage.isAPK(sc)) {
                                packageClasses++;
                            }
                        }
                        totalClasses = classSet.size();

                        System.out.println("Reachable classes  = " + totalClasses);
                        System.out.println("Reachable package classes  = " + packageClasses);
                        System.out.println("Reachable methods  = " + totalMethod);
                        System.out.println("Reachable native methods  = " + totalNativeMethods);
                        System.out.println("Reachable package methods  = " + packageMethods);
                        System.out.println("Reachable package native methods  = " + packageNativeMethods);

                        try {
                            pstmt0 = conn.prepareStatement("insert into callgraph0(no,totalMethods,totalNativeMethods,totalClasses,packageMethods,packageNativeMethods,packageClasses) values(?,?,?,?,?,?,?)");
                            pstmt0.setString(1, no);
                            pstmt0.setInt(2, totalMethod);
                            pstmt0.setInt(3, totalNativeMethods);
                            pstmt0.setInt(4, totalClasses);
                            pstmt0.setInt(5, packageMethods);
                            pstmt0.setInt(6, packageNativeMethods);
                            pstmt0.setInt(7, packageClasses);
                            pstmt0.execute();
                            pstmt0.close();
                        } catch (Exception e) {

                        }
                    }
                    stmt.close();
                    rs.close();


                    stmt = conn.createStatement();
                    rs = stmt.executeQuery("select no from callgraph where no="+no);
                    if(!rs.next()) {
                        totalMethod = totalNativeMethods = totalClasses = packageMethods = packageNativeMethods = packageClasses = 0;

                        analyzer.buildCallGraph(true);

                        totalMethod = Scene.v().getReachableMethods().size();
                        QueueReader<MethodOrMethodContext> methods = Scene.v().getReachableMethods().listener();
                        Set<SootClass> classSet = new HashSet<SootClass>();
                        while (methods.hasNext()) {
                            SootMethod sm = methods.next().method();
                            classSet.add(sm.getDeclaringClass());
                            if (AndroidPackage.isAPK(sm)) {
                                packageMethods++;
                            }
                            if (sm.isNative()) {
                                if (AndroidPackage.isAPK(sm)) {
                                    packageNativeMethods++;
                                }
                                totalNativeMethods++;
                            }
                        }

                        for (SootClass sc : classSet) {
                            if (AndroidPackage.isAPK(sc)) {
                                packageClasses++;
                            }
                        }
                        totalClasses = classSet.size();
                        System.out.println("Reachable classes  = " + totalClasses);
                        System.out.println("Reachable methods  = " + totalMethod);
                        System.out.println("Reachable native methods  = " + totalNativeMethods);
                        System.out.println("Reachable package classes  = " + packageClasses);
                        System.out.println("Reachable package methods  = " + packageMethods);
                        System.out.println("Reachable package native methods  = " + packageNativeMethods);
                        try {
                            pstmt1 = conn.prepareStatement("insert into callgraph(no,totalMethods,totalNativeMethods,totalClasses,packageMethods,packageNativeMethods,packageClasses) values(?,?,?,?,?,?,?)");
                            pstmt1.setString(1, no);
                            pstmt1.setInt(2, totalMethod);
                            pstmt1.setInt(3, totalNativeMethods);
                            pstmt1.setInt(4, totalClasses);
                            pstmt1.setInt(5, packageMethods);
                            pstmt1.setInt(6, packageNativeMethods);
                            pstmt1.setInt(7, packageClasses);
                            pstmt1.execute();
                            pstmt1.close();
                        } catch (Exception e) {

                        }
                    }
                    stmt.close();
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
           //}
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println(e);
            }
//        }
    }
*/

    /**
     * Created by kevin on 11/17/14.
     */
    public static class Node {
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
}
