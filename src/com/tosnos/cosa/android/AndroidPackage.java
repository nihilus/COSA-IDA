package com.tosnos.cosa.android;

import com.google.common.collect.*;
import com.tosnos.cosa.CallChain;
import com.tosnos.cosa.DB;
import com.tosnos.cosa.TraceMethod;
import com.tosnos.cosa.binary.LibraryModule;
import com.tosnos.cosa.binary.NativeLibraryHandler;
import com.tosnos.cosa.binary.elf.ElfHelper;
import com.tosnos.cosa.binary.function.Subroutine;
import com.tosnos.cosa.util.Misc;
import com.tosnos.cosa.util.SigNumberedString;
import one.elf.ElfSymbol;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.tagkit.CodeAttribute;
import soot.util.Chain;
import soot.util.NumberedString;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class AndroidPackage {
    public static final int DYNAMIC_ANALYSIS = 1;
    public static final int STATIC_ANALYSIS = 0;

    public static final int API_LEVEL = 16; // for Jelly bean
    public static final short CPU_TYPE_ARM = 4;
    public static final short CPU_TYPE_ARM_V7A = 10;
    public static final short CPU_TYPE_X86 = 3;
    public static final short CPU_TYPE_MIPS = 8;
    protected static final int BUTTON_POSITIVE = -1;
    protected static final int BUTTON_NEGATIVE = -2;
    protected static final int BUTTON_NEUTRAL = -3;
    private static final String TAG_TRIGGER = "trigger";
    private static final String TAG_RECEIVER = "receiver";
    private static final String TAG_CALLBACK = "callback";
    private static String extraLibDir = "libs/";
    public static final String MANIFEST_XML = "AndroidManifest.xml";
//    public static final String ASSETS_DIR = "assets/";
//    public static final String RES_DIR = "res/";
    public static final String LAYOUT_DIR = "res/layout/";
//    public static final String ARMEABI = "lib/armeabi/";
//    public static final String ARMEABI_V7A = "lib/armeabi-v7a/";
//    public static final String X86 = "lib/X86/";
    private static Logger logger = LoggerFactory.getLogger(AndroidPackage.class.getName());
    private final AndroidManifest androidManifest;
    private final String filePath;
    private final String packageName;
    private List<LibraryModule> modules;
    private int numOfNativeMethods = 0;

    protected final RefType clStart = RefType.v("java.lang.Thread");
    public Map<Integer, LayoutResource> idMap = new HashMap<Integer, LayoutResource>();
    public Map<String, LayoutResource> tagMap = new HashMap<String, LayoutResource>();
    protected LinkedHashMultimap<NumberedString, Receiver> receiverMap;
    protected Map<NumberedString, String> executorMap = new HashMap<NumberedString, String>();
    protected Map<RefType, ListenerOrCallBack> listenerOrCallBackMap;
    private LinkedHashMultimap<NumberedString, TempReceiver> callbackOrListenerTriggerMap = LinkedHashMultimap.create();
    private Map<SootClass, Map<NumberedString, SootMethod>> overriddenMethodMaps = new HashMap<SootClass, Map<NumberedString, SootMethod>>();
    private Map<SootClass, Set<NumberedString>> inheritableMethodMap = new HashMap<SootClass, Set<NumberedString>>();
    private Map<SootClass, Set<SootMethod>> interfaceMethodMap = new HashMap<SootClass, Set<SootMethod>>();
    private Table<SootClass, String, Set<SootMethod>> overriddenMethodsStartsWithMap = HashBasedTable.create();
    private Table<Value, Integer, Value> receiverValueTable = HashBasedTable.create();

    private Map<Integer, SootMethod> methodIdMap = new HashMap<Integer, SootMethod>();
    private Map<SootMethod, TraceMethod> traceMethodMap = new HashMap<SootMethod, TraceMethod>();
    private LinkedHashSet <CallChain>  nativeMethodChains = new LinkedHashSet<CallChain>();
    private Set<SootMethod> dummyMethodSet = new HashSet<SootMethod>();
    private int appNo = -1;

    public AndroidPackage(String filePath) {
        this.filePath = filePath;
        try {
            ZipFile zipFile = new ZipFile(filePath);
            ZipEntry zipEntry = zipFile.getEntry(MANIFEST_XML);
            if (zipEntry != null) {
                androidManifest = new AndroidManifest(zipFile.getInputStream(zipEntry));
                packageName = androidManifest.getPackageName();
                ResultSet rs = DB.getStmt().executeQuery("select no from app where package='" + packageName + "'");
                if(rs.next()) {
                    appNo = rs.getInt(1);
                } else {
                    System.err.println(packageName + " is not exist on Database");
//                    throw new RuntimeException(packageName + " is not exist on Database");
                }
            } else {
                throw new RuntimeException("Error when looking for manifest in apk");
            }

            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.getName().startsWith(LAYOUT_DIR) && entry.getName().endsWith("xml")) {
                    parseLayoutXML(zipFile.getInputStream(entry));
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error when looking for manifest in apk: " + e);
        }
        initReceivers();
    }

    public Set<SootMethod> getDummyMethodSet() {
        return dummyMethodSet;
    }

    public int getAppNo() {
        return appNo;
    }

    private void buildCallGraph() {
        Options.v().setPhaseOption("cg.spark", "on");
        Map<String, String> sootOptions = new HashMap<String, String>();
        sootOptions.put("vta", "true");
        sootOptions.put("on-fly-cg", "false");
        sootOptions.put("enabled", "true");
        sootOptions.put("use-original-names", "false");
        sootOptions.put("verbose", "true");
        sootOptions.put("propagator", "worklist");
        sootOptions.put("set-impl", "double");
        sootOptions.put("double-set-old", "hybrid");
        sootOptions.put("double-set-new", "hybrid");
        sootOptions.put("ignore-types", "false");
        SparkTransformer.v().transform("", sootOptions);
    }

    public void buildDynamicCallGraph() throws SQLException {
        LinkedHashSet<CallChain> nativeCallChainRootSet = null;
        ResultSet rs;
        int testcaseId = -1;
        // get recent testcase id for the target app
        rs = DB.getStmt().executeQuery("select id from testcase where app_no=" + appNo + " order by id desc limit 1");
        if(!rs.next()) {
            return;
        }




        testcaseId = rs.getInt(1);
        rs = DB.getStmt().executeQuery("select method_id, testcase_id, num_of_invocation, elapsed_time from tracemethod where testcase_id=" + testcaseId);
        while(rs.next()) {
            int methodId = rs.getInt(1);
            SootMethod sm = methodIdMap.get(methodId);
            if(sm==null) {
                ResultSet rs0 = DB.getStmt().executeQuery("select class.name, method.signature, method.accessflags from method, class where method.id=" + methodId + " and method.class_id = class.id");
                if(rs0.next()) {
                    String className = rs0.getString(1);
                    String methodSig = rs0.getString(2);
                    SootClass sc = Scene.v().loadClassAndSupport(className);
                    if(!sc.isPhantom()) {
                        String[] methodInfo = Misc.parseMethodInfo(methodSig);
                        List<Type> paramTypes = Misc.getTypeList(methodInfo[1]);
                        Type returnType = Misc.getType(methodInfo[2]);
                        sm = sc.getMethod(methodInfo[0], paramTypes, returnType);
                        methodIdMap.put(methodId, sm);
                    }
                }
            }

            if(sm!=null) {
                TraceMethod tm = traceMethodMap.get(sm);
                if (tm == null) {
                    tm = new TraceMethod(sm, methodId, rs.getInt(2), rs.getInt(3), rs.getLong(4));
                    traceMethodMap.put(sm, tm);
                }
            }
        }

        // add child
        rs = DB.getStmt().executeQuery("select method_id, child_id, num_of_invocation from tracemethod_child where testcase_id=" + testcaseId + " order by method_id");
        while(rs.next()) {
            int methodId = rs.getInt(1);
            int childId = rs.getInt(2);
            int numOfInvocation = rs.getInt(3);
            TraceMethod traceMethod = traceMethodMap.get(methodIdMap.get(methodId));
            TraceMethod childMethod = traceMethodMap.get(methodIdMap.get(childId));
            traceMethod.addChild(childMethod, numOfInvocation);
        }

        Map<Integer, CallChain> callChainIdMap = new HashMap<Integer, CallChain>();
        nativeCallChainRootSet = new LinkedHashSet<CallChain>();
        rs = DB.getStmt().executeQuery("select min(methodcall.id) from methodcall, method, class where methodcall.testcase_id=" + testcaseId + " and methodcall.method_id=method.id and class.id=method.class_id and class.app_no=" + appNo + " and (method.accessflags&256)==256 group by method_id order by methodcall.id");
        while(rs.next()) {
            int callchainId = rs.getInt(1);
            ResultSet rs0;
            CallChain lastCallChain = null;
            do {
                rs0 = DB.getStmt().executeQuery("select method_id, caller_id from methodcall where testcase_id=" + testcaseId + " and id=" + callchainId);
                int methodId = rs0.getInt(1);
                CallChain callChain = callChainIdMap.get(methodId);
                if(callChain==null) {
                    callChain = new CallChain(traceMethodMap.get(methodIdMap.get(methodId)));
                    callChainIdMap.put(methodId, callChain);
                }
                if(lastCallChain!=null) {
                    callChain.addCallee(lastCallChain);
                }
                lastCallChain = callChain;
                callchainId = rs0.getInt(2);
            } while(callchainId>0);
            nativeCallChainRootSet.add(lastCallChain);
        }



    }

    public LinkedHashSet<CallChain> buildDynamicCallChains() throws SQLException {
        LinkedHashSet<CallChain> nativeCallChainRootSet = null;
        ResultSet rs;
        int testcaseId = -1;
        rs = DB.getStmt().executeQuery("select id from testcase where app_no=" + appNo + " order by id desc limit 1");
        if(rs.next()) {
            testcaseId = rs.getInt(1);
        }

        if(testcaseId>0) {
            rs = DB.getStmt().executeQuery("select method_id, testcase_id, num_of_invocation, elapsed_time from tracemethod where testcase_id=" + testcaseId);
            while(rs.next()) {
                int methodId = rs.getInt(1);
                SootMethod sm = methodIdMap.get(methodId);
                if(sm==null) {
                    ResultSet rs0 = DB.getStmt().executeQuery("select class.name, method.signature, method.accessflags from method, class where method.id=" + methodId + " and method.class_id = class.id");
                    if(rs0.next()) {
                        String className = rs0.getString(1);
                        String methodSig = rs0.getString(2);
                        SootClass sc = Scene.v().loadClassAndSupport(className);
                        if(!sc.isPhantom()) {
                            String[] methodInfo = Misc.parseMethodInfo(methodSig);
                            List<Type> paramTypes = Misc.getTypeList(methodInfo[1]);
                            Type returnType = Misc.getType(methodInfo[2]);
                            sm = sc.getMethod(methodInfo[0], paramTypes, returnType);
                            methodIdMap.put(methodId, sm);
                        }
                    }
                }

                if(sm!=null) {
                    TraceMethod tm = traceMethodMap.get(sm);
                    if (tm == null) {
                        tm = new TraceMethod(sm, methodId, rs.getInt(2), rs.getInt(3), rs.getLong(4));
                        traceMethodMap.put(sm, tm);
                    }
                }
            }

            // add child
            rs = DB.getStmt().executeQuery("select method_id, child_id, num_of_invocation from tracemethod_child where testcase_id=" + testcaseId + " order by method_id");
            while(rs.next()) {
                int methodId = rs.getInt(1);
                int childId = rs.getInt(2);
                int numOfInvocation = rs.getInt(3);
                TraceMethod traceMethod = traceMethodMap.get(methodIdMap.get(methodId));
                TraceMethod childMethod = traceMethodMap.get(methodIdMap.get(childId));
                traceMethod.addChild(childMethod, numOfInvocation);
            }

            Map<Integer, CallChain> callChainIdMap = new HashMap<Integer, CallChain>();
            nativeCallChainRootSet = new LinkedHashSet<CallChain>();
            rs = DB.getStmt().executeQuery("select min(methodcall.id) from methodcall, method, class where methodcall.testcase_id=" + testcaseId + " and methodcall.method_id=method.id and class.id=method.class_id and class.app_no=" + appNo + " and (method.accessflags&256)==256 group by method_id order by methodcall.id");
            while(rs.next()) {
                int callchainId = rs.getInt(1);
                ResultSet rs0;
                CallChain lastCallChain = null;
                do {
                    rs0 = DB.getStmt().executeQuery("select method_id, caller_id from methodcall where testcase_id=" + testcaseId + " and id=" + callchainId);
                    int methodId = rs0.getInt(1);
                    CallChain callChain = callChainIdMap.get(methodId);
                    if(callChain==null) {
                        callChain = new CallChain(traceMethodMap.get(methodIdMap.get(methodId)));
                        callChainIdMap.put(methodId, callChain);
                    }
                    if(lastCallChain!=null) {
                        callChain.addCallee(lastCallChain);
                    }
                    lastCallChain = callChain;
                    callchainId = rs0.getInt(2);
                } while(callchainId>0);
                nativeCallChainRootSet.add(lastCallChain);
            }
        }
        return nativeCallChainRootSet;
    }

    public CallGraph getCallGraph(File dump) throws Exception {
        if(!Scene.v().hasCallGraph()) {
            LinkedHashSet<CallChain> nativeCallChainRootSet = null;

            SigNumberedString.reset();
            Options.v().set_process_dir(Collections.singletonList(filePath));
            Options.v().set_full_resolver(true);
            Options.v().set_no_output_inner_classes_attribute(true);
            Options.v().set_output_dir("."); // do not create a output directory
            Options.v().set_prepend_classpath(true);
            Options.v().set_keep_line_number(true);
            Options.v().set_verbose(false);
            Options.v().set_whole_program(true);
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_allow_phantom_refs(true);
            Options.v().set_force_android_jar("./libs/android.jar"); // for android

            if(dump!=null) {
                nativeCallChainRootSet = buildDynamicCallChains();
            } else {
                Scene.v().loadNecessaryClasses();
            }

            setupDummyMainMethod(androidManifest); // create a dummy main method
            for (SootClass sc : Scene.v().getApplicationClasses()) { //packageClassSet) { // find receivers then add them to the body
                for (SootMethod sm : sc.getMethods()) {
                    if (sm.isConcrete()) {
                        findReceivers(sm);
                    } else if (sm.isNative()) {
                        numOfNativeMethods++;
                    }

                    int x = 10;
                    x++;
                }
            }

            Scene.v().loadDynamicClasses();
            //Scene.v().loadNecessaryClasses();
            /*
            java.lang.RuntimeException: This operation requires resolving level SIGNATURES but android.support.v8.renderscript.RenderScript is at resolving level DANGLING
If you are extending Soot, try to add the following call before calling soot.Main.main(..):
Scene.v().addBasicClass(android.support.v8.renderscript.RenderScript,SIGNATURES);
Otherwise, try whole-program mode (-w).
	at soot.SootClass.checkLevelIgnoreResolving(SootClass.java:151)
             */
            Scene.v().getSootClass("android.support.v8.renderscript.RenderScript");
            Scene.v().getSootClass("java.io.FileSystem"); // to prevent an error, java.util.ConcurrentModificationException, occurred in MethodPAG.java

            System.out.println("application " + Scene.v().getApplicationClasses().size());


            if(dump!=null) {
                new RuntimeException("dynamic callgraph builder is not implemented yet");
            } else {
                buildCallGraph();
            }


//            if (numOfNativeMethods > 0 && targetCPU != -1 && secondTargetCPU != -1) {
//                if (targetCPU > 0) {
//                    if (targetCPU == CPU_TYPE_X86) {
//                        throw new RuntimeException("X86 architecture is not supported");
//                    }
//                    List<LibraryModule> modules = getModules(targetCPU);
//                    if (modules.size() == 0) {
//                        modules = getModules(secondTargetCPU);
//                    }
//
//                    NativeLibraryHandler handler = new NativeLibraryHandler(modules, targetCPU);
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
//
//            } else {

   //         }


        }

        return Scene.v().getCallGraph();
    }

//    public static String getLibraryPath(int cpuType) {
//        String libDir;
//        switch (cpuType) {
//            case CPU_TYPE_ARM:
//                targetMachine = 40;
//                libDir = ARMEABI;
//                break;
//            case CPU_TYPE_ARM_V7A:
//
//                libDir = ARMEABI_V7A;
//                break;
//            default:
//                targetMachine = 3;
//                throw new RuntimeException("unsupported cpu type:" + cpuType);
//        }
//        return libDir;
//
//    }

    public int getNumOfNativeMethods() {
        return numOfNativeMethods;
    }

    public AndroidManifest getAndroidManifest() {
        return androidManifest;
    }

    public List<LibraryModule> getModules(short targetCPU) {
        if (modules == null || modules.size() == 0) {
            try {
                Set<String> libNameSet = new HashSet<String>();
                modules = new LinkedList<LibraryModule>();
                Multimap<String, LibraryModule> allModules = HashMultimap.create(); // for extra modules
                ZipFile zipFile = new ZipFile(filePath);
                for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String entryName = entry.getName();
                    if (entryName.endsWith(".so")) {
                        if (entry.getSize() > 0) {
                            String fileName = Misc.getFileName(entry.getName());
                            libNameSet.add(fileName);
                            File moduleFile = File.createTempFile("cosa", ".so");
                            InputStream in = zipFile.getInputStream(entry);
                            OutputStream out = new BufferedOutputStream(new FileOutputStream(moduleFile));
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) >= 0) {
                                out.write(buffer, 0, len);
                            }
                            in.close();
                            out.close();


                            LibraryModule module = new LibraryModule(packageName, moduleFile.getAbsolutePath(), false); // do not init now
                            String moduleName = module.getModuleName();
                            if (module.getModuleName() == null) {
                                moduleName = fileName.substring(3, fileName.indexOf("."));
                                int pos = moduleName.indexOf("_");
                                if(pos>0) {
                                    moduleName = moduleName.substring(0,pos);
                                }
                                module.setModuleName(moduleName);
                            }

                            short machine = module.getMachine();
                            switch(targetCPU) {
                                case CPU_TYPE_X86:
                                    if(machine==CPU_TYPE_X86) {
                                        modules.add(module);
                                    }
                                    break;
                                case CPU_TYPE_MIPS:
                                    if(machine==CPU_TYPE_MIPS) {
                                        modules.add(module);
                                    }
                                    break;
                                default: // for ARM
                                    if(machine!=CPU_TYPE_X86 && machine!=CPU_TYPE_MIPS) {
                                        allModules.put(moduleName, module);
                                    }
                            }
                        }
                    }
                }

                for(String moduleName:allModules.keySet()) {
                    Collection<LibraryModule> moduleSet = allModules.get(moduleName);
                    if(moduleSet.size()==1) {
                        modules.addAll(moduleSet);
                    } else {
                        // filter target modules
                        LibraryModule armModule = null;
                        boolean found = false;
                        for(LibraryModule module:moduleSet) {
                            if(targetCPU==module.getARMCPUArch()) {
                                modules.add(module);
                                found = true;
                                break;
                            } else if(module.getARMCPUArch()==CPU_TYPE_ARM) {
                                armModule = module;
                            }
                        }

                        if(!found && armModule!=null) { // it a module for the target cpu is not exist, use an arm module instead of it.
                            modules.add(armModule);
                        }

                    }
                }

                // init modules
                for(LibraryModule module:modules) {
                    module.init();
                }


     /*

                String libDir = getLibraryPath(targetCPU);
                ZipFile zipFile = new ZipFile(filePath);
                for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                    ZipEntry entry = (ZipEntry) e.nextElement();
                    String entryName = entry.getName();
                    if (entryName.endsWith("so") && (entryName.startsWith(libDir) || entryName.startsWith(ASSETS_DIR) || entryName.startsWith(RES_DIR))) {
                       if (entry.getSize() > 0) {
                            String fileName = Misc.getFileName(entry.getName());
                            libNameSet.add(fileName);
                            File moduleFile = File.createTempFile("cosa", ".so");
                            InputStream in = zipFile.getInputStream(entry);
                            OutputStream out = new BufferedOutputStream(new FileOutputStream(moduleFile));
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) >= 0) {
                                out.write(buffer, 0, len);
                            }
                            in.close();
                            out.close();

                            if(entryName.startsWith(libDir)) {
                                LibraryModule module = new LibraryModule(packageName, moduleFile.getAbsolutePath());
                                if (module.getModuleName() == null) {
                                    module.setModuleName(fileName.substring(3, fileName.length() - 3));
                                }
                                modules.add(module);
                            } else {
                                ExtraLibrary module = new ExtraLibrary(packageName, moduleFile.getAbsolutePath());
                                String moduleName = module.getModuleName();
                                if (module.getModuleName() == null) {
                                    moduleName = fileName.substring(3, fileName.length() - 3);
                                    module.setModuleName(moduleName);
                                }
                                extraModules.put(moduleName, module);
                            }
                        }
                    }
                }

                */

//                if(modules.size()>0) {
//                    // add extra library that doesn't include the package file
//                    File extDirect = new File(filePath.substring(0,filePath.lastIndexOf("/"))+"/ext/"+packageName+"/"+libDir);
//                    if(extDirect.isDirectory()) {
//                        for(File file:extDirect.listFiles()) {
//                            if(!libNameSet.contains(file.getName()) && file.getName().endsWith(".so")) {
//                                LibraryModule module = new LibraryModule(packageName, file.getAbsolutePath());
//                                modules.add(module);
//                            }
//                        }
//                    }
//                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error when looking for manifest in apk: " + e);
            }
        }
        return modules;
    }

    public void parseLayoutXML(InputStream is) {
        try {
            byte[] data = IOUtils.toByteArray(is);
            AxmlReader ar = new AxmlReader(data);
            ar.accept(new Visitor());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error when looking into layout files in apk: " + e);
        }
    }

    private class Visitor extends AxmlVisitor {
        LayoutResource resource;

        public Visitor() {

        }

        public Visitor(String nodeName, LayoutResource parent) {
            this.resource = new LayoutResource(nodeName, parent);
        }

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            resource.put(name, obj);
            super.attr(ns, name, resourceId, type, obj);
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            return new Visitor(name, resource);
        }

        @Override
        public void end() {
            super.end();
            Object id = resource.get("id");
            if (id != null) {
                setId((Integer) id, resource);
            }
            Object tag = resource.get("tag");
            if (tag != null) {
                setTag(tag.toString(), resource);
            }
        }
    }


    public static void addExtraLibrary(String packageName, short cpuType) throws IOException {
        throw new RuntimeException("N/A");
//        File file = new File(extraLibDir + packageName + "/" + AndroidPackage.getLibraryPath(cpuType));
//        if (!file.isDirectory()) {
//            throw new RuntimeException("The package named '" + packageName + "' is not exist");
//        }
//
//        for (File dir : file.listFiles()) {
//            if (dir.toString().endsWith(".so")) {
//                LibraryModule libraryModule = new LibraryModule(packageName, dir.toString());
//                for (ElfSymbol symbol : libraryModule.getElfHelper().getDynamicSymbols()) {
//                    String name = symbol.toString();
//                    if (symbol.size() > 0 && name.startsWith("Java_")) {
//                        String names[] = Misc.javaDemingling(name);
//                        try {
//                            PreparedStatement pstmt = DB.getPStmt("insert into ext_method(packageName, fileName, className, methodName) values(?,?,?,?)");
//                            pstmt.setString(1, packageName);
//                            pstmt.setString(2, dir.getName());
//                            pstmt.setString(3, names[0]);
//                            pstmt.setString(4, names[1]);
//                            pstmt.execute();
//                            pstmt.close();
//                        } catch (SQLException e1) {
//                            e1.printStackTrace();
//                        }
//                        System.out.println(names[0] + " " + names[1]);
//                    }
//                }
//            }
//        }
//        System.out.println("done");

    }

    public static boolean isTrigger(SootMethod sm) {
        return sm.hasTag(TAG_TRIGGER);
    }

    public static boolean isReceiver(SootMethod sm) {
        return sm.hasTag(TAG_RECEIVER);
    }

    public static boolean isCallback(SootMethod sm) {
        return sm.hasTag(TAG_CALLBACK);
    }

    public static void setTrigger(SootMethod sm) {
        sm.addTag(new CodeAttribute(TAG_TRIGGER));
    }

    public static void setReceiver(SootMethod sm) {
        sm.addTag(new CodeAttribute(TAG_RECEIVER));
    }

    public static void setCallback(SootMethod sm) {
        sm.addTag(new CodeAttribute(TAG_CALLBACK));
    }

//    public static LibraryModule retrieveExternalModule(String className, String methodName, int cpuType) {
//        try {
//            ResultSet rs = DB.getStmt().executeQuery("select package, file from ext_library where class_name ='" + className + "' and method_name='" + methodName + "'");
//            if (rs.next()) {
//                String packageName = rs.getString(1);
//                String fileName = rs.getString(2);
//                logger.info("An external library named {} from a package, '{}' has been found", fileName, packageName);
//                LibraryModule module = new LibraryModule(packageName, extraLibDir + packageName + "/" + AndroidPackage.getLibraryPath(cpuType) + "/" + fileName);
//                module.setExternal();
//                return module;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


    public void doAnalysis(short targetCPU, short secondTargetCPU) throws Exception {
        setupDummyMainMethod(getAndroidManifest()); // create a dummy main method

        if (targetCPU > 0) {
            if (targetCPU == CPU_TYPE_X86) {
                throw new RuntimeException("X86 architecture is not supported");
            }


            if (getNumOfNativeMethods() > 0) {
                List<LibraryModule> modules = getModules(targetCPU);
                if (modules.size() == 0) {
                    modules = getModules(secondTargetCPU);
                }

                NativeLibraryHandler nativeAnalyzer = NativeLibraryHandler.getInstance();
                nativeAnalyzer.init(modules, targetCPU);


                System.out.println("Checking native methods....");
//            do {
//                buildCallGraph();
//            } while(nativeAnalyzer.traverse(Scene.v().getCallGraph(), Scene.v().getMainMethod())>0);
               // buildCallGraph();


                List<Subroutine> nativeMethods = nativeAnalyzer.traverse(Scene.v().getCallGraph(), Scene.v().getMainMethod());
//                for (NativeMethod nativeMethod : nativeMethods) {
//                    System.out.println(nativeMethod.getBody());
//                }
            }
        }

        if (!Scene.v().hasCallGraph()) {
        //    buildCallGraph();
        }
    }


//    private Map<Integer, Resource> resourceMap = new HashMap<Integer, Resource>();

    private void setupDummyMainMethod(AndroidManifest manifest) {
        SootClass sc = new SootClass(manifest.getPackageName() + "dummyMain");



        SootMethod mainMethod = new SootMethod("main", Collections.<Type>singletonList(ArrayType.v(RefType.v("java.lang.String"), 1)), VoidType.v(), Modifier.STATIC | Modifier.PUBLIC);
        sc.addMethod(mainMethod);
        dummyMethodSet.add(mainMethod);

        JimpleBody body = Jimple.v().newBody(mainMethod);
        mainMethod.setActiveBody(body);

        Chain units = body.getUnits();
        Chain locals = body.getLocals();

        ParameterRef paramRef = Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.String"), 1), 0);
        Local paramLocal = Jimple.v().newLocal("$r0", ArrayType.v(RefType.v("java.lang.String"), 1));
        locals.add(paramLocal);
        units.add(Jimple.v().newIdentityStmt(paramLocal, paramRef));

        for (Component component : manifest.getComponents()) {
            SootMethod method = component.getEntryPoint(this);
            if (method != null && !sc.declaresMethod(method.getNumberedSubSignature())) {
                sc.addMethod(method);
                dummyMethodSet.add(method);
                setReceiver(method);
                units.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(method.makeRef())));

            }
        }

        units.add(Jimple.v().newReturnVoidStmt());

        Scene.v().addClass(sc);
        sc.setApplicationClass();
        Scene.v().setMainClass(sc);
    }

    public void setId(Integer id, LayoutResource resource) {
        idMap.put(id, resource);
    }

    public void setTag(String tag, LayoutResource resource) {
        tagMap.put(tag, resource);
    }

    private boolean initReceivers() {
        if (receiverMap != null) {
            return true;
        }

        try {
            receiverMap = LinkedHashMultimap.create();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(this.getClass().getResourceAsStream("/Receivers.xml"));
            NodeList triggers = doc.getElementsByTagName("trigger");
            for (int i = 0; i < triggers.getLength(); i++) {
                Element trigger = (Element) triggers.item(i);
                NumberedString sigTrigger = Scene.v().getSubSigNumberer().findOrAdd(trigger.getAttribute("name"));
                NodeList receiverNodeList = trigger.getElementsByTagName("receiver");
                for (int j = 0; j < receiverNodeList.getLength(); j++) {
                    int group = -1;
                    Element receiverNode = (Element) receiverNodeList.item(j);
                    boolean startWith = "true".equals(receiverNode.getAttribute("startWith"));


                    NumberedString sigReceiver = null;
                    if (receiverNode.hasAttribute("name")) {
                        sigReceiver = Scene.v().getSubSigNumberer().findOrAdd(receiverNode.getAttribute("name"));
                    }

                    if (receiverNode.hasAttribute("group")) {
                        group = Integer.parseInt(receiverNode.getAttribute("group"));
                    }

                    String replace = null;
                    if (receiverNode.hasAttribute("replace")) {
                        replace = receiverNode.getAttribute("replace");
                    }

                    boolean store = false;
                    boolean fragment = false;
                    boolean iface = false;

                    Kind kind = null;
                    Parameter base = null;
                    List<Parameter> parameters = new ArrayList<Parameter>();
                    String attr = receiverNode.getAttribute("kind");
                    if ("VIRTUAL".equals(attr)) {
                        kind = Kind.VIRTUAL;
                    } else if ("INTERFACE".equals(attr)) {
                        kind = Kind.INTERFACE;
                    } else if ("SPECIAL".equals(attr)) {
                        kind = Kind.SPECIAL;
                    } else if ("THREAD".equals(attr)) {
                        kind = Kind.THREAD;
                    } else if ("EXECUTOR".equals(attr)) {
                        kind = Kind.EXECUTOR;
                    } else if ("STORE".equals(attr)) {
                        store = true;
                    } else if ("FRAGMENT".equals(attr)) {
                        fragment = true;
                    } else {
                        throw new RuntimeException("Unknown Kind :" + attr);
                    }

                    // ignorable
                    NodeList baseNodeList = receiverNode.getElementsByTagName("base");
                    if (baseNodeList.getLength() > 0) {
                        Element baseNode = (Element) baseNodeList.item(0);
                        boolean ignoreable = "true".equals(baseNode.getAttribute("ignoreable"));
                        Parameter.Type source = Parameter.Type.valueOf(baseNode.getAttribute("source"));
                        String srcIndex = baseNode.getAttribute("srcIndex");
                        String index = baseNode.getAttribute("index");
                        if (index.isEmpty()) {
                            base = new Parameter(source, Parameter.UNKNOWN, Parameter.parseInt(srcIndex), ignoreable);
                        } else {
                            base = new Parameter(source, Parameter.parseInt(index), Parameter.parseInt(srcIndex), ignoreable);
                        }
                    }

                    NodeList paramNodeList = receiverNode.getElementsByTagName("parameter");
                    for (int k = 0; k < paramNodeList.getLength(); k++) {
                        Element paramNode = (Element) paramNodeList.item(0);
                        boolean ignoreable = "true".equals(paramNode.getAttribute("ignoreable"));

                        String index = paramNode.getAttribute("index");


                        Parameter.Type source = Parameter.Type.valueOf(paramNode.getAttribute("source"));
                        String srcIndex = paramNode.getAttribute("srcIndex");


                        parameters.add(new Parameter(source, Parameter.parseInt(index), Parameter.parseInt(srcIndex), ignoreable));
                    }

                    if (kind == Kind.EXECUTOR) {
                        executorMap.put(sigTrigger, replace);
                    } else {
                        receiverMap.put(sigTrigger, new Receiver(sigReceiver, base, parameters, kind, startWith, group, store, fragment));
                    }
                }
            }

            listenerOrCallBackMap = new HashMap<RefType, ListenerOrCallBack>();
            NodeList listenerNodeList = doc.getElementsByTagName("listener");
            for (int i = 0; i < listenerNodeList.getLength(); i++) {
                Element listenerNode = (Element) listenerNodeList.item(i);
                RefType listenerType = RefType.v(listenerNode.getAttribute("name"));
                NumberedString sigTrigger = null;
                Parameter base = null;

                NodeList triggerNodeList = listenerNode.getElementsByTagName("trigger");
                if (triggerNodeList.getLength() > 0) {
                    Element triggerNode = (Element) triggerNodeList.item(0);
                    if (triggerNode.hasAttribute("name")) {
                        sigTrigger = Scene.v().getSubSigNumberer().findOrAdd(triggerNode.getAttribute("name"));
                    }
                }

                NodeList baseNodeList = listenerNode.getElementsByTagName("base");
                if (baseNodeList.getLength() > 0) {
                    Element baseNode = (Element) baseNodeList.item(0);
                    boolean ignoreable = "true".equals(baseNode.getAttribute("ignoreable"));
                    Parameter.Type source = Parameter.Type.valueOf(baseNode.getAttribute("source"));
                    String srcIndex = baseNode.getAttribute("srcIndex");
                    String index = baseNode.getAttribute("index");
                    if (index.isEmpty()) {
                        base = new Parameter(source, Parameter.UNKNOWN, Parameter.parseInt(srcIndex), ignoreable);
                    } else {
                        base = new Parameter(source, Parameter.parseInt(index), Parameter.parseInt(srcIndex), ignoreable);
                    }
                }

                listenerOrCallBackMap.put(listenerType, new ListenerOrCallBack(true, sigTrigger, base, listenerType));
            }


            NodeList callbackNodeList = doc.getElementsByTagName("callback");
            for (int i = 0; i < callbackNodeList.getLength(); i++) {
                Element callbackNode = (Element) callbackNodeList.item(i);
                RefType callbackType = RefType.v(callbackNode.getAttribute("name"));
                NumberedString sigTrigger = null;
                Parameter base = null;

                NodeList triggerNodeList = callbackNode.getElementsByTagName("trigger");
                if (triggerNodeList.getLength() > 0) {
                    Element triggerNode = (Element) triggerNodeList.item(0);
                    if (triggerNode.hasAttribute("name")) {
                        sigTrigger = Scene.v().getSubSigNumberer().findOrAdd(triggerNode.getAttribute("name"));
                    }
                }

                NodeList baseNodeList = callbackNode.getElementsByTagName("base");
                if (baseNodeList.getLength() > 0) {
                    Element baseNode = (Element) baseNodeList.item(0);
                    boolean ignoreable = "true".equals(baseNode.getAttribute("ignoreable"));
                    Parameter.Type source = Parameter.Type.valueOf(baseNode.getAttribute("source"));
                    String srcIndex = baseNode.getAttribute("srcIndex");
                    String index = baseNode.getAttribute("index");
                    if (index.isEmpty()) {
                        base = new Parameter(source, Parameter.UNKNOWN, Parameter.parseInt(srcIndex), ignoreable);
                    } else {
                        base = new Parameter(source, Parameter.parseInt(index), Parameter.parseInt(srcIndex), ignoreable);
                    }
                }
                listenerOrCallBackMap.put(callbackType, new ListenerOrCallBack(false, sigTrigger, base, callbackType));
            }


            return true;
        } catch (Exception e) {
            e.printStackTrace();
            G.v().out.println("Warning: Receivers.txt is not exist. The default method to find receivers will be used");
        }

        return false;
    }

    private Unit addVirtualCallSite(Stmt s, InstanceInvokeExpr iie, SootMethod m, Value oldRecv, Value receiver, SootMethod method, Kind kind, Receiver r, Body b, Map<Type, Value> localValue, Unit lastInsertedUnit) {
        List<Value> args = new ArrayList<Value>();        // handle arguments
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (method.getParameterType(i) instanceof PrimType) {
                args.add(IntConstant.v(0));
            } else {
                args.add(NullConstant.v());
            }
        }

        InvokeExpr invoke;

        if (kind == Kind.SPECIAL) {
            invoke = Jimple.v().newSpecialInvokeExpr((Local) receiver, method.makeRef(), args);
        } else if (kind == Kind.INTERFACE || method.getDeclaringClass().isInterface()) { // methodRef.declaringClass().isInterface() method.isAbstract()) {
            invoke = Jimple.v().newInterfaceInvokeExpr((Local) receiver, method.makeRef(), args);
        } else {
            invoke = Jimple.v().newVirtualInvokeExpr((Local) receiver, method.makeRef(), args);
        }

        if (r != null) { // it the receiver is not null, collect parameters
            for (Parameter p : r.params) {
                Value v = null;
                switch (p.source) {
                    case TRIGGER:
                        if (p.srcIndex == Parameter.LEFT) {
                            v = ((AssignStmt) s).getLeftOp();
                        } else if (p.srcIndex == Parameter.BASE) {
                            v = oldRecv;
                        } else if (p.srcIndex > 0) {
                            v = iie.getArg(p.srcIndex);
                        }
                        break;
                    case THIS:
                        v = b.getThisLocal();
                        break;
                }
                if (v != null) {
                    invoke.setArg(p.index, v);
                }
            }
        }

        for (int i = 0; i < args.size(); i++) {
            Type paramType = method.getParameterType(i);
            if (args.get(i) == NullConstant.v()) {
                Value v = null;
                if (localValue != null) {
                    v = localValue.get(paramType);
                }
                if (v == null) {
                    v = Jimple.v().newLocal("$r" + (b.getLocalCount() + 1), paramType);
                    b.getLocals().addFirst((Local) v);

                    Value rvalue;
                    if (paramType instanceof RefType) {
                        rvalue = Jimple.v().newNewExpr((RefType) paramType);
                    } else if (paramType instanceof ArrayType) {
                        if (((ArrayType) paramType).numDimensions == 1) {
                            rvalue = Jimple.v().newNewArrayExpr(((ArrayType) paramType).baseType, IntConstant.v(0));
                        } else {
                            List<Value> dims = new ArrayList<Value>();
                            for (int num = 0; num < ((ArrayType) paramType).numDimensions; num++) {
                                dims.add(IntConstant.v(0));
                            }
                            rvalue = Jimple.v().newNewMultiArrayExpr((ArrayType) paramType, dims);
                        }
                    } else {
                        throw new RuntimeException(method.getParameterType(i).toString());
                    }

                    Unit u = Jimple.v().newAssignStmt(v, rvalue);


                    b.getUnits().insertAfter(u, lastInsertedUnit);
                    lastInsertedUnit = u;
                    if (localValue != null) {
                        localValue.put(method.getParameterType(i), v);
                    }
                }
                invoke.setArg(i, v);
//                args.set(i, v);
            }
        }

    /*    for (int i = 0; i < args.size(); i++) {
            if (args.get(i) == NullConstant.v()) {
                Value v = null;
                if (localValue != null) {
                    v = localValue.get(method.getParameterType(i));
                }
                if (v == null) {
                    v = Jimple.v().newLocal("$r" + (b.getLocalCount()+1), method.getParameterType(i));
                    b.getLocals().addFirst((Local) v);
                    Unit u = Jimple.v().newAssignStmt(v, Jimple.v().newNewExpr(RefType.v(method.getParameterType(i).toString())));
                    b.getUnits().insertAfter(u, lastInsertedUnit);
//                    u = Jimple.v().newSpecialInvokeExpr(v, )
                    lastInsertedUnit = u;
                    if (localValue != null) {
                        localValue.put(method.getParameterType(i), v);
                    }
                }
                invoke.setArg(i, v);
            } else {
                invoke.setArg(i, args.get(i));
            }
        }
        */

        Unit u = Jimple.v().newInvokeStmt(invoke);

        b.getUnits().insertAfter(u, lastInsertedUnit);
        lastInsertedUnit = u;

        return lastInsertedUnit;
    }

    // compare two types
    private boolean hasAncestorOrSelf(Type source, Type target) {
        // both types should be the same
        if (source.equals(target)) {
            return true;
        }

        if (source instanceof ArrayType && target instanceof ArrayType) {
            if (((ArrayType) source).numDimensions != ((ArrayType) target).numDimensions) {
                return false;
            }
            source = ((ArrayType) source).baseType;
            target = ((ArrayType) target).baseType;
        }

        if (source instanceof RefType && target instanceof RefType) {
            return hasAncestorOrSelf(((RefType) source).getSootClass(), ((RefType) target).getSootClass());

        }
        return false;
    }

    private boolean hasAncestorOrSelf(SootClass source, SootClass target) {
        //  check interface or super class
        if (!target.isPhantom() && !source.isPhantom()) {
            if (target == source) {
                return true;
            }

            if (target.isInterface()) {
                for (SootClass iface : source.getInterfaces()) {
                    if (iface == target) {
                        return true;
                    }
                    if (iface.hasSuperclass()) {
                        if (hasAncestorOrSelf(iface.getSuperclass(), target)) {
                            return true;
                        }
                    }
                }
            }

            if (source.hasSuperclass()) { // check super class
                return hasAncestorOrSelf(source.getSuperclass(), target);
            }
        }
        return false;
    }

    private Unit addCallbackOrReceiver(Value oldRecv, Value receiver, SootMethod method, Body b, Map<Type, Value> localValue, Unit lastInsertedUnit) {
        List<Value> args = new ArrayList<Value>();        // handle arguments
        for (int i = 0; i < method.getParameterCount(); i++) {
            if (method.getParameterType(i) instanceof PrimType) {
                args.add(IntConstant.v(0));
            } else {
                args.add(NullConstant.v());
            }
        }

        for (int i = 0; i < method.getParameterCount(); i++) {
            if (args.get(i) == NullConstant.v()) {
                Type paramType = method.getParameterType(i);


                if (hasAncestorOrSelf(oldRecv.getType(), paramType)) {
                    args.set(i, oldRecv);
                } else {
                    // find localvalue........ and local variables....
                    Value v = null;
                    if (localValue != null) {
                        v = localValue.get(paramType);
                    }
                    if (v == null) {
                        v = Jimple.v().newLocal("$r" + (b.getLocalCount() + 1), paramType);
                        b.getLocals().addFirst((Local) v);

                        Value rvalue;
                        if (paramType instanceof RefType) {
                            rvalue = Jimple.v().newNewExpr((RefType) paramType);
                        } else if (paramType instanceof ArrayType) {
                            if (((ArrayType) paramType).numDimensions == 1) {
                                rvalue = Jimple.v().newNewArrayExpr(((ArrayType) paramType).baseType, IntConstant.v(0));
                            } else {
                                List<Value> dims = new ArrayList<Value>();
                                for (int num = 0; num < ((ArrayType) paramType).numDimensions; num++) {
                                    dims.add(IntConstant.v(0));
                                }
                                rvalue = Jimple.v().newNewMultiArrayExpr((ArrayType) paramType, dims);
                            }
                        } else {
                            throw new RuntimeException(method.getParameterType(i).toString());
                        }

                        Unit u = Jimple.v().newAssignStmt(v, rvalue);

                        b.getUnits().insertAfter(u, lastInsertedUnit);
                        lastInsertedUnit = u;
                    }
                    args.set(i, v);
                }
            }
        }

        InvokeExpr invoke;

        if (method.getDeclaringClass().isInterface()) { // methodRef.declaringClass().isInterface() method.isAbstract()) {
            invoke = Jimple.v().newInterfaceInvokeExpr((Local) receiver, method.makeRef(), args);
        } else if(method.isStatic()) {
            invoke = Jimple.v().newStaticInvokeExpr(method.makeRef(), args);
        } else {
            invoke = Jimple.v().newVirtualInvokeExpr((Local) receiver, method.makeRef(), args);
        }

        Unit u = Jimple.v().newInvokeStmt(invoke);
        b.getUnits().insertAfter(u, lastInsertedUnit);
        lastInsertedUnit = u;

        return lastInsertedUnit;
    }

    // get methods within interfaces included in the Android Framework (not in APK)
    private Set<SootMethod> getInterfaceMethods(SootClass sc) {
        Set<SootMethod> interfaceMethodSet = interfaceMethodMap.get(sc);
        if (interfaceMethodSet == null) {
            interfaceMethodSet = new HashSet<SootMethod>();
            interfaceMethodMap.put(sc, interfaceMethodSet);
            if (!sc.isApplicationClass()) {
                interfaceMethodSet.addAll(sc.getMethods());
            }

            for (SootClass iface : sc.getInterfaces()) {
                interfaceMethodSet.addAll(getInterfaceMethods(iface));
            }
        }
        return interfaceMethodSet;
    }

    private Set<NumberedString> getInheritableMethodSet(SootClass sc) {
        Set<NumberedString> inheritableMethodSet = inheritableMethodMap.get(sc);
        if (inheritableMethodSet == null) {
            inheritableMethodSet = new HashSet<NumberedString>();
            inheritableMethodMap.put(sc, inheritableMethodSet);

            if (!sc.isApplicationClass()) {
                for (SootMethod sm : sc.getMethods()) {
                    if (!sm.isConstructor() && !sm.isPrivate() && !sm.isFinal() && !sm.isStatic()) {
                        inheritableMethodSet.add(sm.getNumberedSubSignature());
                    }
                }
            }

            if (sc.hasSuperclass()) {
                inheritableMethodSet.addAll(getInheritableMethodSet(sc.getSuperclass()));
            }
        }
        return inheritableMethodSet;
    }

    private Map<NumberedString, SootMethod> getOverriddenMethodMap(SootClass sc) {
        Map<NumberedString, SootMethod> overriddenMethodMap = overriddenMethodMaps.get(sc);
        if (overriddenMethodMap == null) {
            overriddenMethodMap = new HashMap<NumberedString, SootMethod>();
            overriddenMethodMaps.put(sc, overriddenMethodMap);

            if (sc.isApplicationClass()) {
                if (sc.hasSuperclass()) {
                    overriddenMethodMap.putAll(getOverriddenMethodMap(sc.getSuperclass()));
                }

                for (SootClass iface : sc.getInterfaces()) {
                    for (SootMethod sm : getInterfaceMethods(iface)) {
                        NumberedString subSig = sm.getNumberedSubSignature();
                        overriddenMethodMap.put(subSig, sm);
                    }
                }

                for (NumberedString subSig : getInheritableMethodSet(sc)) {
                    SootMethod sm = sc.getMethodUnsafe(subSig);
                    if (sm != null) {
                        //   setCallback(sm);
                        overriddenMethodMap.put(subSig, sm);
                    }
                }
            }
        }
        return overriddenMethodMap;
    }

    public Set<SootMethod> getOverriddenMethodsStartsWith(SootClass sc, String name) {
        if (name == null) {
            throw new RuntimeException("name is null");
        }

        name = name.trim();
        if (name.isEmpty()) {
            throw new RuntimeException("name is empty");
        }
        Set<SootMethod> overriddenMethods = overriddenMethodsStartsWithMap.get(sc, name);
        if (overriddenMethods == null) {
            overriddenMethods = new HashSet<SootMethod>();
            overriddenMethodsStartsWithMap.put(sc, name, overriddenMethods);
            for (SootMethod sm : getOverriddenMethodMap(sc).values()) {
                if (sm.getName().startsWith(name)) {
                    overriddenMethods.add(sm);
                }
            }
        }
        return new HashSet(overriddenMethods);
    }

    private void findReceivers(SootMethod m) {
        Body b = m.retrieveActiveBody();

        Map<Integer, Value> localIdValue = new HashMap<Integer, Value>();
        Map<String, Value> localTagValue = new HashMap<String, Value>();
        Iterator<Unit> it = b.getUnits().snapshotIterator();
        while (it.hasNext()) {
            final Stmt s = (Stmt) it.next();
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                if (ie instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
                    Value receiver = iie.getBase();

                    NumberedString subSig =
                            iie.getMethodRef().getSubSignature();

                    if (subSig == SigNumberedString.v().sigStart && hasAncestorOrSelf(iie.getMethod().getDeclaringClass(), Scene.v().getSootClass("java.lang.Thread"))) {
                        // check type of base class
                        // Thread start
                        setTrigger(m);
                        if (iie.getMethod().toString().equals("<java.lang.Thread: void start()>")) {
                            Value v = receiverValueTable.remove(receiver, 0); // ((JimpleLocal) receiver).getValue(0);
                            if (v != null) {
                                iie.setBase(v);
                            }
                        }
                        // get base and set root
                        Type t = iie.getBase().getType(); // TODO: add sigRun method to Trigger
//                        if (t instanceof RefType) {
//                            System.out.println(t);
//                            setTrigger(getOverriddenMethodMap(((RefType) t).getSootClass()).get(SigNumberedString.v().sigRun));
//                        }
                    } else if (subSig == SigNumberedString.v().sigExecutorExecute) {
                        setTrigger(m);
                    } else if (subSig == SigNumberedString.v().sigExecute) {
                        setTrigger(m); // it can be handled by Soot
                    } else if (executorMap.containsKey(subSig)) {
                        if (iie.getArgCount() > 0) {
                            Value runnable = iie.getArg(0);
                            if (runnable instanceof Local) {
                                setTrigger(m);
                                // getReplacement statement
                                b.getUnits().swapWith(s, Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr((Local) runnable, Scene.v().getMethod(executorMap.get(subSig)).makeRef())));//,s);
                            }
                        }
                    } else {
                        Value oldRecv = receiver;
                        Collection<Receiver> receivers = receiverMap.get(subSig);

                        Unit lastInsertedUnit = s;
                        if (receivers.size() > 0) {
                            Set<Integer> solvedGroupSet = new HashSet<Integer>();
                            for (Receiver recv : receivers) {
                                if (recv.group > 0 && solvedGroupSet.contains(recv.group)) {
                                    continue;
                                }

                                if (recv.isStore()) {
                                    for (Parameter param : recv.params) {
                                        if (param.srcIndex < 0) {
                                            throw new RuntimeException("not implemented for a special index of parameters");
                                        }
                                        receiverValueTable.put(receiver, param.index, iie.getArg(param.srcIndex));
//                                        ((JimpleLocal) receiver).putValue(param.index, iie.getArg(param.srcIndex));
                                    }
                                    continue;
                                }

                                Parameter base = recv.base;
                                if (base != null) {
                                    // set receiver
                                    switch (base.source) {
                                        case TRIGGER:
                                            if (base.srcIndex == Parameter.LEFT) {
                                                receiver = ((AssignStmt) s).getLeftOp();
                                            } else if (iie.getArgCount() > base.srcIndex) {
                                                receiver = iie.getArg(base.srcIndex);
                                            } else if (!base.ignoreable) {
                                                continue;
                                            }

                                            if (receiver instanceof NullConstant && !base.ignoreable) {
                                                continue;
                                            }
                                            break;
                                        case NEW: {
                                            LayoutResource resource = null;
                                            String className = null;
                                            int id = -1;
                                            String tag = null;
                                            if (base.index == Parameter.ID && (ie.getArg(base.srcIndex) instanceof IntConstant)) {
                                                id = ((IntConstant) ie.getArg(base.srcIndex)).value;
                                                resource = idMap.get(id);
                                            } else if (base.index == Parameter.TAG && (ie.getArg(base.srcIndex) instanceof StringConstant)) {
                                                tag = ((StringConstant) ie.getArg(base.srcIndex)).value;
                                                resource = tagMap.get(tag);
                                            }

                                            if (resource != null) {
                                                className = resource.getString("class");
                                                if (className == null) {
                                                    className = resource.getString("name");
                                                }
                                                if (className == null) {
                                                    className = resource.getNodeName();
                                                }

                                                if (className != null && Scene.v().containsClass(className)) {
                                                    SootClass sc = Scene.v().loadClassAndSupport(className);
                                                    if (!sc.isPhantom() && sc.declaresMethod(recv.sigRecv)) {
                                                        RefType scType = sc.getType(); // RefType.v(className);
                                                        receiver = null;
                                                        if (s instanceof AssignStmt) {
                                                            receiver = ((AssignStmt) s).getLeftOp(); // it will be cast......to find right one....
                                                            // find cast type
                                                            while ((lastInsertedUnit = b.getUnits().getSuccOf(lastInsertedUnit)) != null) {
                                                                if (lastInsertedUnit instanceof JAssignStmt) {
                                                                    Value rightOp = ((JAssignStmt) lastInsertedUnit).getRightOp();
                                                                    if (rightOp instanceof JCastExpr && ((JCastExpr) rightOp).getOp() == receiver) {
                                                                        receiver = ((JAssignStmt) lastInsertedUnit).getLeftOp();
                                                                        Unit u = Jimple.v().newAssignStmt(receiver, Jimple.v().newNewExpr(scType));
                                                                        b.getUnits().swapWith(lastInsertedUnit, u);
                                                                        lastInsertedUnit = u;
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (receiver == null || lastInsertedUnit == null) {
                                                            receiver = Jimple.v().newLocal("$r" + (b.getLocalCount() + 1), scType);
                                                            b.getLocals().addFirst((Local) receiver);
                                                            Unit u = Jimple.v().newAssignStmt(receiver, Jimple.v().newNewExpr(scType));
                                                            b.getUnits().insertAfter(u, s);
                                                            lastInsertedUnit = u;
                                                        }
                                                        lastInsertedUnit = addVirtualCallSite(s, iie, m, oldRecv, receiver, sc.getMethod(recv.sigRecv), recv.kind, recv, b, null, lastInsertedUnit);

                                                        if (!recv.isFragment()) {
                                                            if (id > 0) {
                                                                localIdValue.put(id, receiver);
                                                            } else {
                                                                localTagValue.put(tag, receiver);
                                                            }
                                                            solvedGroupSet.add(recv.group);
                                                        }
                                                    }

                                                    if (!recv.isFragment()) {
                                                        continue;
                                                    }
                                                }
                                            }
                                            break;
                                        }
                                        case STORAGE:
                                            if (base.index == Parameter.ID) {
                                                if ((ie.getArg(base.srcIndex) instanceof IntConstant)) {
                                                    int id = ((IntConstant) ie.getArg(base.srcIndex)).value;
                                                    receiver = localIdValue.get(id);
                                                    if (receiver == null) {
                                                        continue;
                                                    }
                                                } else {
                                                    continue;
                                                }
                                            }
                                            break;
                                        default:
                                            throw new RuntimeException("wrong Type for base :" + base.srcIndex);
                                    }
                                }

                                // check if the receiver has a target method
                                if (recv.isFragment()) {
                                    for (NumberedString sig : SigNumberedString.v().fragmentSigs) {
//                                        SootMethod method = Scene.v().loadClassAndSupport(receiver.getType().toString()).getOverriddenMethod(sig); //declaresMethodWithinPackage(Scene.v().loadClassAndSupport(receiver.getType().toString()), sig);
                                        Type type = receiver.getType();
                                        if (type instanceof RefType) {
                                            SootMethod method = getOverriddenMethodMap(((RefType) type).getSootClass()).get(sig); //declaresMethodWithinPackage(Scene.v().loadClassAndSupport(receiver.getType().toString()), sig);

                                            if (method != null && !method.isPhantom()) {
                                                lastInsertedUnit = addVirtualCallSite(s, iie, m, oldRecv, receiver, method, Kind.VIRTUAL, null, b, null, lastInsertedUnit);
                                            }
                                        }
                                    }
                                } else if (recv.isStartWith()) {
                                    Map<Type, Value> localValues = new HashMap<Type, Value>();
                                    for (SootMethod method : getOverriddenMethodsStartsWith(((RefType) receiver.getType()).getSootClass(), recv.sigRecv.getString())) {
                                        if (!method.isPhantom()) {
                                            lastInsertedUnit = addVirtualCallSite(s, iie, m, oldRecv, receiver, method, recv.kind, recv, b, localValues, lastInsertedUnit);
                                            solvedGroupSet.add(recv.group);
                                        }
                                    }
                                } else {

                                    SootMethod method = (recv.kind == Kind.INTERFACE) ? ((RefType) receiver.getType()).getSootClass().getMethod(recv.sigRecv)
                                            : getOverriddenMethodMap(((RefType) receiver.getType()).getSootClass()).get(recv.sigRecv);
                                    if (method != null && !method.isPhantom()) {
                                        lastInsertedUnit = addVirtualCallSite(s, iie, m, oldRecv, receiver, method, recv.kind, recv, b, null, lastInsertedUnit);
                                        solvedGroupSet.add(recv.group);
                                    }
                                }
                            }
                        } else if (callbackOrListenerTriggerMap.containsKey(subSig)) { // callback & listener
                            Map<Type, Value> localValues = new HashMap<Type, Value>();
                            Value v = null;
                            for (TempReceiver tempReceiver : callbackOrListenerTriggerMap.get(subSig)) {
                                if (tempReceiver.base != null && v == null) {
                                    if (s instanceof AssignStmt) {
                                        v = ((AssignStmt) s).getLeftOp();
                                    } else {
                                        v = Jimple.v().newLocal("$r" + (b.getLocalCount() + 1), iie.getMethodRef().returnType());
                                        b.getLocals().addFirst((Local) v);
                                        Unit u = Jimple.v().newAssignStmt(v, iie);
                                        b.getUnits().swapWith(s, u);
                                        lastInsertedUnit = u;
                                    }
                                    oldRecv = v;
                                }
                                lastInsertedUnit = addCallbackOrReceiver(oldRecv, tempReceiver.receiver, tempReceiver.method, b, localValues, lastInsertedUnit);
                            }
                            callbackOrListenerTriggerMap.removeAll(subSig);
                        } else {
                            // find receiver with param types
                            List<Type> paramTypes = iie.getMethodRef().parameterTypes();
                            for (int i = 0; i < paramTypes.size(); i++) {
                                // find listener or callback from the xml table
                                ListenerOrCallBack loc = listenerOrCallBackMap.get(paramTypes.get(i));
                                if (loc != null) {
                                    SootClass param = ((RefType) paramTypes.get(i)).getSootClass();
                                    Map<Type, Value> localValues = new HashMap<Type, Value>();
                                    if (iie.getArg(i) != NullConstant.v()) {
                                        receiver = iie.getArg(i);
                                        // get method implemented of callback interface
                                        for (SootMethod method : getInterfaceMethods(param)) { // it is interface....
                                            if (loc.trigger != null) { // if a trigger is not null of the targer listener or callback, keep the results with map for the trigger.
                                                callbackOrListenerTriggerMap.put(loc.trigger, new TempReceiver(oldRecv, receiver, method, loc.base));
                                            } else {
                                                lastInsertedUnit = addCallbackOrReceiver(oldRecv, receiver, method, b, localValues, lastInsertedUnit);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.println(b);
    }

    static class Parameter {
        public static final int LEFT = -1;
        public static final int TAG = -2;
        public static final int ID = -3;
        public static final int BASE = -4;
        public static final int UNKNOWN = -5;
        Type source;
        int index;
        int srcIndex;
        boolean ignoreable;
        Value v;

        public Parameter(Type source, int index, int srcIndex, boolean ignoreable, Value v) {
            this.source = source;
            this.index = index;
            this.srcIndex = srcIndex;
            this.ignoreable = ignoreable;
            this.v = v;
        }

        public Parameter(Type source, int index, int srcIndex, boolean ignoreable) {
            this(source, index, srcIndex, ignoreable, null);
        }

        public static int parseInt(String str) {
            if (str.isEmpty()) {
                return UNKNOWN;
            }

            if ("LEFT".equals(str)) {
                return LEFT;
            } else if ("TAG".equals(str)) {
                return TAG;
            } else if ("ID".equals(str)) {
                return ID;
            } else if ("BASE".equals(str)) {
                return BASE;
            } else {
                return Integer.parseInt(str);
            }
        }

        enum Type {VALUE, TRIGGER, RECEIVER, STORAGE, NEW, THIS}
    }

    class ListenerOrCallBack {
        final boolean listener;
        final NumberedString trigger;
        final Parameter base;
        final RefType ifaceType;

        public ListenerOrCallBack(boolean listener, NumberedString trigger, Parameter base, RefType ifaceType) {
            this.listener = listener;
            this.trigger = trigger;
            this.base = base;
            this.ifaceType = ifaceType;
        }
    }

    class Receiver {
        final int group; // if group is greater than 1, only one receiver can be selected within the same group.
        final NumberedString sigRecv;
        final Parameter base;
        final List<Parameter> params;
        final Kind kind;
        final boolean startWith;
        final boolean store;
        final boolean fragment;

        public Receiver(NumberedString sigRecv, Parameter base, List<Parameter> params, Kind kind, boolean startWith, int group, boolean store, boolean fragment) {
            this.sigRecv = sigRecv;
            this.base = base;
            this.params = params;
            this.kind = kind;
            this.startWith = startWith;
            this.group = group;
            this.store = store;
            this.fragment = fragment;
        }

        public boolean isStartWith() {
            return startWith;
        }

        public boolean isStore() {
            return store;
        }

        public boolean isFragment() {
            return fragment;
        }

        public String toString() {
            return sigRecv + " " + "startWith:" + startWith + " group id:" + group;
        }
    }

    class TempReceiver {
        final Value oldRecv;
        final Value receiver;
        final SootMethod method;
        final Parameter base;

        public TempReceiver(Value oldRecv, Value receiver, SootMethod method, Parameter base) {
            this.oldRecv = oldRecv;
            this.receiver = receiver;
            this.method = method;
            this.base = base;
        }
    }

    private int APIVersion = -1;
    private final int defaultSdkVersion = 15;

    public int getAPIVersion() {
        if(APIVersion == -1) {
            // process AndroidManifest.xml
            int sdkTargetVersion = androidManifest.getSdkTargetVersion();
            int minSdkVersion = androidManifest.getMinSdkVersion();

            if (sdkTargetVersion != -1) {
                if (sdkTargetVersion > defaultSdkVersion
                        && minSdkVersion != -1
                        && minSdkVersion <= defaultSdkVersion) {
                    G.v().out.println("warning: Android API version '" + sdkTargetVersion + "' not available, using minApkVersion '" + minSdkVersion + "' instead");
                    APIVersion = minSdkVersion;
                } else {
                    APIVersion = sdkTargetVersion;
                }
            } else if (minSdkVersion != -1) {
                APIVersion = minSdkVersion;
            } else {
                G.v().out.println("Could not find sdk version in Android manifest! Using default: " + defaultSdkVersion);
                APIVersion = defaultSdkVersion;
            }

            if (APIVersion <= 2)
                APIVersion = 3;
        }
        return APIVersion;
    }

    class ExtraLibrary {
        private String moduleName;
        private String packageName;
        private String filePath;


        public ExtraLibrary(String packageName, String filePath) {
            this.packageName = packageName;
            this.filePath = filePath;
            parse(filePath);
        }

        private void parse(String filePath) {
            try {
                ElfHelper elfHelper = new ElfHelper(filePath);
                ElfHelper.Dynamic[] dynamics = elfHelper.getDynamicSections();
                // get filename
                for (ElfHelper.Dynamic dyn : dynamics) {
                    if (dyn.d_tag == ElfHelper.Dynamic.DT_SONAME) {
                        this.moduleName = dyn.toString();
                        this.moduleName = this.moduleName.substring(3, this.moduleName.length() - 3);
                    }
                }
                // get cpu type

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getPackageName() {
            return packageName;
        }

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

    }
}
