package com.tosnos.cosa.binary;

import com.google.common.collect.*;
import com.microsoft.z3.*;
import com.microsoft.z3.Expr;
import com.tosnos.cosa.android.AndroidPackage;
import com.tosnos.cosa.binary.asm.*;
import com.tosnos.cosa.binary.asm.value.*;
import com.tosnos.cosa.binary.asm.value.Immediate;
import com.tosnos.cosa.binary.asm.value.JNI.JNIEnv;
import com.tosnos.cosa.binary.asm.value.JNI.JNIValue;
import com.tosnos.cosa.binary.asm.value.JNI.JavaVM;
import com.tosnos.cosa.binary.asm.value.memory.*;
import com.tosnos.cosa.binary.cfg.CFG;
import com.tosnos.cosa.binary.function.*;
import com.tosnos.cosa.util.Misc;
import com.tosnos.cosa.util.SigNumberedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.Body;
import soot.VoidType;
import soot.jimple.*;
import soot.jimple.Stmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by kevin on 12/7/14.
 */
public class NativeLibraryHandler implements com.tosnos.cosa.binary.asm.Modifier {
    public final static String SYS_LIB = "syslib";
    public final static String ENV_CLASS = "JNIEnv";
    public final static String VM_CLASS = "JavaVM";
    public final static String SYSTEM_LIB_CLASS = "SystemLibrary";

    public final static String EXT_LIB = "./libs/ext/";
    private final static int CAS = 0xffff0fc0;
    private final static int MBF = 0xffff0fa0; // LinuxKernelMemoryBarrierFunc;
    private final static Logger logger = LoggerFactory.getLogger(NativeLibraryHandler.class);
    public final AllocatedMemory pEnv = new AllocatedMemory(JNIEnv.env);
    public final AllocatedMemory pVM = new AllocatedMemory(JavaVM.vm);
    private final Map<String, LibraryModule> registeredModuleMap = new HashMap<String, LibraryModule>();
    private final Table<String, String, SubroutineOrFunction> exportedSubroutineMap = HashBasedTable.create();

    //private final Map<Integer, AbstractValue> stackMemory = new HashMap<Integer, AbstractValue>();
//    public final static HeapMemory heapMemory = new HeapMemory();
    private final Map<String, Address> exportedObjectMap = new HashMap<String, Address>();
    private final Map<SootMethod, NativeMethod> nativeMethodMap = new HashMap<SootMethod, NativeMethod>();
    private int cpuType;
    private Address stackBASE = new Address(new AbstractMemory("S"), 0xF0000000);
    private Map<SootField, JNIValue> fieldReferenceMap = new HashMap<SootField, JNIValue>();
    private CFG currentCFG;
    private Subroutine currentSubroutine;
    private Set<SootClass> clinitedSet = new HashSet<SootClass>();

    //    private boolean initializing = false;
    private Stack<Node> nodeStack = new Stack<Node>();
    private NZCVExpr[] nzcvExpr; // NZCV // for positive...
    private Random random = new Random();
//    private Map<AbstractValue, AbstractValue> pthreadKeys = new HashMap<AbstractValue, AbstractValue>();

    private Set<Function> invokedMethodCalls = new HashSet<Function>();

    private boolean jniException = false;
    private Map<String, String> errorMsgMap = new HashMap<String, String>();
    private com.microsoft.z3.Context ctx;

    private static NativeLibraryHandler instance = new NativeLibraryHandler();

    // keep the failed queue with required value (with java jni parameters)
    public Map<Subroutine, Set<Address>> failedQueue = new HashMap<Subroutine, Set<Address>>();
    public Set<Address> missingValueSet;


    private NativeLibraryHandler() {
    }

    public static NativeLibraryHandler getInstance() {
        return instance;
    }

    public void init(List<LibraryModule> modules, int cpuType) throws Exception {
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        this.ctx = new com.microsoft.z3.Context(cfg);
        this.cpuType = cpuType;

        // prepare JNI method for soot
        SootClass env = new SootClass(ENV_CLASS);
        Scene.v().addClass(env);

        for (int i = 0x10; i < 0x3a0; i = i + 4) {
            JNIFunction jniFunction = (JNIFunction) JNIEnv.env.read(i, TYPE_INT);
            SootMethod method = new SootMethod(jniFunction.getName(), new ArrayList(), VoidType.v(), soot.Modifier.PUBLIC | soot.Modifier.STATIC);
            env.addMethod(method);
        }

        SootClass vm = new SootClass(VM_CLASS);
        Scene.v().addClass(vm);

        for (int i = 0xc; i < 0x20; i = i + 4) {
            JNIFunction jniFunction = (JNIFunction) JavaVM.vm.read(i, TYPE_INT);
            SootMethod method = new SootMethod(jniFunction.getName(), new ArrayList(), VoidType.v(), soot.Modifier.PUBLIC | soot.Modifier.STATIC);
            vm.addMethod(method);
        }

        // prepare native functions for soot
        SootClass nativeLib = new SootClass(SYSTEM_LIB_CLASS);
        Scene.v().addClass(nativeLib);
        for (AbstractValue internalFunction : InternalFunction.getFunctions()) {
            if (internalFunction.isSubroutineOrFunction()) {
                String name = ((Function) internalFunction).getName();
                SootMethod method = new SootMethod(name, new ArrayList(), VoidType.v(), soot.Modifier.PUBLIC | soot.Modifier.STATIC);
                exportedSubroutineMap.put(name, SYS_LIB, (Function) internalFunction);
                nativeLib.addMethod(method);
            }
        }

        for (LibraryModule module : modules) {
            registerModule(module);
        }
        for (LibraryModule module : modules) {
            initModule(module);
        }

        // store current memory data
        for (LibraryModule module : modules) {
            module.setInitialized(LibraryModule.STATUS.DONE);
        }
    }

    public void cas() throws IOException, Z3Exception {
        AbstractValue oldVal = Register.getRegisterValue(0);
        AbstractValue newVal = Register.getRegisterValue(1);
        AbstractValue ptr = Register.getRegisterValue(2);
        if (!oldVal.isUnknown() && !newVal.isUnknown() && ptr.isAllocatedMemory()) {
            if (oldVal.intValue() == ptr.getValue().intValue()) {
                ((AllocatedMemory) ptr).write(newVal);
                Register.setValue(0, Immediate.ONE);
            } else {
                Register.setValue(0, Immediate.ZERO);
            }
        } else {
            Register.setValue(0, new Immediate());
        }
    }

    public void setJNIException() {
        jniException = true;
    }

    public boolean isJNIException() {
        return jniException;
    }

    public void clearJNIException() {
        jniException = false;
    }

    public LibraryModule getModule(String moduleName) {
        return registeredModuleMap.get(moduleName);
    }

    private void registerModule(LibraryModule module) {
        String moduleName = module.getModuleName();
        if (!registeredModuleMap.containsKey(moduleName)) {
            registeredModuleMap.put(moduleName, module);
            for (NativeMethod nativeMethod : module.getNativeMethods()) {
                nativeMethodMap.put(nativeMethod.getSootMethod(), nativeMethod);
            }

            for (Subroutine subroutine : module.getExportedSubroutines()) {
//                SubroutineOrFunction exportedSubroutine = exportedSubroutineMap.get(subroutine.getName());
//                if(exportedSubroutine!=null) {
//                    // if the subroutine is exist on exported functions already, it should be imported function
//                    subroutine.getLibraryModule().setAsImportedSubroutine(subroutine, exportedSubroutine);
//                } else {
                exportedSubroutineMap.put(subroutine.getName(), module.getModuleName(), subroutine);
//                }
            }
            exportedObjectMap.putAll(module.getExportedObject());
        }
    }

    public void addExportedSubroutineMap(Subroutine subroutine) {
        exportedSubroutineMap.put(subroutine.getName(), subroutine.getLibraryModule().getModuleName(), subroutine);
    }

    public void addNativeMethodMap(NativeMethod nativeMethod) {
        nativeMethodMap.put(nativeMethod.getSootMethod(), nativeMethod);
    }

//    public Map<AbstractValue, AbstractValue> getPthreadKeys() {
//        return pthreadKeys;
//    }

    private void setParameters(Node node, NativeMethod call) throws Z3Exception { //Subroutine procedure, Node node) { //}, SootMethod m) { // set register values or the stackMemory with parameter values.
        int index = 0;
        SootMethod m = call.getSootMethod();
        Register.setValue(index++, pEnv);
        // it should be pointer
        if (!m.isStatic()) {  // count for this object
            Register.setValue(index++, new JNIValue(JNIValue.THIS, JNIFunction.JOBJECT, new AllocatedMemory(new SootValue(m.getDeclaringClass())))); //, sootMethod.getDeclaringClass()));
        } else {
            Register.setValue(index++, new JNIValue(JNIValue.CLASS, JNIFunction.JCLASS, new AllocatedMemory(new SootValue(m.getDeclaringClass())))); //, sootMethod.getDeclaringClass()));
        }

        int argCount = m.getParameterCount(); // get a count of parameters from the method definition

        if (argCount == 0) { // if the target method doesn't take any parameters, return.
            return;
        }

        ArrayList<JNIValue> jniValues = new ArrayList<JNIValue>();


        for (int i = 0; i < argCount; i++) { // make spaces for parameters
            jniValues.add(new JNIValue(i, m.getParameterType(i)));
        }

        if (node != null) {
            // try to find an actual value.
            // find stmt related with current method
            Node parent = node.getParent();
            if (parent != null) {
                Body b = parent.getMethod().getActiveBody();

                Edge edge = node.getEdge();
                if (edge != null) {
                    final Stmt s = edge.srcStmt();
                    if (s != null && s.containsInvokeExpr()) {
                        InvokeExpr ie = s.getInvokeExpr();
                        for (int i = 0; i < argCount; i++) {
                            Value ref = ie.getArg(i);
                            // check instance......such as integer or String Value
                            if (ref instanceof IntConstant) {
                                jniValues.get(i).setValue(new Immediate(((IntConstant) ref).value));
                            } else if (ref instanceof LongConstant) {
                                jniValues.get(i).setValue(new Immediate(((LongConstant) ref).value));
                            } else if (ref instanceof StringConstant) {
                                jniValues.get(i).setValue(new StringValue(((StringConstant) ref).value));
                            } else {
                                Stmt s0 = s;
                                while ((s0 = (Stmt) b.getUnits().getPredOf(s0)) != null) {
                                    if (s0 instanceof AssignStmt) {
                                        if (ref == ((AssignStmt) s0).getLeftOp()) {
                                            ref = ((AssignStmt) s0).getRightOp();
                                            if (ref instanceof JInstanceFieldRef) {
                                                JNIValue value = fieldReferenceMap.get(((JInstanceFieldRef) ref).getField());
                                                if (value != null) {
                                                    jniValues.set(i, value);
                                                }
                                                break;
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

        int backIndex = argCount - 1;
        for (int i = 0; i < argCount; i++) {
            // type from SootMethod....
            if (index < Register.REG_PARAM_MAX) {
                JNIValue v = jniValues.get(i);
                if (v.getValueType() == JNIFunction.JLONG || v.getValueType() == JNIFunction.DOUBLE) { // The Long type needs one more space
                    if (v.isImmediate()) {
                        Immediate value = (Immediate) v.getValue();
                        Register.setValue(index++, new JNIValue(v.getParamIndex(), JNIFunction.JB_64, value.getLower(ctx)));
                        Register.setValue(index++, new JNIValue(v.getParamIndex(), JNIFunction.JH_64, value.getUpper(ctx)));
                    } else {
                        Immediate value = (Immediate) Immediate.newValue(v.getType());
                        Register.setValue(index++, new JNIValue(v.getParamIndex(), JNIFunction.JB_64, value.getLower(ctx)));
                        Register.setValue(index++, new JNIValue(v.getParamIndex(), JNIFunction.JH_64, value.getUpper(ctx)));

                    }
                } else {
                    Register.setValue(index++, v);
                }
            } else {
                JNIValue v = jniValues.get(backIndex--);
                if (v.getValueType() == JNIFunction.JLONG || v.getValueType() == JNIFunction.DOUBLE) { // The Long type needs one more space
                    if (v.isImmediate()) {
                        Immediate value = (Immediate) v.getValue();
                        push(new JNIValue(v.getParamIndex(), JNIFunction.JH_64, value.getUpper(ctx))); // put the value to the stackMemory
                        push(new JNIValue(v.getParamIndex(), JNIFunction.JB_64, value.getLower(ctx))); // put the value to the stackMemory
                    } else {
                        Immediate value = (Immediate) Immediate.newValue(v.getType());
                        push(new JNIValue(v.getParamIndex(), JNIFunction.JH_64, value.getUpper(ctx)));
                        push(new JNIValue(v.getParamIndex(), JNIFunction.JB_64, value.getLower(ctx)));
                    }
                } else {
                    push(v); // put the value to the stackMemory
                }
            }
        }
    }

    public com.microsoft.z3.Context getContext() {
        return ctx;
    }

    public void push(AbstractValue value) {
        Address stackAddress = Register.getSP();
        stackAddress = (Address) stackAddress.sub(Immediate.WORD);
        stackAddress.write(value);
        Register.setSP(stackAddress); // it will be stored into valueStack after setParameters
    }

    private void loadLibrary(String name) throws Exception {
        LibraryModule module = registeredModuleMap.get(name);
        if (module == null) {
            throw new RuntimeException("Library named " + name + " is not exist");
        }
        if (initModule(module)) {
            logger.info("{} is initialized by loadLibrary", module.getModuleName());
        }
    }

    public List<Subroutine> traverse(CallGraph cg, SootMethod entry) throws Exception {
        List<Subroutine> nativeMethods = new ArrayList<Subroutine>();
        traverse(cg, new Node(null, entry), nativeMethods, 0);
        return nativeMethods;
    }

    private void traverse(CallGraph cg, Node node, List<Subroutine> nativeMethods, int callStack) throws Exception {
        if (nodeStack.contains(node)) {
            return;
        }

        try {
            logger.trace(Misc.getSpaceString(callStack) + node.getMethod());

            nodeStack.push(node);
            if (node.getMethod().getNumberedSubSignature() == SigNumberedString.v().sigLoadLibrary) { // process loadLibrary (init a library)
                Edge edge = node.getEdge();
                if (edge != null) {
                    final Stmt s = edge.srcStmt();
                    if (s != null && s.containsInvokeExpr()) {
                        InvokeExpr ie = s.getInvokeExpr();
                        Value v = ie.getArg(0);
                        if (v instanceof StringConstant) {
                            loadLibrary("lib" + v.toString().replace("\"", "") + ".so");
                        }
                    }
                }
            }

            if (node.getMethod().isNative() && node.getMethod().getDeclaringClass().isApplicationClass()) {
                nativeMethods.add(doAnalysis(node));
            }

            NavigableMap<Integer, Edge> map = new TreeMap<Integer, Edge>();
            Iterator<Edge> it = cg.edgesOutOf(node.getMethod());


            while (it.hasNext()) { // fix ordering.... but.. it is not correct
                Edge e = it.next();
                cg.removeEdge(e);
                List<Unit> units = Lists.newArrayList(node.getMethod().getActiveBody().getUnits().iterator());
                int pos = units.indexOf(e.srcUnit())*100;
                if (pos < 0 && e.kind() != Kind.FINALIZE) { // skip the finalize method
                    throw new RuntimeException("!!"); // it might be casued by receiver and ....others...
                }
                while(map.containsKey(pos)) {
                    pos++;
                }
                map.put(pos, e);
            }


            node.addLeaves(map.values());

            callStack++;
            Node leaf;
            while ((leaf = node.popLeaf()) != null) {
                if (leaf.getMethod().getDeclaringClass().isApplicationClass()) { // filtering
                    traverse(cg, leaf, nativeMethods, callStack);
                }
            }
        } finally {
            nodeStack.pop();
        }
    }

    public NativeMethod getNativeMethod(SootMethod method) throws Exception {
        NativeMethod nativeMethod = nativeMethodMap.get(method);
        if (nativeMethod != null) {
            initModule(nativeMethod.getLibraryModule());
        }
        return nativeMethod;
    }

    //    public void doAnalysis(SootMethod nativeMethod) {
    public Subroutine doAnalysis(Node node) throws Exception {


        // reset all library modules
        for (LibraryModule module : registeredModuleMap.values()) {
            module.clear(); // clear memory except initialized data
        }

        SootMethod method = node.getMethod();
        System.out.println("\nStaring interpreting for " + method.getDeclaringClass().getName() + " " + method.getSubSignature());

        NativeMethod nativeMethod = getNativeMethod(method);

        if(nativeMethod==null) {
            int x = 10;
            x++;
        }
        clinit(method);
//        if (nativeMethod == null) {
//            String className = method.getDeclaringClass().getName();
//            String methodName = method.getName();

//            LibraryModule module = AndroidPackage.retrieveExternalModule(className, methodName, cpuType);
//            if (module != null) {
//                registerModule(module);
//                nativeMethod = nativeMethodMap.get(method);
//                if (nativeMethod == null) {
//                    logger.info("{} is not implemented", method.toString());
//                    return null;
//                }
//            } else {
//                throw new RuntimeException(className + " " + method.getSubSignature() + " is not found");
////                logger.error("{}  is not found", method.toString());
////                return null;
//            }
//        }
//
//        if (!nativeMethod.isLoaded()) { // load instructions
//            initModule(nativeMethod.getLibraryModule());
//        }
//
//
        nzcvExpr = new NZCVExpr[4];

        //    Subroutine method = totalSubroutines.get(address);
        //     if (!procedure.isVisited()) {
        currentSubroutine = nativeMethod;

        //         procedure.setVisited();
        Register.clearAll();
        stackBASE.getMemory().clear();
        Register.setSP(stackBASE);
        Register.setLR(Instruction.empty);

        setParameters(node, nativeMethod); //currentRootFunctionCall, node, method);


//        currentCFG = currentSubroutine.getCFG();

        missingValueSet = new HashSet<Address>();

        if (interpret(currentSubroutine)) { // FALSE: there is an error during the execution
            commit();
            logger.info("Success {}", method.getSubSignature());
            // check failedQueue
            for(Map.Entry<Subroutine, Set<Address>> entry:failedQueue.entrySet()) {
                for(Address value:entry.getValue()) {
                    AbstractValue a = value.deepRead(0, TYPE_INT);
                    if(a!=null) {
                        // check needed....
                    }
                }
            }
        } else {
            if(missingValueSet.size()>0) {
                failedQueue.put(currentSubroutine, missingValueSet);
            }
            logger.error("Error {}", method.getSubSignature());
        }
/*
        for(Map.Entry<Subroutine, Set<AbstractValue>> entry:failedQueue.entrySet()) {
            // check failed failedQueue
            if(entry.getKey()==currentSubroutine) {
                continue;
            }

            boolean pass = true;
            for(AbstractValue value:entry.getValue()) {
                if(value.isAddress()) {
                    AbstractValue v = ((Address)value).read(0, TYPE_INT);
                    int x = 10;
                    x++;

                } else {
                    throw new RuntimeException("it is not memory related value");
                }
            }
        }
*/

        // check R0
        // put a real value into stackMemory for later usage
        // it can be.... real value...
        if (method.getReturnType() != VoidType.v()) {
            AbstractValue v = Register.getRegisterValue(Register.R0);
            if (v != null) {
                if (!v.isJNIValue()) {
                    JNIValue value = new JNIValue(JNIValue.RESULT, method.getReturnType());
                    if (v.isImmediate()) {
                        value.setValue(v);
                    } else {
                        value.setValue(new AllocatedMemory(v));
                    }
                    v = value;
                }
                Node parent = node.getParent();
                if (parent != null) {
                    Edge edge = node.getEdge();
                    if (edge != null) {
                        Stmt s = edge.srcStmt(); // for the current method
                        if (s != null && s.containsInvokeExpr() && s instanceof AssignStmt) {
                            Value ret = ((AssignStmt) s).getLeftOp();
                            Body b = parent.getMethod().getActiveBody();
                            while ((s = (Stmt) b.getUnits().getSuccOf(s)) != null) { // find a stmt that has a method taking the return value as a parameter
                                if (s instanceof AssignStmt && ((AssignStmt) s).getRightOp() == ret) {
                                    ret = ((AssignStmt) s).getLeftOp();
                                    if (ret instanceof JInstanceFieldRef) {
                                        fieldReferenceMap.put(((JInstanceFieldRef) ret).getField(), (JNIValue) v);
                                        logger.info("value {} kept with filed {}", v, ((JInstanceFieldRef) ret).getFieldRef());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return nativeMethod;
    }

    private boolean initModule(LibraryModule module) throws Exception {
        if (module.isInitialized()) {
            return false;
        }

        List<String> requiredLibraries = module.getRequiredLibraries();
        for (String moduleName : requiredLibraries) {
            LibraryModule requiredModule = registeredModuleMap.get(moduleName);
            if (requiredModule == null) {
                // check files
                LibraryModule extModule = new LibraryModule(null, EXT_LIB + "lib" + moduleName + ".so");
                registerModule(extModule);
                logger.info("Loading external library : {}", moduleName);
            }
        }

        logger.info("Loading library : {}", module.getModuleName());

        module.load(); // load instructions

        // linking external symbols
        for (ImportedSubroutine importedExecutable : module.getImportedSubroutines()) {
            SubroutineOrFunction exportedSubroutine = exportedSubroutineMap.get(importedExecutable.getName(), SYS_LIB);
            if (exportedSubroutine == null) {
                for (String requireLibraryName : module.getRequiredLibraries()) {
                    exportedSubroutine = exportedSubroutineMap.get(importedExecutable.getName(), requireLibraryName);
                    if (exportedSubroutine != null) {
                        break;
                    }
                }
            }

            if (exportedSubroutine == null) {
                throw new RuntimeException("symbol is not found " + importedExecutable.getName() );
            }
            importedExecutable.setSubroutineOrFunction(exportedSubroutine);
        }

        for (Map.Entry<String, Address> entry : module.getImportedObject().entrySet()) {
            entry.getValue().write(exportedObjectMap.get(entry.getKey()));
        }

        logger.info("{} is loaded", module.getModuleName());
        module.setInitialized(LibraryModule.STATUS.START);

        // dump registers
        Subroutine oldSubroutine = currentSubroutine;
        NZCVExpr[] oldNzcvExpr = nzcvExpr;
        AbstractValue[] oldRegs = Register.dump();
        AbstractValue[] oldVPFRegs = VPFRegister.dump();
        nzcvExpr = new NZCVExpr[4];



        List<Subroutine> initArrays = module.getInitArrays();
        if (initArrays.size() > 0) {
            logger.info("file: " + module.getModuleName());
            logger.info("initialized by .init_array");

            for (Subroutine initArray : initArrays) {
                Register.clearAll();
                Register.setSP(stackBASE);
                currentSubroutine = initArray;
                if(!interpret(currentSubroutine)) {
                    throw new RuntimeException("init failed");
                }
            }
        }

        // JNI_OnLoad()
        Register.clearAll();
        Register.setSP(stackBASE);
        Subroutine jniOnLoad = module.getJniOnLoad();
        if (jniOnLoad != null) {
            Register.setValue(Register.R0, pVM);

            currentSubroutine = jniOnLoad;
            if(!interpret(currentSubroutine)) {
                throw new RuntimeException("JNI_OnLoad failed");
            }
        }
        module.setInitialized(LibraryModule.STATUS.DONE);

        // restore registers and a stack
        Register.restore(oldRegs);
        VPFRegister.restore(oldVPFRegs);
        nzcvExpr = oldNzcvExpr;
        currentSubroutine = oldSubroutine;

        logger.info("Loaded library : {}", module.getModuleName());
        return true;
    }

    // for static initializer
    private synchronized void clinit(SootMethod method) throws Exception {
        SootClass sc = method.getDeclaringClass();
        if (!clinitedSet.contains(sc)) {
            clinitedSet.add(sc);
            if (sc.declaresMethodByName(SootMethod.staticInitializerName)) {
                SootMethod clinitMethod = sc.getMethodByName(SootMethod.staticInitializerName);
                Body clinitBody = clinitMethod.retrieveActiveBody();
                for (Unit unit : clinitBody.getUnits()) {
                    if (unit instanceof Stmt) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            SootMethod m = stmt.getInvokeExpr().getMethod();
                            if (m.isNative()) {
                                NativeMethod procedure = getNativeMethod(m);
                                if (procedure != null) {

                                    Subroutine oldSubroutine = currentSubroutine;
                                    currentSubroutine = procedure;

                                    NZCVExpr[] oldNzcvExpr = nzcvExpr;
                                    AbstractValue[] oldRegs = Register.dump();
                                    AbstractValue[] oldVPFRegs = VPFRegister.dump();
                                    nzcvExpr = new NZCVExpr[4];

                                    Register.clearAll();
                                    Register.setSP(stackBASE);
                                    setParameters(null, procedure); // for clinit - a static initializer doesn't take any parameters

                                    interpret(currentSubroutine);

                                    Register.restore(oldRegs);
                                    VPFRegister.restore(oldVPFRegs);
                                    nzcvExpr = oldNzcvExpr;
                                    currentSubroutine = oldSubroutine;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private SubroutineOrFunction getSubroutineOrFunction(LibraryModule module, String name) throws Exception {
        Subroutine subroutine = module.getSubroutine(name);
        return getSubroutineOrFunction(subroutine);
    }

    private SubroutineOrFunction getSubroutineOrFunction(LibraryModule module, Integer address) throws Exception {
        Subroutine subroutine = module.getSubroutine(address);
        return getSubroutineOrFunction(subroutine);
    }

    private SubroutineOrFunction getSubroutineOrFunction(SubroutineOrFunction sof) throws Exception {
        if (sof == null) {
            return null; // throw new RuntimeException(name + " is not found");
        }

        if (sof.isImported()) {
            sof = ((ImportedSubroutine) sof).getFunctionOrSubroutine();
        }


        if (sof.isSubroutine()) {
            initModule(((Subroutine) sof).getLibraryModule());
            if (((Subroutine) sof).isNativeMethod()) {
                clinit(((NativeMethod) sof).getSootMethod());
            }
        }

        return sof;
    }

    // return true: exit, false: normal
    private boolean interpret(Subroutine procedure) throws Exception {
        return interpret(procedure, null, new Stack<Subroutine>(), new Stack<Instruction>());
    }

    private boolean interpret(Subroutine procedure, Instruction startInst, Stack<Subroutine> callStack, Stack<Instruction> ambiguousCallStack) throws Exception {
        if (startInst != null) { // to text ambiguous condition
            Register.setPC(startInst);
        } else {
            logger.info("PUSH");
            callStack.push(procedure);
            logger.warn("\n\n{}#{}-Interpreting {} - {}", Misc.getSpaceString(callStack.size()), callStack.size(), procedure.getName(), procedure.getLibraryModule().getModuleName());
            Register.setPC(procedure.getFirstInstruction());
        }

        Instruction instruction;
        int itCount = 0;
        while ((instruction = Register.getPC()) != Instruction.empty) { //  && instruction.getAddress() != returnAddress) {
            boolean printed = false;
            try {
                if (itCount > 0) {
                    itCount--;
                }

                int modifier = instruction.getModifier();
                byte handlingType = instruction.getHandlingType();
                Instruction pcInst = instruction.getLibraryModule().getInstruction(instruction.getAddress() + (instruction.isThumb() ? 4 : 8));
                if (pcInst != null) {
                    Register.setPC(pcInst);
                } else {
                    Register.setPC(new Immediate(instruction.getAddress() + (instruction.isThumb() ? 4 : 8)));
                }

                if(instruction.isConditional()) {
                    byte conditional = instruction.getConditional();
                    ConstrainExpr positiveExpr = getConstrainExpr(conditional);
                    Set<Numeral> numeralSet = positiveExpr.setConstrain();
                    boolean positiveFlag = positiveExpr.checkCondition(ctx);
                    positiveExpr.releaseConstrain(numeralSet);

                    ConstrainExpr negativeExpr = getConstrainExpr(INVCOND[conditional]);
                    numeralSet = negativeExpr.setConstrain();
                    boolean negativeFlag = negativeExpr.checkCondition(ctx);
                    negativeExpr.releaseConstrain(numeralSet);


                    if (!positiveFlag && !negativeFlag) {
                        logger.error("Consistent condition!!! {}\n", Thread.currentThread().getStackTrace()[1]);
                        return false;
                    }

                    if(positiveFlag && negativeFlag) {
                        logger.error("Ambiguous conditional "+ instruction.toString());
                        return runAmbiguous(positiveExpr, negativeExpr, procedure, instruction, (Stack<Subroutine>) callStack.clone(), ambiguousCallStack);
                    }

                    if(!positiveFlag) {
                        if(instruction.isSkipForFalse()) {
                            logger.info("{}(skipped)", instruction.toString());
                            Register.setPC(instruction.getNext());
                            continue;
                        } else if(conditional==CONDI_CS) {
                            instruction.setCarry(false);
                        }
                    } else if(conditional == CONDI_CS) {
                        instruction.setCarry(true);
                    }
                }

                if (instruction.isTableSwitchBranch()) { // TODO: where is the next instruction for the switch table
                    AbstractValue rn = ((TableSwitchBranchInstruction) instruction).getRn().getValue();
                    AbstractValue rm = ((TableSwitchBranchInstruction) instruction).getRm().getValue();

                    if (rn.isUnknown() || rm.isUnknown()) { // test all cases
                        // create table...
                        logger.info("Switch Branch\n");
                        boolean result = false;


                        for (int branch : ((TableSwitchBranchInstruction) instruction).getBranches()) {
                            logger.info("Switch 0x" + Integer.toHexString(branch));
                            // check point.....
                            MemorySnapshot snapshot = new MemorySnapshot();
                            if (interpret(procedure, instruction.getLibraryModule().getInstruction(branch), (Stack<Subroutine>) callStack.clone(), ambiguousCallStack)) {
                                result |= true;
                                snapshot.commit();
                            } else {
                                snapshot.rollback();
                            }
                        }
                        logger.info("Switch Done\n");


                        return result;
                    } else {
                        AbstractValue branchValue = instruction.getLibraryModule().getInternalMemory().read(((AbstractValue) rn).intValue() + ((AbstractValue) rm).intValue(), handlingType);
                        Register.setPC(instruction.getLibraryModule().getInstruction(pcInst.intValue() + branchValue.intValue() * 2));
                    }
                    continue;
                } else if (instruction.isBranch()) {
                    // for comparebracnh update flags first
                    if(instruction.isCompareBranch()) {
                        Register source = (Register)instruction.getOperand(0);
                        AbstractValue value = source.getValue();
                        Numeral numeral = value.getNumeral();
                        Opcode opcode = instruction.getOpcode();
                        if(!numeral.isUnknown()) {
                            boolean result = numeral.intValue()==0?true:false; // for cbz
                            if(opcode==Opcode.CBZ?!result:result) {
                                Register.setPC(instruction.getNext());
                                continue;
                            }
                        } else {
                            BitVecExpr bitVecExpr = numeral.getBitVecExpr(ctx);
                            BoolExpr exp = ctx.mkEq(bitVecExpr, ctx.mkBV(0, 32));
                            ConstrainExpr positiveExpr = new ConstrainExpr(exp).setNumeral(numeral);
                            ConstrainExpr negativeExpr = new ConstrainExpr(ctx.mkNot(exp)).setNumeral(numeral);

                            Set<Numeral> numeralSet = positiveExpr.setConstrain();
                            boolean positiveFlag = positiveExpr.checkCondition(ctx);
                            positiveExpr.releaseConstrain(numeralSet);

                            numeralSet = negativeExpr.setConstrain();
                            boolean negativeFlag = negativeExpr.checkCondition(ctx);
                            negativeExpr.releaseConstrain(numeralSet);

                            if (!positiveFlag && !negativeFlag) {
                                logger.error("Consistent condition!!! {}\n", Thread.currentThread().getStackTrace()[1]);
                                return false;
                            }

                            if(positiveFlag && negativeFlag) {
                                logger.error("Ambiguous conditional "+ instruction.toString());
                                return runAmbiguous(positiveExpr, negativeExpr, procedure, instruction, (Stack<Subroutine>) callStack.clone(), ambiguousCallStack);
                            }

                            if(opcode==Opcode.CBZ?!positiveFlag:positiveFlag) {
                                Register.setPC(instruction.getNext());
                                continue;
                            }
                        }
                    }


                    SubroutineOrFunction nextSub = null;
                    LibraryModule module = instruction.getLibraryModule();
                    if (instruction.isCall()) { // call instruction
                        Instruction nextInst = instruction.getNext();
                        if (nextInst == null) {
                            int address = instruction.getAddress() + (instruction.isThumb() ? 2 : 4);
                            Register.setLR(new Address(instruction.getLibraryModule().getInternalMemory(), address));
                        } else {
                            Register.setLR(nextInst);
                        }
                    }

                    Operand rd = ((BranchInstruction) instruction).getBranchDestination();
                    if (rd.isOperandSet()) {
                        rd = rd.getValue(); // ((OperandSet) rd).getValue(this, flow);
                    }


                    if (rd.isRegister()) {
                        if (((Register) rd).isLinkRegister()) { // return
                            Register.setPC(Register.getLR());
                            logger.info("RETURN\n");
                            logger.info("POP");
                            callStack.pop();
                            continue;
                        } else if (((Register) rd).isProgramCounter()) { // return
                            int offset = instruction.isThumb() ? 4 : 8;
                            Register.setPC(module.getInstruction(instruction.getAddress() + offset));
                            continue;
                        } else {
                            rd = rd.getValue();
                        }
                    }

                    if (rd.isAssociatedValue()) {
                        rd = rd.getValue();
                    }


                    AbstractValue value = (AbstractValue) rd;

                    if (value.isIMemoryValue()) {
                        LibraryModule libraryModule = ((IMemoryValue) value).getModule();
                        if (libraryModule != null) {
                            module = libraryModule;
                        }
                        // value = ((IMemoryValue)value).read(0, 0);
                    } else if (value.isVariable()) {
                        // get variable


                        LibraryModule libraryModule = ((LocalVariable) value).getModule();
                        if (libraryModule != null) {
                            module = libraryModule;
                        }
                    }

                    logger.info("Register {} is {}", rd, value);
                    if(value.isUnknown()) { // unknown branch
                        logger.debug("Unknown indirect branch : {}", instruction);
                        return false;
                    }
                    if (value.isSubroutineOrFunction()) { // for JNI function
                        nextSub = (SubroutineOrFunction) value;
                    } else if(value.isAddress()) {
                        AbstractValue v = ((Address)value).read();
                        if(v.isSubroutineOrFunction()) {
                            nextSub = (SubroutineOrFunction) v;
                        } else {
                            Integer addr = value.intValue() & ~1;
                            Instruction nextInst = module.getInstruction(addr);
                            if (nextInst != null) {
                                Register.setPC(nextInst);
                                continue;
                            }
                        }
                    } else {
                        Integer addr = value.intValue() & ~1;
                        nextSub = getSubroutineOrFunction(module, addr);
                        if (nextSub == null) {
                            if (addr == CAS) {
                                cas();
                                logger.debug("Internal kernel function compare-and-swap (CAS)");
                                Register.setPC(Register.getLR());
                                continue;
                            } else if (addr == MBF) {
                                logger.debug("Internal kernel function memory barrier(MBF)");
                                Register.setPC(Register.getLR());
                                continue;
                            } else {
                                Instruction nextInst = module.getInstruction(addr);
                                if (nextInst != null) {
                                    Register.setPC(nextInst);
                                    continue;
                                }
                            }
                        }
                    }
                    if (nextSub != null) {
                        if (nextSub.isFunction()) { // internal or JNI function
                            if (((Function) nextSub).exec(this, module, procedure, callStack)) { // should module be null??
                                logger.error("abort " + Thread.currentThread().getStackTrace()[1]);
                                return false;
                            }
                            // store the result
                            logger.debug("Internal function {}, result: {}", nextSub.getName(), Register.getRegisterValue(Register.R0));
                            Register.setPC(Register.getLR());
                            continue;
                        } else if (nextSub.getName().equals("__umodsi3")) { // for test
                            Register.setValue(0, new Immediate(Integer.toUnsignedLong(Register.r0.getValue().intValue()) % Integer.toUnsignedLong(Register.r1.getValue().intValue())));
                            Register.setPC(Register.getLR());
                            logger.debug("__umodsi3");
                            continue;
                        } else {
                            if (((Subroutine) nextSub).isSystemSubroutine()) {
                                logger.debug("abort {} is system procedure {}",  nextSub, Thread.currentThread().getStackTrace()[1]);
                                // clear result of procedure to unknown

//                                    Register.setValue(Register.R0, AbstractValue.empty);
//                                    Register.setPC(Register.getLR());
                                //continue;
                                // return...
                                return false;
//                                    throw new RuntimeException("system !");
//                            if (callStack.contains(nextSub)) {
//                                logger.info("{} is recursive", nextSub.getName());
//                                // clear result of procedure to unknown
//                                Register.setValue(Register.R0, AbstractValue.empty);
//                                valueTracker.put(index, Register.R0, Register.getValue(Register.R0));
//                                Register.setPC(Register.getLR());
//                                continue;
//                                //  return; // Register.getValue(Register.LR).intValue();
                            } else {
                                logger.info("PUSH");
                                callStack.push((Subroutine) nextSub);
                                logger.warn("\n\n{}#{}-Interpreting {} - {}", Misc.getSpaceString(callStack.size()), callStack.size(), nextSub.getName(), ((Subroutine) nextSub).getLibraryModule().getModuleName());
                                Register.setPC(((Subroutine) nextSub).getFirstInstruction());
                                continue;
                            }
                        }
//                        } else if(rd.isVariable() && ((Variable)rd).getName().startsWith("nullsub")) {
//                            Register.setPC(Register.getLR());
//                            continue;
                    }
                    throw new RuntimeException("unknown branch");
                } else if (instruction.isLoad()) {
                    if(instruction.getAddress()==0x1ee6c) {
                        int x = 10;
                        x++;
                    }

                    Register[] destinations = ((LoadInstruction) instruction).getLoadDestinations();
                    Operand rn = ((LoadInstruction) instruction).getLoadSource();

/*
                    // check if the current value has a constrains that indicates the value equals to zero, then return just false, since it will be assigned with an allocated memory block,
                    // otherwise, read the value from the backend memory space with an address.
                    // in this case, it will create an other pass to handle multiple data format for the same address


                    // if this is the same memory block

                    // for failing queue
                    boolean checkPseudo = false;
                    for(Operand operand:rn.getElements()) {
                        if(operand.getValue().isPseudoValue()) {
                            AbstractValue ref = ((PseudoValue)operand.getValue()).getReference();
                            if(ref.isAddress()) { // if the address is from internal memory
                                missingValueSet.add((Address)ref);
                            }
                            checkPseudo = true;
                        }
                    }

                    if(checkPseudo) {
                        logger.debug("abort - from missing data {}", Thread.currentThread().getStackTrace()[1]);
                        return false;
                    }
*/


                    // change pseudoValue to heap memory value
                    for(Operand operand:rn.getElements()) {
                        if(operand.getValue().isPseudoValue()) {
                            PseudoValue v = (PseudoValue)operand.getValue();
                            Numeral n = v.getNumeral();
                            BitVecExpr exprValue = n.getBitVecExpr(ctx);
                            Expr notZeroConstrain = ctx.mkNot(ctx.mkEq(exprValue, ctx.mkBV(0, exprValue.getSortSize())));
                            for(BoolExpr p:v.getNumeral().getConstrains()) {
                                if(notZeroConstrain.equals(p)) {
                                    logger.error("unnecessary routine");
                                    return false;
                                }
                            }

                            if(v.getReference().isAddress()) {
                                // check deepmemory...if it is nul l thatn assigen...
                                AbstractValue deep = ((Address)v.getReference()).deepRead(0, TYPE_INT);
                                if(deep!=null) {
                                    v.setReplacedValue((AllocatedMemory)deep); // set memory
                                } else {
                                    logger.error("it may be disordered execution");
                                    missingValueSet.add((Address)v.getReference());
                                    return false;
//                                    v.setAsAllocatedMemory();
                                }
                            }
                        }
                    }

                    boolean before = ((LoadInstruction) instruction).isBefore();
                    boolean decrement = ((LoadInstruction) instruction).isDecerement();
                    AbstractValue source = rn.getValue();
                    int size = destinations.length;
                    for (int i = 0; i < size; i++) {
                        Register register = decrement?destinations[size-i-1]:destinations[i];
                        AbstractValue loadedValue;
                        if (before) {
                            source = decrement ? source.sub(Immediate.WORD) : source.add(Immediate.WORD);
                        }

                        logger.info("load source is {}", source);
                        if (source.isIMemoryValue()) {
                            if (register.isDoubleword()) {
                                loadedValue = ((IMemoryValue) source).read(TYPE_DOUBLE);
                            } else {
                                loadedValue = ((IMemoryValue) source).read(handlingType);
                            }
                        } else { // if (value.isImmediate()) { // it could be from heap memory
                            try {
                                if (register.isDoubleword()) {
                                    loadedValue = instruction.getLibraryModule().read(source.getValue().intValue(), TYPE_DOUBLE);
                                } else {
                                    loadedValue = instruction.getLibraryModule().read(source.getValue().intValue(), handlingType);
                                }
                            } catch (NullPointerException | IOException e) {
                                logger.error("abort:" + e.toString());
                                return false;
                            }
                        }

//                                if (loadedValue != null && loadedValue.getValue() != null && (type == TYPE_DOUBLE && !register.isDoubleword()
//                                        && (loadedValue.isImmediate() && (loadedValue).getNumeral().getType() == TYPE_DOUBLE)
//                                        || (loadedValue.isAssociatedValue()
//                                        && loadedValue.getValue().isImmediate()
//                                        && ((loadedValue.getValue()).getNumeral().getType() == TYPE_DOUBLE)))) {
                        if (loadedValue != null && loadedValue.getValue() != null && handlingType == TYPE_DOUBLE && !register.isDoubleword() && loadedValue.getNumeral().getType() == TYPE_DOUBLE) {
                            AssociatedValue v = null;
                            if (loadedValue.isAssociatedValue()) {
                                v = (AssociatedValue) loadedValue;
                                loadedValue = loadedValue.getValue();
                            }
                            if (size > (i + 1)) {
//                                        if (intValue != null) {
//                                            if (v == null) {
//                                                register.setValue(new Immediate(intValue[0]));
//                                                destinations[++i].setValue(new Immediate(intValue[1]));
//                                            } else {
//                                                register.setValue(new AssociatedValue(v.getAddress(), new Immediate(intValue[0])));
//                                                destinations[++i].setValue(new AssociatedValue((Address) v.getAddress().getReplacement(v.getNumeral().add(ctx, Immediate.WORD.getNumeral())), new Immediate(intValue[1])));
//                                            }
//                                        } else {
//                                            Immediate newDouble = Immediate.newValue(TYPE_DOUBLE);
                                if (v == null) {
                                    register.setValue(loadedValue.getLower(ctx));
                                    i++;
                                    register = decrement?destinations[size-i-1]:destinations[i];
                                    register.setValue(loadedValue.getUpper(ctx));
                                } else {
                                    register.setValue(new AssociatedValue(v.getAddress(), loadedValue.getLower(ctx)));
                                    i++;
                                    register = decrement?destinations[size-i-1]:destinations[i];
                                    register.setValue(new AssociatedValue((Address) v.getAddress().add(Immediate.WORD), loadedValue.getUpper(ctx)));

                                }
//                                        }
                            } else {
                                throw new RuntimeException("not implemented");
                            }
                        } else {
                            register.setValue(loadedValue);
                            logger.debug("loaded Value {}", register.getValue());
                        }
//                                valueTracker.put(index, register.getNumber(), loadedValue);

                        if (!before) {
                            source = decrement ? source.sub(Immediate.WORD) : source.add(Immediate.WORD);
                        }
                    }

                    if (rn.isRegister() && rn.isPreIndexed()) {
                        ((Register) rn).setValue(source);
                    }

                    // post indexed value....
                    if (instruction.getOperands().length == 3 && rn.isAddressSet() && instruction.getOperand(2).isImmediate()) {
                        Register base = ((AddressSet) rn).getBaseRegister();
                        base.setValue(base.getValue().add(instruction.getOperand(2).getValue()));
                    }


                    if (((LoadInstruction) instruction).isPCRelative()) { // && instruction.inLastBlock()) {
                        // check pc...if this is subroutine or not.
                        AbstractValue v = Register.getRegisterValue(Register.PC);

                        if(v.isAddress() && ((Address)v).read().isSubroutineOrFunction()) {
                            v = ((Address)v).read();
                        }

                        if (v.isSubroutineOrFunction()) {
                        logger.info("JNI Function {}\n", v); // for pop
                        if (((Function) v).exec(this, instruction.getLibraryModule(), procedure, callStack)) { // should module be null??
                            logger.error("abort " + Thread.currentThread().getStackTrace()[2]);
                            return false;
                        }
                        logger.debug("Internal function {}, result: {}", ((Function) v).getName(), Register.getRegisterValue(Register.R0));
                        Register.setPC(Register.getLR());


                        } else if (!v.isInstruction() && instruction.getPrev().getOperand(0).isRegister() && ((Register) instruction.getPrev().getOperand(0)).isLinkRegister()) {


                            callStack.add(callStack.peek());
                            logger.info("Call sub\n"); // for pop
                            try {
                                Register.setPC(instruction.getLibraryModule().getInstruction(v.intValue()));
                            } catch (RuntimeException e) {
                                logger.error("abort:" + e.toString());
                                callStack.pop();
                                return false;
                            }
                        } else {
                            logger.info("RETURN\n"); // for pop
                            logger.info("POP");
                            callStack.pop();
                        }
                        continue;
                    }
//                        }
                } else if (instruction.isStore()) {

                    Register[] sources = ((StoreInstruction) instruction).getStoreSources();
                    Operand rd = ((StoreInstruction) instruction).getStoreDestination();

/*
                    // for failing queue
                    boolean checkPseudo = false;
                    for(Operand operand:rd.getElements()) {
                        if(operand.getValue().isPseudoValue()) {
                            AbstractValue ref = ((PseudoValue)operand.getValue()).getReference();
                            if(ref.isAddress()) { // if the address is from internal memory
                                missingValueSet.add((Address)ref);
                            }
                            checkPseudo = true;
                        }
                    }

                    if(checkPseudo) {
                        logger.debug("abort - from missing data {}", Thread.currentThread().getStackTrace()[1]);
                        return false;
                    }
*/

                    // change pseudoValue to heap memory value
                    for(Operand operand:rd.getElements()) {
                        if(operand.getValue().isPseudoValue()) {
                            PseudoValue v = (PseudoValue)operand.getValue();
                            Numeral n = v.getNumeral();
                            BitVecExpr exprValue = n.getBitVecExpr(ctx);
                            Expr notZeroConstrain = ctx.mkNot(ctx.mkEq(exprValue, ctx.mkBV(0, exprValue.getSortSize())));
                            for(BoolExpr p:v.getNumeral().getConstrains()) {
                                if(notZeroConstrain.equals(p)) {
                                    logger.error("unnecessary routine");
                                    return false;
                                }
                            }

                            if(v.getReference().isAddress()) {
                                // check deepmemory...if it is nul l thatn assigen...
                                AbstractValue deep = ((Address)v.getReference()).deepRead(0, TYPE_INT);
                                if(deep!=null) {
                                    v.setReplacedValue((AllocatedMemory)deep); // set memory
                                } else {
                                    logger.error("it may be disordered execution");
                                    missingValueSet.add((Address)v.getReference());
                                    return false;
//                                    v.setAsAllocatedMemory();
                                }
                            }
                        }
                    }


                    AbstractValue value = rd.getValue();

//                    if ((value.isAddress() && ((Address) value).getMemory() == stackBASE.getMemory())) {
//                        sources = Arrays.copyOf(sources, sources.length, Register[].class); // create a new array for a reversed list
//                        Collections.reverse(Arrays.asList(sources));
//                    }
//                    else if (value.isPseudoValue()) {
//                        ((PseudoValue) value).setDynamicMemoryType();
//                        value = value.getValue();
//                    }
// TODO: pseudo value

                    boolean before = ((StoreInstruction) instruction).isBefore();
                    boolean decrement = ((StoreInstruction) instruction).isDecerement();

                    // retrieve real values
                    int size = sources.length;
                    for (int i=0;i<size;i++) { //Register register : sources) {
                        Register register = decrement?sources[size-i-1]:sources[i];
                        AbstractValue v = register.getValue();
                        if (before) {
                            value = decrement ? value.sub(Immediate.WORD) : value.add(Immediate.WORD);
                        }

                        logger.info("store destination is {}", value);

                        if (value.isIMemoryValue()) {
                            // store....

                            if (register.isDoubleword()) {
                                ((IMemoryValue) value).write(v, TYPE_DOUBLE);
                            } else {
                                ((IMemoryValue) value).write(v, handlingType);
                            }
                            logger.info("stored value is {}", v);
                        } else if (value.isVariable()) {
                            if (register.isDoubleword()) {
                                ((LocalVariable) value).write(v, TYPE_DOUBLE);
                            } else {
                                ((LocalVariable) value).write(v, handlingType);
                            }
                        } else if (value.isUnknown()) {

                            throw new RuntimeException("UNKNOWN");
                        } else {
                            instruction.getLibraryModule().write(value.intValue(), v, handlingType);
                        }

                        if (!before) {
                            value = decrement ? value.sub(Immediate.WORD) : value.add(Immediate.WORD);
                        }
                    }

                    if (rd.isRegister() && rd.isPreIndexed()) {
                        ((Register) rd).setValue(value);
                    }

                    // post index??
                    if (instruction.getOperands().length == 3 && rd.isAddressSet() && instruction.getOperand(2).isImmediate()) {
                        Register base = ((AddressSet) rd).getBaseRegister();
                        base.setValue(base.getValue().add(instruction.getOperand(2).getValue()));
                    }
                /*
                STREX performs a conditional store to memory. The conditions are as follows:
If the physical address does not have the Shared TLB attribute, and the executing processor has an outstanding tagged physical address, the store takes place, the tag is cleared, and the value 0 is returned in Rd.
If the physical address does not have the Shared TLB attribute, and the executing processor does not have an outstanding tagged physical address, the store does not take place, and the value 1 is returned in Rd.
If the physical address has the Shared TLB attribute, and the physical address is tagged as exclusive access for the executing processor, the store takes place, the tag is cleared, and the value 0 is returned in Rd.
If the physical address has the Shared TLB attribute, and the physical address is not tagged as exclusive access for the executing processor, the store does not take place, and the value 1 is returned in Rd.
                 */
                    if ((modifier & MODIFIER_EXTENTION) > 0) { // for strex
                        sources[0].setValue(Immediate.ZERO);
                    }
                } else if (instruction.isMove()) {
                    instruction.compute(ctx, itCount==0?nzcvExpr:null);
                    if(((MoveInstruction)instruction).isPCRelated()) {
                        continue;
                    }
                } else if (instruction.isArithmetic() || instruction.isGeneral() || instruction.isLogical() || instruction.isShift() || instruction.isMultiply()) {
                    instruction.compute(ctx, itCount==0?nzcvExpr:null);
                } else if (instruction.isCompare()) {
                    instruction.compute(ctx, nzcvExpr);
                } else if (instruction.isVFPMove() || instruction.isVFPArithmetic() || instruction.isVFPDivide()) {
                    instruction.compute(ctx);
                } else if (instruction.isIfThen()) {
                    itCount = ((IfThenInstruction) instruction).getConditionSwitch().length + 2;
                } else if (!instruction.isIgnorable()) {
                    throw new RuntimeException("not implemented for " + instruction);
                }
                Register.setPC(instruction.getNext());
            } catch (Exception e) {
                System.err.println("Trace " + instruction.toString());
                throw e;
            } finally {
                if (!printed) {
                    logger.info(instruction.toString());
//                    logger.debug(Register.getStatus());
                }
            }
        }
        return true;
    }


    // return true:  execute current instruction
    // return false: go to the next step
    // test both situation, if two are ok then, the value remains as unknown, otherwise, it should have bounded values.
    private boolean runAmbiguous(ConstrainExpr positiveExpr, ConstrainExpr negativeExpr, Subroutine procedure, Instruction instruction, Stack<Subroutine> callStack, Stack<Instruction> ambiguousCallStack) throws Exception {
        // run positive
        if (!ambiguousCallStack.contains(instruction)) { // if is is meet the same ambiguous, make a decision randomly
            logger.warn("Set Ambiguous condition to true - {} \n", instruction);

            MemorySnapshot snapshot = new MemorySnapshot();
            Set<Numeral> numeralSet = positiveExpr.setConstrain();

            ambiguousCallStack.push(instruction); // conditional....

            boolean positiveResult = interpret(procedure, instruction, (Stack<Subroutine>) callStack.clone(), ambiguousCallStack);

            positiveExpr.releaseConstrain(numeralSet);

            if (positiveResult) {
                snapshot.commit();
            } else {
                snapshot.rollback();
            }

            logger.warn("Set Ambiguous condition to false  - {}\n", instruction);
            snapshot = new MemorySnapshot();
            numeralSet = negativeExpr.setConstrain();
            boolean negativeResult = interpret(procedure, instruction, (Stack<Subroutine>) callStack.clone(), ambiguousCallStack);

            negativeExpr.releaseConstrain(numeralSet); // release constrains

            if (negativeResult) {
                snapshot.commit();
            } else {
                snapshot.rollback();
            }

            ambiguousCallStack.pop(); // conditional....

            logger.warn("Done Ambiguous condition - {}\n", instruction);

            return positiveResult || negativeResult;
        } else {
            /*
            logger.info("Run Ambiguous 0x" + Integer.toHexString(instruction.getAddress()) + " Randomly \n");
            if (random.nextBoolean()) {
                logger.info("{}(randomly skipped(false))", instruction.toString());
                Register.setPC(instruction.getNext());
                nzcv = instruction.getRandomNegative();
                return false; // continue;continue;
            }
            nzcv = instruction.getRandomPositive();
            logger.info("Continue from Ambiguous(true)\n");
            */

            logger.error("abort the same ambiguous condition - {}\n", instruction);
//            throw new RuntimeException("the same ambiguous!");
            return false;
        }



        /*
        public short getRandomNegative() {
            orgValue = null;
            orgReg = -1;
            if(isCompare() && isBranch()) { // skip
                if(conditional == CONDI_NE) {
                    ((Register) operands[0]).setValue(Immediate.ZERO);
                    orgReg = ((Register) operands[0]).getNumber();
                    orgValue = getOperand(0).getValue();
                }
                return -1;
            } else {
                short[] result = negativeConditions[conditional];
                if (conditional == CONDI_NE) {
                    if (prev.isCompare()) {
                        AbstractValue value = prev.getOperand(1).getValue();
                        if (value != null && !value.isUnknown()) {
                            orgReg = ((Register) prev.getOperand(0)).getNumber();
                            orgValue = prev.getOperand(0).getValue();
                            ((Register) prev.getOperand(0)).setValue(value);
                        }
                    }
                }
                int rnd = new Random().nextInt(result.length);
                return result[rnd];
            }
        }
        */




        // put the next pc value - lr...
        //   return true; // keep going
    }

    // extract an abstract value from registers and stackMemory memory
    public AbstractValue getValueOfParameter(int index) throws IOException {
        if (index < Register.REG_PARAM_MAX) {
            return Register.getRegisterValue(index);
        } else {
            return Register.getSP().read((index - Register.REG_PARAM_MAX) * Instruction.WORD, TYPE_INT);
        }
    }

    public SubroutineOrFunction findSymbol(String name, String moduleName) {
        SubroutineOrFunction sof = exportedSubroutineMap.get(name, moduleName);
        if (sof.isSubroutine()) {
            try {
                initModule(((Subroutine) sof).getLibraryModule());
                if (((Subroutine) sof).isNativeMethod()) {
                    clinit(((NativeMethod) sof).getSootMethod());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sof;
    }

    public String getErrorMsg(String functionName) {
        return errorMsgMap.get(functionName);
    }

    public void putErrorMsg(String functionName, String errorMsg) {
        errorMsgMap.put(functionName, errorMsg);
    }


    class Node {
        private Node parent;
        private Edge edge;
        private SootMethod method;
        private List<Node> leaves = new LinkedList<Node>();
        // initial values


        public Node(Node parent, Edge edge) {
            this(parent, edge.tgt());
            this.edge = edge;
        }

        private Node(Node parent, SootMethod method) {
            this.method = method;
            this.parent = parent;
        }

        public List<Node> getLeaves() {
            return leaves;
        }

        public Unit getUnit() {
            if (edge != null) {
                return edge.srcUnit();
            }
            return null;
        }

        public Edge getEdge() {
            return edge;
        }

        public SootMethod getMethod() {
            return method;
        }

        public Node getParent() {
            return parent;
        }

        public void pushLeafToClosedMain(Edge e) {
            if (AndroidPackage.isReceiver(method)) {
                String name = e.tgt().getName();
                int size = leaves.size();
                if (size == 0) {
                    leaves.add(new Node(this, e));
                } else {
                    int i = size - 1;
                    if ("onStart".equals(name)) {
                        for (; i >= 0; i--) {
                            String name0 = leaves.get(i).method.getName();
                            if (name0.startsWith("onCreate") || name0.equals("onStart") || !name0.startsWith("on")) {
                                break;
                            }
                        }
                        leaves.add(i + 1, new Node(this, e));
                    } else if ("onResume".equals(name)) {
                        for (; i >= 0; i--) {
                            String name0 = leaves.get(i).method.getName();
                            if (name0.startsWith("onCreate") || name0.equals("onStart") || name0.equals("onResume") || !name0.startsWith("on")) {
                                break;
                            }
                        }
                        leaves.add(i + 1, new Node(this, e));
                    } else if ("onStop".equals(name)) {
                        for (; i >= 0; i--) {
                            String name0 = leaves.get(i).method.getName();
                            if (!name0.equals("onStop") && !name0.equals("onDestroy")) {
                                break;
                            }
                        }
                        leaves.add(i + 1, new Node(this, e));
                    } else if ("onDestroy".equals(name)) {
                        leaves.add(new Node(this, e));
                    } else {
                        // etc... onPause, onKeydown, onClick....
                        for (; i >= 0; i--) {
                            String name0 = leaves.get(i).method.getName();
                            if (!name0.startsWith("onPause") && !name0.equals("onStop") && !name0.equals("onDestroy")) {
                                break;
                            }
                        }
                        leaves.add(i + 1, new Node(this, e));
                    }
                }
            } else {
                parent.pushLeafToClosedMain(e);
            }
        }

        // for what??? on...start?????
        public void addLeaves(Collection<Edge> methodList) { //????
            for (Edge e : methodList) {
                String name = e.tgt().getName();
                if (!AndroidPackage.isReceiver(method) &&
                        !name.startsWith("onCreate") &&
                        name.startsWith("on") && AndroidPackage.isCallback(e.tgt())) {
                    parent.pushLeafToClosedMain(e);
                } else {
                    leaves.add(new Node(this, e));
                }
            }
        }

        public Node popLeaf() {
            if (leaves.isEmpty()) {
                return null;
            }
            return leaves.remove(0);
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }

    public void commit() {
        for (LibraryModule module : registeredModuleMap.values()) {
            module.getInternalMemory().commit();
        }
    }

    class MemorySnapshot {
        private final List<AbstractMemory.LocalSnapshot> oldInternalLocalSnapshotList = new ArrayList<AbstractMemory.LocalSnapshot>();
        private final NZCVExpr[] oldNzcvExpr;
        private final AbstractValue[] oldRegs;
        private final AbstractValue[] oldVPFRegs;
        private final AbstractMemory.LocalSnapshot oldStack;
        private final Set<SootClass> oldClinitedSet;

        private MemorySnapshot() {
            for (LibraryModule module : registeredModuleMap.values()) {
                oldInternalLocalSnapshotList.add(module.getInternalMemory().checkPoint());
            }
            oldNzcvExpr = nzcvExpr.clone();
            oldRegs = Register.dump();
            oldVPFRegs = VPFRegister.dump();
            oldStack = stackBASE.getMemory().checkPoint();
            oldClinitedSet = clinitedSet;
            clinitedSet = new HashSet<SootClass>(clinitedSet);
        }

        public void commit() {
            for (AbstractMemory.LocalSnapshot localSnapshot : oldInternalLocalSnapshotList) {
                localSnapshot.commit();
            }
            oldStack.rollback();
            Register.restore(oldRegs);
            VPFRegister.restore(oldVPFRegs);
            nzcvExpr = oldNzcvExpr;
            clinitedSet = oldClinitedSet;
        }

        public void rollback() {
            for (AbstractMemory.LocalSnapshot localSnapshot : oldInternalLocalSnapshotList) {
                localSnapshot.rollback();
            }
            oldStack.rollback();
            Register.restore(oldRegs);
            VPFRegister.restore(oldVPFRegs);
            nzcvExpr = oldNzcvExpr;
            clinitedSet = oldClinitedSet;
        }
    }

    /*
 eq	Equal.	Z==1
 ne	Not equal.	Z==0
 cs or hs	Unsigned higher or same (or carry set).	C==1
 cc or lo	Unsigned lower (or carry clear).	C==0
 mi	Negative. The mnemonic stands for "minus".	N==1
 pl	Positive or zero. The mnemonic stands for "plus".	N==0
 vs	Signed overflow. The mnemonic stands for "V set".	V==1
 vc	No signed overflow. The mnemonic stands for "V clear".	V==0
 hi	Unsigned higher.	(C==1) && (Z==0)
 ls	Unsigned lower or same.	(C==0) || (Z==1)
 ge	Signed greater than or equal.	N==V
 lt	Signed less than.	N!=V
 gt	Signed greater than.	(Z==0) && (N==V)
 le	Signed less than or equal.	(Z==1) || (N!=V)
 al (or omitted)	Always executed.	None tested.
     */
    private ConstrainExpr getConstrainExpr(short conditional) throws Z3Exception {
        switch(conditional) {
            case CONDI_EQ:
                return nzcvExpr[Z].getPositive();
            case CONDI_NE:
                return nzcvExpr[Z].getNegative();
            case CONDI_CS:
                return nzcvExpr[C].getPositive();
            case CONDI_CC:
                return nzcvExpr[C].getNegative();
            case CONDI_MI:
                return nzcvExpr[N].getPositive();
            case CONDI_PL:
                return nzcvExpr[N].getNegative();
            case CONDI_VS:
                return nzcvExpr[V].getPositive();
            case CONDI_VC:
                return nzcvExpr[V].getNegative();
            case CONDI_HI:
                return nzcvExpr[C].getPositive().and(ctx, nzcvExpr[Z].getNegative());
            case CONDI_LS:
                return nzcvExpr[C].getNegative().or(ctx, nzcvExpr[Z].getPositive());
            case CONDI_GE:
                return nzcvExpr[N].getPositive().eq(ctx,nzcvExpr[V].getPositive());
            case CONDI_LT:
                return nzcvExpr[N].getPositive().ne(ctx,nzcvExpr[V].getPositive());
            case CONDI_GT:
                return nzcvExpr[Z].getNegative().and(ctx, nzcvExpr[N].getPositive().eq(ctx,nzcvExpr[V].getPositive()));
            case CONDI_LE:
                return nzcvExpr[Z].getPositive().or(ctx, nzcvExpr[N].getPositive().ne(ctx,nzcvExpr[V].getPositive()));
        }
        throw new RuntimeException("unexpected condition");
    }

}
